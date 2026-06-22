package com.auctionsystem.services;

import com.mailersend.sdk.MailerSend;
import com.mailersend.sdk.MailerSendResponse;
import com.mailersend.sdk.emails.Email;
import com.mailersend.sdk.exceptions.MailerSendException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final String provider;
    private final String fromAddress;
    private final String fromName;
    private final String mailerSendToken;
    private final String resendToken;

    private final String overrideTo;

    public MailService(
            JavaMailSender mailSender,
            @Value("${app.mail.provider:smtp}") String provider,
            @Value("${app.mail.from}") String fromAddress,
            @Value("${app.mail.from-name:Auction System}") String fromName,
            @Value("${app.mail.mailersend.token:}") String mailerSendToken,
            @Value("${app.mail.resend.token:}") String resendToken,
            @Value("${app.mail.override-to:}") String overrideTo
    ) {
        this.mailSender = mailSender;
        this.provider = provider;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
        this.mailerSendToken = mailerSendToken;
        this.resendToken = resendToken;
        this.overrideTo = overrideTo;
    }

    public void sendPlainText(String to, String subject, String body) {
        if (overrideTo != null && !overrideTo.isBlank()) {
            body = "[Para: " + to + "]\n" + body;
            to = overrideTo;
        }
        switch (provider.toLowerCase(Locale.ROOT)) {
            case "resend" -> sendWithResend(to, subject, body);
            case "mailersend" -> sendWithMailerSend(to, subject, body);
            default -> {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromAddress);
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
            }
        }
    }

    private void sendWithResend(String to, String subject, String body) {
        if (resendToken == null || resendToken.isBlank()) {
            throw new IllegalStateException("RESEND_API_TOKEN no configurado");
        }
        Map<String, Object> payload = Map.of(
                "from", fromName + " <" + fromAddress + ">",
                "to", List.of(to),
                "subject", subject,
                "text", body
        );
        try {
            RestClient.create().post()
                    .uri("https://api.resend.com/emails")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + resendToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (org.springframework.web.client.RestClientResponseException ex) {
            throw new IllegalStateException(
                    "Resend rechazo el email (HTTP " + ex.getStatusCode() + "): " + ex.getResponseBodyAsString(), ex);
        }
    }

    private void sendWithMailerSend(String to, String subject, String body) {
        if (mailerSendToken == null || mailerSendToken.isBlank()) {
            throw new IllegalStateException("MAILERSEND_API_TOKEN no configurado");
        }

        Email email = new Email();
        email.setFrom(fromName, fromAddress);
        email.addRecipient("Cliente", to.toLowerCase(Locale.ROOT));
        email.setSubject(subject);
        email.setPlain(body);
        email.setHtml("<p>" + escapeHtml(body) + "</p>");

        MailerSend mailerSend = new MailerSend();
        mailerSend.setToken(mailerSendToken);
        try {
            MailerSendResponse response = mailerSend.emails().send(email);
            if (response == null || response.messageId == null || response.messageId.isBlank()) {
                throw new IllegalStateException("MailerSend no devolvio messageId");
            }
        } catch (MailerSendException ex) {
            throw new IllegalStateException("Error enviando email con MailerSend: " + ex.getMessage(), ex);
        }
    }

    private String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
