package com.example.multi_stores.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class EmailTestController {

    @Autowired
    private JavaMailSender mailSender;

    @GetMapping("/mail")
    public String testEmail() {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("jlaaam@ac.sce.ac.il");
            message.setTo("jlaa.amarajlaa@gmail.com");
            message.setSubject("Brevo Test");
            message.setText("If you see this, Brevo is working!");

            mailSender.send(message);
            return "Email sent successfully!";
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (msg.contains("Authentication failed") || msg.contains("535") || msg.contains("authentication"))
                return "Error: Brevo SMTP authentication failed. In Brevo go to Settings → SMTP & API → SMTP tab. Use the 'SMTP login' (email) as spring.mail.username and your SMTP key as spring.mail.password in application.properties. No auth needed in Postman for this URL.";
            return "Error: " + msg;
        }
    }
}
