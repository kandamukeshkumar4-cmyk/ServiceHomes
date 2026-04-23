package com.servicehomes.api.notifications.application;

import com.servicehomes.api.notifications.application.dto.NotificationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@Slf4j
public class EmailNotificationService {

    private static final String HTML_TEMPLATE = "templates/email/notification.html";
    private static final String TEXT_TEMPLATE = "templates/email/notification.txt";

    private final ObjectProvider<SesClient> sesClientProvider;
    private final boolean enabled;
    private final String fromAddress;
    private final String defaultActionUrl;

    public EmailNotificationService(
        ObjectProvider<SesClient> sesClientProvider,
        @Value("${notifications.email.enabled:${messaging.email.enabled:false}}") boolean enabled,
        @Value("${notifications.email.from-address:${messaging.email.from-address:no-reply@servicehomes.example.com}}") String fromAddress,
        @Value("${notifications.email.default-action-url:https://servicehomes.example.com}") String defaultActionUrl
    ) {
        this.sesClientProvider = sesClientProvider;
        this.enabled = enabled;
        this.fromAddress = fromAddress;
        this.defaultActionUrl = defaultActionUrl;
    }

    public EmailDeliveryResult send(NotificationMessage notification) {
        if (!enabled) {
            return EmailDeliveryResult.skipped("email_disabled");
        }

        if (!StringUtils.hasText(notification.recipientEmail())) {
            return EmailDeliveryResult.failed("missing_recipient_email", false);
        }

        SesClient sesClient = sesClientProvider.getIfAvailable();
        if (sesClient == null) {
            return EmailDeliveryResult.failed("ses_client_unavailable", true);
        }

        try {
            SendEmailResponse response = sesClient.sendEmail(buildEmail(notification));
            return EmailDeliveryResult.sent(response.messageId());
        } catch (RuntimeException ex) {
            log.warn("Failed to send notification email {} to {}: {}",
                notification.id(), notification.recipientUserId(), ex.getMessage());
            return EmailDeliveryResult.failed(ex.getClass().getSimpleName(), true);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    private SendEmailRequest buildEmail(NotificationMessage notification) {
        Map<String, String> textValues = templateValues(notification, false);
        Map<String, String> htmlValues = templateValues(notification, true);

        software.amazon.awssdk.services.ses.model.Message emailMessage =
            software.amazon.awssdk.services.ses.model.Message.builder()
                .subject(Content.builder()
                    .charset(StandardCharsets.UTF_8.name())
                    .data(subject(notification))
                    .build())
                .body(Body.builder()
                    .html(Content.builder()
                        .charset(StandardCharsets.UTF_8.name())
                        .data(renderTemplate(HTML_TEMPLATE, htmlValues))
                        .build())
                    .text(Content.builder()
                        .charset(StandardCharsets.UTF_8.name())
                        .data(renderTemplate(TEXT_TEMPLATE, textValues))
                        .build())
                    .build())
                .build();

        return SendEmailRequest.builder()
            .source(fromAddress)
            .destination(Destination.builder().toAddresses(notification.recipientEmail()).build())
            .message(emailMessage)
            .build();
    }

    private String subject(NotificationMessage notification) {
        return notification.title().replaceAll("\\R+", " ").trim();
    }

    private Map<String, String> templateValues(NotificationMessage notification, boolean html) {
        String actionUrl = StringUtils.hasText(notification.actionUrl())
            ? notification.actionUrl().trim()
            : defaultActionUrl;

        Map<String, String> values = Map.of(
            "recipientName", fallback(notification.recipientName(), "there"),
            "title", notification.title(),
            "body", notification.body(),
            "actionUrl", actionUrl,
            "notificationType", notification.type().name()
        );

        if (!html) {
            return values;
        }

        return values.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                entry -> HtmlUtils.htmlEscape(entry.getValue(), StandardCharsets.UTF_8.name())
            ));
    }

    private String renderTemplate(String path, Map<String, String> values) {
        String template = loadTemplate(path);
        for (Map.Entry<String, String> entry : values.entrySet()) {
            template = template.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return template;
    }

    private String loadTemplate(String path) {
        try {
            return StreamUtils.copyToString(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Email template missing: " + path, ex);
        }
    }

    private String fallback(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    public record EmailDeliveryResult(
        boolean attempted,
        boolean sent,
        String reason,
        String providerMessageId,
        boolean retryable
    ) {
        public static EmailDeliveryResult skipped(String reason) {
            return new EmailDeliveryResult(false, false, reason, null, false);
        }

        public static EmailDeliveryResult sent(String providerMessageId) {
            return new EmailDeliveryResult(true, true, "sent", providerMessageId, false);
        }

        public static EmailDeliveryResult failed(String reason, boolean retryable) {
            return new EmailDeliveryResult(true, false, reason, null, retryable);
        }
    }
}
