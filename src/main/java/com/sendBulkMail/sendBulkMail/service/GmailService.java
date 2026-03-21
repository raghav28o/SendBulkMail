package com.sendBulkMail.sendBulkMail.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Properties;

@Service
@RequiredArgsConstructor
@Slf4j
public class GmailService {

    private final OAuth2AuthorizedClientService authorizedClientService;

    @Value("${app.mail.personal-name:Bulk Mailer}")
    private String personalName;

    @Value("${app.base-url:http://localhost:9080}")
    private String baseUrl;

    public void sendEmail(String principalName, String to, String subject, String htmlBody, Long recipientId, String userEmail) throws MessagingException, IOException, GeneralSecurityException {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient("google", principalName);
        if (client == null) {
            log.error("No authorized client found for principal: {}", principalName);
            throw new RuntimeException("No authorized client found for user: " + principalName);
        }

        String accessToken = client.getAccessToken().getTokenValue();
        
        Gmail service = new Gmail.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                request -> request.getHeaders().setAuthorization("Bearer " + accessToken))
                .setApplicationName("SendBulkMail")
                .build();

        // Inject tracking pixel if recipientId is provided
        String finalBody = htmlBody;
        if (recipientId != null) {
            String trackingUrl = baseUrl + "/api/track/open/" + recipientId;
            String trackingPixel = "<img src=\"" + trackingUrl + "\" width=\"1\" height=\"1\" style=\"display:none;\" />";
            finalBody = htmlBody + trackingPixel;
        }

        MimeMessage mimeMessage = createEmail(to, userEmail, subject, finalBody);
        Message message = createMessageWithEmail(mimeMessage);
        service.users().messages().send("me", message).execute();
        log.info("Email sent via Gmail API from {} to: {}", userEmail, to);
    }

    private MimeMessage createEmail(String to, String from, String subject, String bodyText) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(from));
        email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setContent(bodyText, "text/html; charset=utf-8");
        return email;
    }

    private Message createMessageWithEmail(MimeMessage emailContent) throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }
}
