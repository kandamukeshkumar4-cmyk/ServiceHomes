package com.servicehomes.api.messaging.application;

import java.time.Instant;
import java.util.UUID;

public interface MessageEmailNotifier {

    void notifyIfNeeded(NewMessageEmailCommand command);

    record NewMessageEmailCommand(
        UUID threadId,
        UUID newMessageId,
        UUID recipientId,
        String recipientEmail,
        String recipientName,
        String senderName,
        String listingTitle,
        String messagePreview,
        Instant sentAt
    ) {}
}
