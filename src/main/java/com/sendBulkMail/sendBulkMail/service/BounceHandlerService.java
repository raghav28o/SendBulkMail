package com.sendBulkMail.sendBulkMail.service;

import com.sendBulkMail.sendBulkMail.model.EmailRecipient;
import com.sendBulkMail.sendBulkMail.repository.EmailRecipientRepository;
import jakarta.mail.*;
import jakarta.mail.search.FlagTerm;
import jakarta.mail.search.OrTerm;
import jakarta.mail.search.SearchTerm;
import jakarta.mail.search.SubjectTerm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class BounceHandlerService {

    private final EmailRecipientRepository recipientRepository;

    @Value("${spring.mail.imap.host:imap.gmail.com}")
    private String host;

    @Value("${spring.mail.imap.port:993}")
    private String port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    private static final Pattern EMAIL_REGEX = Pattern.compile("[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}");

    @Scheduled(fixedDelay = 300000) // Run every 5 minutes
    public void checkForBounces() {
        log.info("Starting background check for email bounces...");
        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imaps");
        properties.put("mail.imaps.host", host);
        properties.put("mail.imaps.port", port);

        try {
            Session emailSession = Session.getDefaultInstance(properties);
            Store store = emailSession.getStore("imaps");
            store.connect(host, username, password);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            // Search for unread delivery failure notifications
            SearchTerm searchTerm = new OrTerm(
                new SubjectTerm("Delivery Status Notification (Failure)"),
                new SubjectTerm("Undeliverable:")
            );
            
            // Only search unread messages
            FlagTerm flagTerm = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
            Message[] messages = inbox.search(searchTerm);

            log.info("Found {} potential bounce notifications in inbox.", messages.length);

            for (Message message : messages) {
                try {
                    String content = getMessageContent(message);
                    extractAndMarkBouncedEmail(content);
                    // Mark as seen so we don't process it again next time
                    message.setFlag(Flags.Flag.SEEN, true);
                } catch (Exception e) {
                    log.error("Error processing bounce message: {}", e.getMessage());
                }
            }

            inbox.close(false);
            store.close();
        } catch (Exception e) {
            log.error("IMAP Error: {}", e.getMessage());
        }
    }

    private String getMessageContent(Message message) throws Exception {
        Object content = message.getContent();
        if (content instanceof String) {
            return (String) content;
        } else if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (bodyPart.getContent() instanceof String) {
                    result.append(bodyPart.getContent());
                }
            }
            return result.toString();
        }
        return "";
    }

    private void extractAndMarkBouncedEmail(String content) {
        Matcher matcher = EMAIL_REGEX.matcher(content);
        while (matcher.find()) {
            String email = matcher.group();
            // We look for recipients with this email and status 'SENT' to mark as 'BOUNCED'
            // Using optimized repository query
            List<EmailRecipient> recipients = recipientRepository.findByEmailIgnoreCaseAndStatus(email, EmailRecipient.RecipientStatus.SENT);
            
            for (EmailRecipient recipient : recipients) {
                log.warn("Marking email as BOUNCED: {} for batch ID: {}", email, recipient.getBatch().getId());
                recipient.setStatus(EmailRecipient.RecipientStatus.BOUNCED);
                recipient.setErrorMessage("Delivery Status Notification: Recipient not found or rejected.");
                recipientRepository.save(recipient);
            }
        }
    }
}
