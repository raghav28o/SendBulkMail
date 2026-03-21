package com.sendBulkMail.sendBulkMail.service;

import com.sendBulkMail.sendBulkMail.model.EmailBatch;
import com.sendBulkMail.sendBulkMail.model.EmailRecipient;
import com.sendBulkMail.sendBulkMail.repository.EmailBatchRepository;
import com.sendBulkMail.sendBulkMail.repository.EmailRecipientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkEmailTaskExecutor {

    private final EmailRecipientRepository recipientRepository;
    private final EmailBatchRepository batchRepository;
    private final GmailService gmailService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Async
    public void sendDailyBatch(EmailBatch batch) {
        log.info("Starting daily execution for batch ID: {}, subject: {}, createdBy (principal): {}, userEmail: {}", 
                batch.getId(), batch.getSubject(), batch.getCreatedBy(), batch.getUserEmail());
        
        String principalName = batch.getCreatedBy();
        String userEmail = batch.getUserEmail();
        
        // Verify we still have a valid token (at least that we can load the client)
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient("google", principalName);
        if (client == null) {
            log.error("Cannot execute batch {}: No authorized client found for principal {}", batch.getId(), principalName);
            return;
        }

        List<EmailRecipient> pendingRecipients = recipientRepository.findPendingByBatchId(
                batch.getId(), PageRequest.of(0, batch.getDailyLimit()));

        if (pendingRecipients.isEmpty()) {
            log.info("No pending recipients for batch ID: {}. Marking batch as COMPLETED.", batch.getId());
            batch.setStatus(EmailBatch.BatchStatus.COMPLETED);
            batchRepository.save(batch);
            return;
        }

        for (EmailRecipient recipient : pendingRecipients) {
            try {
                log.debug("Sending email to: {}", recipient.getEmail());
                
                // Dynamic Personalization
                String personalizedSubject = batch.getSubject();
                String personalizedBody = batch.getBody();
                
                String nameReplacement = (recipient.getName() != null && !recipient.getName().isEmpty()) 
                        ? " " + recipient.getName() 
                        : "";
                
                personalizedSubject = personalizedSubject.replace("[[NAME]]", nameReplacement);
                personalizedBody = personalizedBody.replace("[[NAME]]", nameReplacement);

                // Using gmailService with principalName and userEmail
                gmailService.sendEmail(principalName, recipient.getEmail(), personalizedSubject, personalizedBody, recipient.getId(), userEmail);
                recipient.setStatus(EmailRecipient.RecipientStatus.SENT);
                recipient.setSentAt(LocalDateTime.now());
            } catch (Exception e) {
                log.error("Failed to send email to: {}. Error: {}", recipient.getEmail(), e.getMessage());
                recipient.setStatus(EmailRecipient.RecipientStatus.FAILED);
                recipient.setErrorMessage(e.getMessage());
            }
            recipientRepository.save(recipient);

            if (batch.getDelaySeconds() > 0) {
                try {
                    Thread.sleep(batch.getDelaySeconds() * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Thread interrupted during delay", e);
                    break;
                }
            }
        }

        batch.setLastRunDate(LocalDateTime.now());
        
        // Check if all recipients are processed to update status
        long totalPending = recipientRepository.countByBatchIdAndStatus(batch.getId(), EmailRecipient.RecipientStatus.PENDING);
        if (totalPending == 0) {
            log.info("All recipients processed for batch ID: {}. Marking as COMPLETED.", batch.getId());
            batch.setStatus(EmailBatch.BatchStatus.COMPLETED);
        }
        
        batchRepository.save(batch);
        log.info("Daily execution finished for batch ID: {}", batch.getId());
    }
}
