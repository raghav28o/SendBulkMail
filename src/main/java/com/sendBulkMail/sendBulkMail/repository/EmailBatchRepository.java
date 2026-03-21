package com.sendBulkMail.sendBulkMail.repository;

import com.sendBulkMail.sendBulkMail.model.EmailBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailBatchRepository extends JpaRepository<EmailBatch, Long> {
    List<EmailBatch> findByStatus(EmailBatch.BatchStatus status);
    List<EmailBatch> findAllByOrderByCreatedAtDesc();
    List<EmailBatch> findByCreatedByOrderByCreatedAtDesc(String createdBy);
}
