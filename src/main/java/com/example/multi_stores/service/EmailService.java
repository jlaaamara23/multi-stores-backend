package com.example.multi_stores.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:}")
    private String fromEmail;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends a verification email with a link. Uses Gmail SMTP when configured.
     * If app.mail.from is empty, skips sending (e.g. for local dev without mail config).
     */
    public void sendVerificationEmail(String toEmail, String verificationToken) {
        if (mailSender == null || fromEmail == null || fromEmail.isBlank()) {
            log.warn("Mail not configured. Verification email not sent to {}", toEmail);
            return;
        }
        String verifyUrl = frontendUrl + "/verify-email?token=" + verificationToken;
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromEmail);
        msg.setTo(toEmail);
        msg.setSubject("Verify your email - جيب");
        msg.setText("Please verify your email by clicking the link below:\n\n" + verifyUrl + "\n\nThis link expires in 24 hours.");
        try {
            mailSender.send(msg);
            log.info("Verification email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Sends an HTML email (e.g. invoice). If mail is not configured, logs and skips.
     */
    public void sendHtmlEmail(String toEmail, String subject, String htmlBody) {
        if (mailSender == null || fromEmail == null || fromEmail.isBlank()) {
            log.warn("Mail not configured. Invoice email not sent to {}", toEmail);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Invoice email sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send invoice email to {}: {}", toEmail, e.getMessage());
        }
    }
}
