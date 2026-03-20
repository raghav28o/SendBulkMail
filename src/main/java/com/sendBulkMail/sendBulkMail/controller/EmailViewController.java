package com.sendBulkMail.sendBulkMail.controller;

import com.sendBulkMail.sendBulkMail.dto.BulkEmailRequest;
import com.sendBulkMail.sendBulkMail.dto.RecipientDTO;
import com.sendBulkMail.sendBulkMail.model.EmailBatch;
import com.sendBulkMail.sendBulkMail.model.EmailRecipient;
import com.sendBulkMail.sendBulkMail.repository.EmailBatchRepository;
import com.sendBulkMail.sendBulkMail.repository.EmailRecipientRepository;
import com.sendBulkMail.sendBulkMail.service.BulkEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
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
    @Transactional
    public String processSchedule(@ModelAttribute BulkEmailRequest request, @RequestParam("recipientList") String recipientList) {
        // Convert the textarea string into a list of RecipientDTO
        List<RecipientDTO> recipients = Arrays.stream(recipientList.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(line -> {
                    if (line.contains(",")) {
                        String[] parts = line.split(",", 2);
                        return new RecipientDTO(parts[0].trim(), parts[1].trim());
                    } else {
                        return new RecipientDTO(line, null);
                    }
                })
                .collect(Collectors.toList());
        request.setRecipients(recipients);
        
        bulkEmailService.createBatch(request);
        return "redirect:/batches";
    }

    @GetMapping("/batch/{id}")
    public String viewBatchDetails(@PathVariable Long id, @RequestParam(defaultValue = "0") int page, Model model) {
        EmailBatch batch = batchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid batch ID: " + id));
        
        long totalSent = recipientRepository.countByBatchIdAndStatus(id, EmailRecipient.RecipientStatus.SENT);
        long totalPending = recipientRepository.countByBatchIdAndStatus(id, EmailRecipient.RecipientStatus.PENDING);
        long totalFailed = recipientRepository.countByBatchIdAndStatus(id, EmailRecipient.RecipientStatus.FAILED);
        long totalBounced = recipientRepository.countByBatchIdAndStatus(id, EmailRecipient.RecipientStatus.BOUNCED);
        long totalOpened = recipientRepository.countByBatchIdAndOpenedAtIsNotNull(id);
        long totalRecipients = recipientRepository.countByBatchId(id);
        
        List<EmailRecipient> recipients = recipientRepository.findByBatchId(id, PageRequest.of(page, 100));
        
        model.addAttribute("batch", batch);
        model.addAttribute("recipients", recipients);
        model.addAttribute("totalSent", totalSent);
        model.addAttribute("totalPending", totalPending);
        model.addAttribute("totalFailed", totalFailed);
        model.addAttribute("totalBounced", totalBounced);
        model.addAttribute("totalOpened", totalOpened);
        model.addAttribute("totalRecipients", totalRecipients);
        
        return "batch-details";
    }
}
