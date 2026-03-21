package com.sendBulkMail.sendBulkMail.controller;

import com.sendBulkMail.sendBulkMail.dto.BulkEmailRequest;
import com.sendBulkMail.sendBulkMail.dto.RecipientDTO;
import com.sendBulkMail.sendBulkMail.model.EmailBatch;
import com.sendBulkMail.sendBulkMail.service.BulkEmailService;
import com.sendBulkMail.sendBulkMail.service.GmailService;
import com.sendBulkMail.sendBulkMail.service.GoogleSheetsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
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
    private final GmailService gmailService;

    @PostMapping("/schedule")
    public ResponseEntity<String> scheduleBulkEmail(@RequestBody BulkEmailRequest request, OAuth2AuthenticationToken authentication) {
        String userEmail = authentication.getPrincipal().getAttribute("email");
        String principalName = authentication.getName();
        log.info("Received request to schedule bulk email from {}: {}", userEmail, request.getSubject());
        
        // We store the principal name in createdBy to ensure we can load the token later for the background task
        EmailBatch batch = bulkEmailService.createBatch(request, principalName, userEmail);
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
    public ResponseEntity<?> testSend(@RequestBody Map<String, String> payload, OAuth2AuthenticationToken authentication) {
        String userEmail = authentication.getPrincipal().getAttribute("email");
        String principalName = authentication.getName();
        String to = payload.get("to");
        String subject = payload.get("subject");
        String body = payload.get("body");

        log.info("Sending test email from {} (principal: {}) to: {}", userEmail, principalName, to);
        
        try {
            // Apply a sample personalization
            String sampleName = "User";
            String testSubject = subject.replace("[[NAME]]", " " + sampleName);
            String testBody = body.replace("[[NAME]]", " " + sampleName);
            
            // GmailService now expects principalName
            gmailService.sendEmail(principalName, to, testSubject, testBody, null, userEmail);
            return ResponseEntity.ok("Test email sent successfully to " + to);
        } catch (Exception e) {
            log.error("Failed to send test email", e);
            return ResponseEntity.badRequest().body("Failed: " + e.getMessage());
        }
    }
}

