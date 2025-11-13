package com.manning.sbip.ch06.service;

import com.manning.sbip.ch06.entity.EmailConfiguration;
import com.manning.sbip.ch06.repository.EmailConfigurationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Properties;

@Service
public class EmailConfigurationService {

    @Autowired
    private EmailConfigurationRepository repository;

    public Optional<EmailConfiguration> getActiveConfiguration() {
        return repository.findByActiveTrue();
    }

    public boolean testConnection(String host, Integer port, String username, String password) {
        try {
            JavaMailSenderImpl mailSender = createMailSender(host, port, username, password);
            mailSender.testConnection();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional
    public EmailConfiguration saveConfiguration(EmailConfiguration config) {
        // Deactivate all existing configurations
        Iterable<EmailConfiguration> allConfigs = repository.findAll();
        for (EmailConfiguration existingConfig : allConfigs) {
            existingConfig.setActive(false);
            repository.save(existingConfig);
        }

        // Save and activate the new configuration
        config.setActive(true);
        return repository.save(config);
    }

    public JavaMailSender createMailSender(EmailConfiguration config) {
        return createMailSender(config.getHost(), config.getPort(), config.getUsername(), config.getPassword());
    }

    private JavaMailSenderImpl createMailSender(String host, Integer port, String username, String password) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "false");

        return mailSender;
    }
}
