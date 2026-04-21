package com.servicehomes.api.analytics.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicehomes.api.analytics.domain.OutboxEvent;
import com.servicehomes.api.analytics.domain.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void publish(String eventType, String aggregateType, String aggregateId, Object payload) {
        publish(eventType, aggregateType, aggregateId, payload, null);
    }

    @Transactional
    public void publish(String eventType, String aggregateType, String aggregateId, Object payload, Map<String, String> metadata) {
        OutboxEvent event = OutboxEvent.builder()
            .eventType(eventType)
            .aggregateType(aggregateType)
            .aggregateId(aggregateId)
            .payload(toJson(payload))
            .metadata(metadata != null ? toJson(metadata) : null)
            .published(false)
            .build();
        outboxEventRepository.save(event);
    }

    @SneakyThrows
    private String toJson(Object obj) {
        return objectMapper.writeValueAsString(obj);
    }
}
