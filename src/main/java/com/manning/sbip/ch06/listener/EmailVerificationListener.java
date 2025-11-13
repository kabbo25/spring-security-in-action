package com.manning.sbip.ch06.listener;

import com.manning.sbip.ch06.entity.ApplicationUser;
import com.manning.sbip.ch06.entity.EmailConfiguration;
import com.manning.sbip.ch06.event.UserRegistrationEvent;
import com.manning.sbip.ch06.service.EmailConfigurationService;
import com.manning.sbip.ch06.service.EmailVerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Optional;

@Service
public class EmailVerificationListener implements ApplicationListener<UserRegistrationEvent> {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationListener.class);

    @Autowired
    private EmailConfigurationService emailConfigurationService;

    @Autowired
    private EmailVerificationService verificationService;

    @Override
    public void onApplicationEvent(UserRegistrationEvent event) {
        ApplicationUser user = event.getUser();
        String username = user.getUsername();
        String verificationId = verificationService.generateVerification(username);
        String email = event.getUser().getEmail();

        // Get email configuration from database
        Optional<EmailConfiguration> configOpt = emailConfigurationService.getActiveConfiguration();

        if (configOpt.isEmpty()) {
            logger.error("No active email configuration found. Please configure email settings at /admin/email-config");
            return;
        }

        try {
            EmailConfiguration config = configOpt.get();
            JavaMailSender mailSender = emailConfigurationService.createMailSender(config);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setSubject("Course Tracker Account Verification");
            message.setText(getText(user, verificationId));
            message.setTo(email);
            message.setFrom(config.getUsername());
            mailSender.send(message);

            logger.info("Verification email sent successfully to: {}", email);
        } catch (Exception e) {
            logger.error("Failed to send verification email to: {}. Error: {}", email, e.getMessage());
        }
    }

    private String getText(ApplicationUser user, String verificationId) {
        String encodedVerificationId = new String(Base64.getEncoder().encode(verificationId.getBytes()));
        StringBuffer buffer = new StringBuffer();
        buffer.append("Dear ").append(user.getFirstName()).append(" ").append(user.getLastName()).append(",").append(System.lineSeparator()).append(System.lineSeparator());
        buffer.append("Your account has been successfully created in the Course Tracker application. ");
        buffer.append("Activate your account by clicking the following link: http://localhost:8080/verify/email?id=").append(encodedVerificationId);
        buffer.append(System.lineSeparator()).append(System.lineSeparator());
        buffer.append("Regards,").append(System.lineSeparator()).append("Course Tracker Team");
        return buffer.toString();
    }
}
