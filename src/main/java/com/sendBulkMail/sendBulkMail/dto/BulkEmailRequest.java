package com.sendBulkMail.sendBulkMail.dto;

import lombok.Data;
import java.time.LocalTime;
import java.util.List;

@Data
public class BulkEmailRequest {
    private String subject;
    private String body;
    private List<String> recipients;
    private Integer dailyLimit;
    private LocalTime startTime;
    private Long delaySeconds;
}
