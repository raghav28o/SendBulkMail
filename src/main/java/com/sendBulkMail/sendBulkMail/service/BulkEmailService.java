package com.sendBulkMail.sendBulkMail.service;

import com.sendBulkMail.sendBulkMail.dto.BulkEmailRequest;
import com.sendBulkMail.sendBulkMail.model.EmailBatch;
import com.sendBulkMail.sendBulkMail.model.EmailRecipient;
import com.sendBulkMail.sendBulkMail.repository.EmailBatchRepository;
import com.sendBulkMail.sendBulkMail.repository.EmailRecipientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkEmailService {

    private final EmailBatchRepository batchRepository;
    private final EmailRecipientRepository recipientRepository;
    private final EmailValidatorService emailValidatorService;

    @Transactional
    public EmailBatch createBatch(BulkEmailRequest request) {
        log.info("Creating new bulk email batch for subject: {}", request.getSubject());
        
        EmailBatch batch = EmailBatch.builder()
                .subject(request.getSubject())
                .body(request.getBody())
                .dailyLimit(request.getDailyLimit())
                .startTime(request.getStartTime())
                .delaySeconds(request.getDelaySeconds())
                .status(EmailBatch.BatchStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        EmailBatch savedBatch = batchRepository.save(batch);

        List<EmailRecipient> recipients = request.getRecipients().stream()
                .filter(dto -> {
                    boolean isValid = emailValidatorService.isValid(dto.getEmail());
                    if (!isValid) {
                        log.warn("Filtering out invalid recipient email: {}", dto.getEmail());
                    }
                    return isValid;
                })
                .map(dto -> EmailRecipient.builder()
                        .email(dto.getEmail())
                        .name(dto.getName())
                        .status(EmailRecipient.RecipientStatus.PENDING)
                        .batch(savedBatch)
                        .build())
                .collect(Collectors.toList());

        recipientRepository.saveAll(recipients);
        
        log.info("Batch created with ID: {} and {} valid recipients ({} filtered out)", 
                savedBatch.getId(), recipients.size(), request.getRecipients().size() - recipients.size());
        return savedBatch;
    }
}
