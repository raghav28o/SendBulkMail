package com.sendBulkMail.sendBulkMail.service;

import com.sendBulkMail.sendBulkMail.dto.RecipientDTO;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class GoogleSheetsService {

    @Value("${google.api.key:}")
    private String apiKey;

    private final EmailValidatorService emailValidatorService;

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "BulkMailer";

    public List<RecipientDTO> fetchEmailsFromSheet(String sheetUrl, Integer sheetIndex) throws GeneralSecurityException, IOException {
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
        List<RecipientDTO> recipients = new ArrayList<>();

        if (values != null) {
            for (List<Object> row : values) {
                String foundEmail = null;
                String foundName = null;
                for (Object cell : row) {
                    if (cell == null) continue;
                    String cellValue = cell.toString().trim();
                    
                    // Simple regex check first to see if it looks like an email
                    if (cellValue.contains("@") && emailValidatorService.isRegexValid(cellValue)) {
                        // Only perform MX check if regex passes
                        if (emailValidatorService.hasMxRecord(cellValue)) {
                            foundEmail = cellValue;
                        } else {
                            log.warn("Filtered out email with no valid MX record: {}", cellValue);
                        }
                    } else if (!cellValue.isEmpty() && foundName == null) {
                        foundName = cellValue;
                    }
                }
                if (foundEmail != null) {
                    recipients.add(new RecipientDTO(foundEmail, foundName));
                }
            }
        }

        log.info("Extracted {} recipients from sheet", recipients.size());
        return recipients;
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
