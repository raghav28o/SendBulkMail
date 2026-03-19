package com.sendBulkMail.sendBulkMail.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class GoogleSheetsService {

    @Value("${google.api.key:}")
    private String apiKey;

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "BulkMailer";
    // More robust email regex
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}");

    public List<String> fetchEmailsFromSheet(String sheetUrl, Integer sheetIndex) throws GeneralSecurityException, IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("Google API Key is missing!");
            throw new IllegalStateException("Google API Key not configured in application.properties. Please add google.api.key=YOUR_KEY");
        }

        String spreadsheetId = extractSpreadsheetId(sheetUrl);
        log.info("Extracted Spreadsheet ID: {}", spreadsheetId);
        int index = (sheetIndex != null && sheetIndex > 0) ? sheetIndex - 1 : 0;

        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, null)
                .setApplicationName(APPLICATION_NAME)
                .build();

        // 1. Get Spreadsheet metadata to find the sheet name by index
        com.google.api.services.sheets.v4.model.Spreadsheet spreadsheet = service.spreadsheets()
                .get(spreadsheetId)
                .setKey(apiKey)
                .execute();

        if (index >= spreadsheet.getSheets().size()) {
            throw new IllegalArgumentException("Sheet number " + (index + 1) + " does not exist. This file only has " + spreadsheet.getSheets().size() + " sheet(s).");
        }

        String sheetName = spreadsheet.getSheets().get(index).getProperties().getTitle();
        String effectiveRange = "'" + sheetName + "'!A1:Z5000";

        log.info("Fetching emails from spreadsheet ID: {}, Sheet Name: '{}' (Index: {})", spreadsheetId, sheetName, index);

        ValueRange response = service.spreadsheets().values()
                .get(spreadsheetId, effectiveRange)
                .setKey(apiKey)
                .execute();


        List<List<Object>> values = response.getValues();
        List<String> emails = new ArrayList<>();

        if (values != null) {
            for (List<Object> row : values) {
                for (Object cell : row) {
                    String cellValue = cell.toString();
                    Matcher matcher = EMAIL_PATTERN.matcher(cellValue);
                    while (matcher.find()) {
                        emails.add(matcher.group());
                    }
                }
            }
        }

        log.info("Extracted {} emails from sheet", emails.size());
        return emails;
    }

    private String extractSpreadsheetId(String url) {
        // Example: https://docs.google.com/spreadsheets/d/SPREADSHEET_ID/edit#gid=0
        if (url.contains("/d/")) {
            String temp = url.split("/d/")[1];
            return temp.split("/")[0];
        }
        return url; // Assume it is already an ID
    }
}
