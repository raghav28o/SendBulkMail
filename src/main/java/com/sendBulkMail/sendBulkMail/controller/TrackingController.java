package com.sendBulkMail.sendBulkMail.controller;

import com.sendBulkMail.sendBulkMail.model.EmailRecipient;
import com.sendBulkMail.sendBulkMail.repository.EmailRecipientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Base64;

@RestController
@RequestMapping("/api/track")
@RequiredArgsConstructor
@Slf4j
public class TrackingController {

    private final EmailRecipientRepository recipientRepository;

    // 1x1 Transparent GIF pixel
    private static final byte[] PIXEL = Base64.getDecoder().decode("R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");

    @GetMapping(value = "/open/{recipientId}", produces = MediaType.IMAGE_GIF_VALUE)
    public ResponseEntity<byte[]> trackOpen(@PathVariable Long recipientId) {
        log.info("Email open tracked for recipient ID: {}", recipientId);

        recipientRepository.findById(recipientId).ifPresent(recipient -> {
            if (recipient.getOpenedAt() == null) {
                recipient.setOpenedAt(LocalDateTime.now());
            }
            recipient.setOpenCount(recipient.getOpenCount() + 1);
            recipientRepository.save(recipient);
        });

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_GIF)
                .body(PIXEL);
    }
}
