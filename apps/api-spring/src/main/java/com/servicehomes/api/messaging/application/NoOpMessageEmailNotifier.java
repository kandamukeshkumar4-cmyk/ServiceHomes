package com.servicehomes.api.messaging.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "messaging.email.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpMessageEmailNotifier implements MessageEmailNotifier {

    @Override
    public void notifyIfNeeded(NewMessageEmailCommand command) {
        // Notifications are disabled unless an email-capable notifier is configured.
    }
}
