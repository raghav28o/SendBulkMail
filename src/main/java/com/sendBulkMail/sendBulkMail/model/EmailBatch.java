package com.sendBulkMail.sendBulkMail.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "email_batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    private Integer dailyLimit;

    private LocalTime startTime;

    private Long delaySeconds;

    @Enumerated(EnumType.STRING)
    private BatchStatus status;

    private String createdBy;

    private String userEmail;

    private LocalDateTime createdAt;

    private LocalDateTime lastRunDate;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EmailRecipient> recipients;

    public enum BatchStatus {
        ACTIVE, COMPLETED, PAUSED
    }
}
