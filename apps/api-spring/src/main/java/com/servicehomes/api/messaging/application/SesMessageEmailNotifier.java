package com.servicehomes.api.messaging.application;

import com.servicehomes.api.messaging.domain.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class SesMessageEmailNotifier implements MessageEmailNotifier {

    private static final String HTML_TEMPLATE = "templates/email/message-notification.html";
    private static final String TEXT_TEMPLATE = "templates/email/message-notification.txt";

    private final MessageRepository messageRepository;
    private final SesClient sesClient;
    private final String fromAddress;
    private final long quietPeriodMinutes;

    @Override
    public void notifyIfNeeded(NewMessageEmailCommand command) {
        if (command.recipientEmail() == null || command.recipientEmail().isBlank()) {
            return;
        }

        Instant cutoff = command.sentAt().minus(Duration.ofMinutes(quietPeriodMinutes));
        if (wasRecipientRecentlyActive(command, cutoff) || hasRecentUnreadAlready(command, cutoff)) {
            return;
        }

        try {
            sendEmail(command);
        } catch (RuntimeException ex) {
            log.warn("Failed to send message notification email for thread {}: {}", command.threadId(), ex.getMessage());
        }
    }

    private boolean wasRecipientRecentlyActive(NewMessageEmailCommand command, Instant cutoff) {
        Instant lastReadAt = messageRepository.findLatestReadAtForRecipient(command.threadId(), command.recipientId());
        return lastReadAt != null && !lastReadAt.isBefore(cutoff);
    }

    private boolean hasRecentUnreadAlready(NewMessageEmailCommand command, Instant cutoff) {
        return messageRepository.countRecentUnreadMessages(
            command.threadId(),
            command.recipientId(),
            cutoff,
            command.newMessageId()
        ) > 0;
    }

    private void sendEmail(NewMessageEmailCommand command) {
        Map<String, String> values = Map.of(
            "recipientName", fallback(command.recipientName(), "there"),
            "listingTitle", fallback(command.listingTitle(), "your reservation"),
            "senderName", fallback(command.senderName(), "Someone"),
            "messagePreview", fallback(command.messagePreview(), "Open ServiceHomes to read the latest message.")
        );

        Message emailMessage = Message.builder()
            .subject(Content.builder().charset(StandardCharsets.UTF_8.name()).data(subject(command)).build())
            .body(Body.builder()
                .html(Content.builder().charset(StandardCharsets.UTF_8.name()).data(renderTemplate(HTML_TEMPLATE, values)).build())
                .text(Content.builder().charset(StandardCharsets.UTF_8.name()).data(renderTemplate(TEXT_TEMPLATE, values)).build())
                .build())
            .build();

        sesClient.sendEmail(SendEmailRequest.builder()
            .source(fromAddress)
            .destination(Destination.builder().toAddresses(command.recipientEmail()).build())
            .message(emailMessage)
            .build());
    }

    private String subject(NewMessageEmailCommand command) {
        return "You have a new message about " + fallback(command.listingTitle(), "your reservation");
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
        return value == null || value.isBlank() ? fallback : value;
    }
}
