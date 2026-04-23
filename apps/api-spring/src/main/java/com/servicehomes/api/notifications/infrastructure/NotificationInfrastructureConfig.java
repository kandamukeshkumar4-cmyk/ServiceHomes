package com.servicehomes.api.notifications.infrastructure;

import jakarta.servlet.ServletException;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

import java.net.URI;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class NotificationInfrastructureConfig {

    private final NotificationWebSocketEndpoint notificationWebSocketEndpoint;

    @Bean
    public ServletContextInitializer notificationWebSocketInitializer() {
        return servletContext -> {
            Object container = servletContext.getAttribute(ServerContainer.class.getName());
            if (!(container instanceof ServerContainer serverContainer)) {
                log.warn("Jakarta WebSocket container is unavailable; notification WebSocket endpoint is disabled");
                return;
            }

            ServerEndpointConfig endpointConfig = ServerEndpointConfig.Builder
                .create(NotificationWebSocketEndpoint.class, NotificationWebSocketEndpoint.PATH)
                .configurator(new SpringEndpointConfigurator(notificationWebSocketEndpoint))
                .build();

            try {
                serverContainer.addEndpoint(endpointConfig);
            } catch (DeploymentException ex) {
                throw new ServletException("Failed to register notification WebSocket endpoint", ex);
            }
        };
    }

    @Bean
    @ConditionalOnProperty(name = "notifications.email.enabled", havingValue = "true")
    @ConditionalOnMissingBean(SesClient.class)
    public SesClient notificationSesClient(
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

    private static final class SpringEndpointConfigurator extends ServerEndpointConfig.Configurator {
        private final NotificationWebSocketEndpoint endpoint;

        private SpringEndpointConfigurator(NotificationWebSocketEndpoint endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public <T> T getEndpointInstance(Class<T> endpointClass) {
            return endpointClass.cast(endpoint);
        }
    }
}
