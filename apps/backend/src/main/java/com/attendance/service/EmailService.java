package com.attendance.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendNewUserCredentials(String toEmail, String username, String rawPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Attendance Management System - Your Account Has Been Created");
            helper.setText(buildEmailContent(username, rawPassword), true);

            mailSender.send(message);
            log.info("Credentials email sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private String buildEmailContent(String username, String rawPassword) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #333;">Welcome to Attendance Management System</h2>
                    <p>Your account has been created. Here are your login credentials:</p>
                    <div style="background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin: 15px 0;">
                        <p><strong>Username:</strong> %s</p>
                        <p><strong>Password:</strong> %s</p>
                    </div>
                    <p style="color: #e74c3c;"><strong>Please change your password after your first login.</strong></p>
                    <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;"/>
                    <p style="color: #999; font-size: 12px;">This is an automated message. Please do not reply.</p>
                </div>
                """.formatted(username, rawPassword);
    }
}
