package com.sendBulkMail.sendBulkMail.repository;

import com.sendBulkMail.sendBulkMail.model.EmailRecipient;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailRecipientRepository extends JpaRepository<EmailRecipient, Long> {
    
    @Query("SELECT r FROM EmailRecipient r WHERE r.batch.id = :batchId AND r.status = 'PENDING'")
    List<EmailRecipient> findPendingByBatchId(Long batchId, Pageable pageable);

    long countByBatchIdAndStatus(Long batchId, EmailRecipient.RecipientStatus status);
}
