package com.sendBulkMail.sendBulkMail.service;

import com.sendBulkMail.sendBulkMail.model.EmailBatch;
import com.sendBulkMail.sendBulkMail.repository.EmailBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkEmailScheduler {

    private final EmailBatchRepository batchRepository;
    private final BulkEmailTaskExecutor taskExecutor;

    @Scheduled(cron = "0 * * * * *") // Runs every minute
    public void scheduleDailyBatch() {
        log.debug("Checking for active bulk mail batches to trigger...");
        List<EmailBatch> activeBatches = batchRepository.findByStatus(EmailBatch.BatchStatus.ACTIVE);
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);

        for (EmailBatch batch : activeBatches) {
            // Check if it's the right time and if it hasn't run today yet
            if (batch.getStartTime().equals(now)) {
                if (batch.getLastRunDate() == null || !batch.getLastRunDate().toLocalDate().equals(LocalDate.now())) {
                    log.info("Triggering daily batch for ID: {}, subject: {}", batch.getId(), batch.getSubject());
                    taskExecutor.sendDailyBatch(batch);
                } else {
                    log.debug("Batch ID: {} already ran today.", batch.getId());
                }
            }
        }
    }
}
