package com.example.multi_stores.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String BREVO_SEND_ENDPOINT = "https://api.brevo.com/v3/smtp/email";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.mail.from:}")
    private String fromEmail;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.mail.brevo-api-key:${BREVO_API_KEY:}}")
    private String brevoApiKey;

    /**
     * Sends a verification email with a link via Brevo API.
     */
    public boolean sendVerificationEmail(String toEmail, String verificationToken) {
        if (!isBrevoConfigured()) {
            log.warn("Brevo API not configured. Verification email not sent to {}", toEmail);
            return false;
        }
        String verifyUrl = frontendUrl + "/verify-email?token=" + verificationToken;
        String text = "Please verify your email by clicking the link below:\n\n" + verifyUrl + "\n\nThis link expires in 2 hours.";
        try {
            sendViaBrevo(toEmail, "Verify your email - جيب", text, null);
            log.info("Verification email sent to {}", toEmail);
            return true;
        } catch (RestClientException e) {
            log.error("Failed to send verification email to {} via Brevo API: {}", toEmail, e.getMessage());
            return false;
        }
    }

    /**
     * Sends an HTML email (e.g. invoice) via Brevo API.
     */
    public void sendHtmlEmail(String toEmail, String subject, String htmlBody) {
        if (!isBrevoConfigured()) {
            log.warn("Brevo API not configured. Invoice email not sent to {}", toEmail);
            return;
        }
        try {
            sendViaBrevo(toEmail, subject, null, htmlBody);
            log.info("Invoice email sent to {}", toEmail);
        } catch (RestClientException e) {
            log.error("Failed to send invoice email to {} via Brevo API: {}", toEmail, e.getMessage());
        }
    }

    private boolean isBrevoConfigured() {
        return brevoApiKey != null && !brevoApiKey.isBlank() && fromEmail != null && !fromEmail.isBlank();
    }

    private void sendViaBrevo(String toEmail, String subject, String textContent, String htmlContent) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey.trim());

        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("sender", Map.of("email", fromEmail.trim(), "name", "JIB"));
        payload.put("to", List.of(Map.of("email", toEmail)));
        payload.put("subject", subject);
        if (textContent != null && !textContent.isBlank()) {
            payload.put("textContent", textContent);
        }
        if (htmlContent != null && !htmlContent.isBlank()) {
            payload.put("htmlContent", htmlContent);
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(BREVO_SEND_ENDPOINT, request, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RestClientException("Brevo API responded with status " + response.getStatusCode().value());
        }
    }
}
