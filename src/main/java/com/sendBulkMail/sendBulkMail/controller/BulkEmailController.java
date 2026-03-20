package com.sendBulkMail.sendBulkMail.controller;

import com.sendBulkMail.sendBulkMail.dto.BulkEmailRequest;
import com.sendBulkMail.sendBulkMail.dto.RecipientDTO;
import com.sendBulkMail.sendBulkMail.model.EmailBatch;
import com.sendBulkMail.sendBulkMail.service.BulkEmailService;
import com.sendBulkMail.sendBulkMail.service.EmailService;
import com.sendBulkMail.sendBulkMail.service.GoogleSheetsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bulk-mail")
@RequiredArgsConstructor
@Slf4j
public class BulkEmailController {

    private final BulkEmailService bulkEmailService;
    private final GoogleSheetsService googleSheetsService;
    private final EmailService emailService;

    @PostMapping("/schedule")
    public ResponseEntity<String> scheduleBulkEmail(@RequestBody BulkEmailRequest request) {
        log.info("Received request to schedule bulk email: {}", request.getSubject());
        EmailBatch batch = bulkEmailService.createBatch(request);
        return ResponseEntity.ok("Bulk email batch created and scheduled with ID: " + batch.getId());
    }

    @PostMapping("/fetch-sheets")
    public ResponseEntity<?> fetchEmails(@RequestBody Map<String, Object> payload) {
        String url = (String) payload.get("url");
        Object indexObj = payload.getOrDefault("sheetIndex", 1);
        Integer index;
        
        if (indexObj instanceof Number) {
            index = ((Number) indexObj).intValue();
        } else if (indexObj instanceof String) {
            index = Integer.parseInt((String) indexObj);
        } else {
            index = 1;
        }

        try {
            List<RecipientDTO> emails = googleSheetsService.fetchEmailsFromSheet(url, index);
            return ResponseEntity.ok(emails);
        } catch (Exception e) {
            log.error("Failed to fetch emails from sheets", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/test-send")
    public ResponseEntity<?> testSend(@RequestBody Map<String, String> payload) {
        String to = payload.get("to");
        String subject = payload.get("subject");
        String body = payload.get("body");

        log.info("Sending test email to: {}", to);
        
        try {
            // Apply a sample personalization
            String sampleName = "User";
            String testSubject = subject.replace("[[NAME]]", " " + sampleName);
            String testBody = body.replace("[[NAME]]", " " + sampleName);
            
            emailService.sendHtmlEmail(to, testSubject, testBody, null);
            return ResponseEntity.ok("Test email sent successfully to " + to);
        } catch (Exception e) {
            log.error("Failed to send test email", e);
            return ResponseEntity.badRequest().body("Failed: " + e.getMessage());
        }
    }
}

