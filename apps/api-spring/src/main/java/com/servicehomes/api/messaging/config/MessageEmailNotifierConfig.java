package com.servicehomes.api.messaging.config;

import com.servicehomes.api.messaging.application.MessageEmailNotifier;
import com.servicehomes.api.messaging.application.NoOpMessageEmailNotifier;
import com.servicehomes.api.messaging.application.SesMessageEmailNotifier;
import com.servicehomes.api.messaging.domain.MessageRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
public class MessageEmailNotifierConfig {

    @Bean
    public MessageEmailNotifier messageEmailNotifier(
        MessageRepository messageRepository,
        ObjectProvider<SesClient> sesClientProvider,
        @Value("${messaging.email.enabled:false}") boolean emailEnabled,
        @Value("${messaging.email.from-address:no-reply@servicehomes.example.com}") String fromAddress,
        @Value("${messaging.email.quiet-period-minutes:5}") long quietPeriodMinutes
    ) {
        if (!emailEnabled) {
            return new NoOpMessageEmailNotifier();
        }

        SesClient sesClient = sesClientProvider.getIfAvailable();
        if (sesClient == null) {
            throw new IllegalStateException("messaging.email.enabled=true requires a SesClient bean");
        }

        return new SesMessageEmailNotifier(
            messageRepository,
            sesClient,
            fromAddress,
            quietPeriodMinutes
        );
    }
}
