package com.servicehomes.api.messaging.application;

public class NoOpMessageEmailNotifier implements MessageEmailNotifier {

    @Override
    public void notifyIfNeeded(NewMessageEmailCommand command) {
        // Notifications are disabled unless an email-capable notifier is configured.
    }
}
