package com.sendBulkMail.sendBulkMail.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_recipients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String name;

    @Enumerated(EnumType.STRING)
    private RecipientStatus status;

    private LocalDateTime sentAt;

    private LocalDateTime openedAt;

    @Builder.Default
    private Integer openCount = 0;

    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private EmailBatch batch;

    public enum RecipientStatus {
        PENDING, SENT, FAILED, BOUNCED, INVALID_ADDRESS
    }
}
