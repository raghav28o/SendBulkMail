package com.sendBulkMail.sendBulkMail.controller;

import com.sendBulkMail.sendBulkMail.dto.BulkEmailRequest;
import com.sendBulkMail.sendBulkMail.model.EmailBatch;
import com.sendBulkMail.sendBulkMail.service.BulkEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bulkMail")
@RequiredArgsConstructor
@Slf4j
public class BulkEmailController {

    private final BulkEmailService bulkEmailService;

    @PostMapping("/schedule")
    public ResponseEntity<String> scheduleBulkEmail(@RequestBody BulkEmailRequest request) {
        log.info("Received request to schedule bulk email: {}", request.getSubject());
        EmailBatch batch = bulkEmailService.createBatch(request);
        return ResponseEntity.ok("Bulk email batch created and scheduled with ID: " + batch.getId());
    }
}
