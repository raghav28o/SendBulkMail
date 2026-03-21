package com.sendBulkMail.sendBulkMail.controller;

import com.sendBulkMail.sendBulkMail.service.GmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
@Slf4j
public class EmailController {

    private final GmailService gmailService;

    @GetMapping("/test")
    public String sendTestEmail(@RequestParam String to, OAuth2AuthenticationToken authentication) {
        String userEmail = authentication.getPrincipal().getAttribute("email");
        String principalName = authentication.getName();
        try {
            log.info("Received request from {} (principal: {}) to send email to: {}", userEmail, principalName, to);
            // Signature: sendEmail(String principalName, String to, String subject, String htmlBody, Long recipientId, String userEmail)
            gmailService.sendEmail(principalName, to, "Test Email", "This is a test email from the Bulk Mail Send application.", null, userEmail);
            return "Email sent successfully to " + to;
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            return "Failed to send email: " + e.getMessage();
        }
    }
}
