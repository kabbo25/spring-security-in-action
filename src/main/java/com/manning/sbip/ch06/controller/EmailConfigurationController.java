package com.manning.sbip.ch06.controller;

import com.manning.sbip.ch06.dto.EmailConfigDto;
import com.manning.sbip.ch06.entity.EmailConfiguration;
import com.manning.sbip.ch06.service.EmailConfigurationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class EmailConfigurationController {

    @Autowired
    private EmailConfigurationService emailConfigurationService;

    @GetMapping("/email-config")
    public String showEmailConfig(Model model) {
        Optional<EmailConfiguration> activeConfig = emailConfigurationService.getActiveConfiguration();

        if (activeConfig.isPresent()) {
            EmailConfiguration config = activeConfig.get();
            EmailConfigDto dto = new EmailConfigDto();
            dto.setHost(config.getHost());
            dto.setPort(config.getPort());
            dto.setUsername(config.getUsername());
            dto.setPassword(config.getPassword());
            model.addAttribute("emailConfig", dto);
            model.addAttribute("hasExistingConfig", true);
        } else {
            model.addAttribute("emailConfig", new EmailConfigDto());
            model.addAttribute("hasExistingConfig", false);
        }

        return "email-config";
    }

    @PostMapping("/email-config/test")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody EmailConfigDto dto) {
        Map<String, Object> response = new HashMap<>();

        boolean success = emailConfigurationService.testConnection(
                dto.getHost(),
                dto.getPort(),
                dto.getUsername(),
                dto.getPassword()
        );

        response.put("success", success);
        response.put("message", success ? "Connection successful!" : "Connection failed. Please check your settings.");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/email-config/save")
    public String saveConfiguration(@Valid @ModelAttribute("emailConfig") EmailConfigDto dto,
                                     BindingResult result,
                                     Model model) {
        if (result.hasErrors()) {
            model.addAttribute("hasExistingConfig", emailConfigurationService.getActiveConfiguration().isPresent());
            return "email-config";
        }

        EmailConfiguration config = new EmailConfiguration();
        config.setHost(dto.getHost());
        config.setPort(dto.getPort());
        config.setUsername(dto.getUsername());
        config.setPassword(dto.getPassword());

        emailConfigurationService.saveConfiguration(config);

        return "redirect:/admin/email-config?success";
    }
}
