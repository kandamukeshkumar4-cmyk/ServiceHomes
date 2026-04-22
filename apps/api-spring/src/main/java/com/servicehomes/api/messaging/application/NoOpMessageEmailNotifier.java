package com.servicehomes.api.messaging.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(MessageEmailNotifier.class)
public class NoOpMessageEmailNotifier implements MessageEmailNotifier {

    @Override
    public void notifyIfNeeded(NewMessageEmailCommand command) {
        // Notifications are disabled unless an email-capable notifier is configured.
    }
}
