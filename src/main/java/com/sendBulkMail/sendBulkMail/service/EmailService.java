package com.sendBulkMail.sendBulkMail.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.mail.personal-name:Raghav Agarwal}")
    private String personalName;

    @Value("${app.base-url:http://localhost:9080}")
    private String appBaseUrl;

    private String getFormattedFrom() {
        return personalName + " <" + fromEmail + ">";
    }

    /**
     * Send a simple text email.
     *
     * @param to      recipient's email address
     * @param subject email subject
     * @param text    email body
     */
    public void sendSimpleEmail(String to, String subject, String text) {
        log.info("Preparing to send simple email to: {}", to);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(getFormattedFrom());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
        log.info("Simple email sent successfully to: {}", to);
    }

    /**
     * Send an HTML email with optional tracking.
     *
     * @param to          recipient's email address
     * @param subject     email subject
     * @param htmlContent HTML content for the email body
     * @param recipientId optional recipient ID for tracking
     * @throws MessagingException if an error occurs during message creation
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent, Long recipientId) throws MessagingException, UnsupportedEncodingException {
        log.info("Preparing to send HTML email to: {}", to);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(new InternetAddress(fromEmail, personalName));
        helper.setTo(to);
        helper.setSubject(subject);

        String finalContent = htmlContent;
        if (recipientId != null) {
            String trackingUrl = appBaseUrl + "/api/track/open/" + recipientId;
            String trackingPixel = "<img src=\"" + trackingUrl + "\" width=\"1\" height=\"1\" style=\"display:none;\" />";
            finalContent += trackingPixel;
        }
        System.out.println("Final HTML content for email to " + to + ": " + finalContent); // Debug log for email content
        
        helper.setText(finalContent, true);

        mailSender.send(message);
        log.info("HTML email sent successfully to: {}", to);
    }

    /**
     * Send an email with an attachment.
     *
     * @param to             recipient's email address
     * @param subject        email subject
     * @param text           email body
     * @param attachmentFile file to attach
     * @throws MessagingException if an error occurs during message creation
     */
    public void sendEmailWithAttachment(String to, String subject, String text, File attachmentFile) throws MessagingException, UnsupportedEncodingException {
        log.info("Preparing to send email with attachment to: {}", to);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(new InternetAddress(fromEmail, personalName));
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text);

        if (attachmentFile != null && attachmentFile.exists()) {
            helper.addAttachment(attachmentFile.getName(), attachmentFile);
        }

        mailSender.send(message);
        log.info("Email with attachment sent successfully to: {}", to);
    }
}
