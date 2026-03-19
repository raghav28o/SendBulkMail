package com.sendBulkMail.sendBulkMail.controller;

import com.sendBulkMail.sendBulkMail.dto.BulkEmailRequest;
import com.sendBulkMail.sendBulkMail.model.EmailBatch;
import com.sendBulkMail.sendBulkMail.model.EmailRecipient;
import com.sendBulkMail.sendBulkMail.repository.EmailBatchRepository;
import com.sendBulkMail.sendBulkMail.repository.EmailRecipientRepository;
import com.sendBulkMail.sendBulkMail.service.BulkEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
@Slf4j
public class EmailViewController {

    private final EmailBatchRepository batchRepository;
    private final EmailRecipientRepository recipientRepository;
    private final BulkEmailService bulkEmailService;

    @GetMapping
    public String index() {
        return "redirect:/batches";
    }

    @GetMapping("/batches")
    public String listBatches(Model model) {
        model.addAttribute("batches", batchRepository.findAllByOrderByCreatedAtDesc());
        return "batches";
    }

    @GetMapping("/schedule")
    public String showScheduleForm(Model model) {
        model.addAttribute("request", new BulkEmailRequest());
        return "schedule";
    }

    @PostMapping("/schedule")
    public String processSchedule(@ModelAttribute BulkEmailRequest request, @RequestParam("recipientList") String recipientList) {
        // Convert the textarea string into a list of emails
        List<String> recipients = Arrays.stream(recipientList.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        request.setRecipients(recipients);
        
        bulkEmailService.createBatch(request);
        return "redirect:/batches";
    }

    @GetMapping("/batch/{id}")
    public String viewBatchDetails(@PathVariable Long id, @RequestParam(defaultValue = "0") int page, Model model) {
        EmailBatch batch = batchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid batch ID: " + id));
        
        List<EmailRecipient> recipients = recipientRepository.findPendingByBatchId(id, PageRequest.of(page, 100));
        // We'll also want to see non-pending ones, so let's adjust the repository later or use findByBatchId
        // For now, let's just get the first 100 recipients for simplicity
        
        long totalSent = recipientRepository.countByBatchIdAndStatus(id, EmailRecipient.RecipientStatus.SENT);
        long totalPending = recipientRepository.countByBatchIdAndStatus(id, EmailRecipient.RecipientStatus.PENDING);
        long totalFailed = recipientRepository.countByBatchIdAndStatus(id, EmailRecipient.RecipientStatus.FAILED);
        
        model.addAttribute("batch", batch);
        model.addAttribute("recipients", batch.getRecipients()); // Caution: Lazy loading thousands might be slow
        model.addAttribute("totalSent", totalSent);
        model.addAttribute("totalPending", totalPending);
        model.addAttribute("totalFailed", totalFailed);
        
        return "batch-details";
    }
}
