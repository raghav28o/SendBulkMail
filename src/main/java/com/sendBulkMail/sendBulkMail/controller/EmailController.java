package com.sendBulkMail.sendBulkMail.controller;

import com.sendBulkMail.sendBulkMail.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
@Slf4j
public class EmailController {

    private final EmailService emailService;

    @GetMapping("/test")
    public String sendTestEmail(@RequestParam String to) {
        try {
            log.info("Received request to send email to: {}", to);
            emailService.sendSimpleEmail(to, "Test Email", "This is a test email from the Bulk Mail Send application.");
            return "Email sent successfully to " + to;
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            return "Failed to send email: " + e.getMessage();
        }
    }
}
