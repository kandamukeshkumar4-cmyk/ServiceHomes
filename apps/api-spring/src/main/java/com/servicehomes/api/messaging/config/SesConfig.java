package com.servicehomes.api.messaging.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

import java.net.URI;

@Configuration
public class SesConfig {

    @Bean
    @ConditionalOnProperty(name = "messaging.email.enabled", havingValue = "true")
    public SesClient sesClient(
        @Value("${aws.ses.region}") String region,
        @Value("${aws.ses.endpoint:}") String endpoint,
        @Value("${aws.ses.access-key:}") String accessKey,
        @Value("${aws.ses.secret-key:}") String secretKey
    ) {
        var builder = SesClient.builder()
            .region(Region.of(region));

        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        if (!accessKey.isBlank() && !secretKey.isBlank()) {
            builder.credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
            );
        }

        return builder.build();
    }
}
