package com.auctionsystem.controllers;

import com.auctionsystem.services.MailService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final MailService mailService;

    @PostMapping("/test-email")
    public ResponseEntity<String> sendTestEmail(@RequestBody @Validated TestEmailRequest request) {
        mailService.sendPlainText(
                request.to(),
                "Auction System - Test SMTP",
                "SMTP integration is active for Auction System backend."
        );
        return ResponseEntity.ok("Email sent");
    }

    public record TestEmailRequest(
            @NotBlank @Email String to
    ) {
    }
}
