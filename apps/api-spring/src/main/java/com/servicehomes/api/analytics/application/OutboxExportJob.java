package com.servicehomes.api.analytics.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicehomes.api.analytics.domain.OutboxEvent;
import com.servicehomes.api.analytics.domain.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j

@Component
@RequiredArgsConstructor
public class OutboxExportJob {

    private final OutboxEventRepository outboxEventRepository;
    private final S3Client s3Client;
    private final ObjectMapper objectMapper;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void export() {
        List<OutboxEvent> events = outboxEventRepository.findByPublishedFalseOrderByCreatedAtAsc();
        if (events.isEmpty()) {
            return;
        }

        try {
            String timestamp = DateTimeFormatter.ofPattern("yyyy/MM/dd/HH-mm-ss").format(Instant.now().atOffset(ZoneOffset.UTC));
            String key = "events/raw/" + timestamp + ".jsonl";

            String content = events.stream()
                .map(this::toJsonLine)
                .collect(Collectors.joining("\n"));

            s3Client.putObject(PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("application/jsonlines")
                .build(), RequestBody.fromBytes(content.getBytes(StandardCharsets.UTF_8)));

            Instant now = Instant.now();
            events.forEach(e -> {
                e.setPublished(true);
                e.setPublishedAt(now);
            });
            outboxEventRepository.saveAll(events);
            log.info("Exported {} events to s3://{}/{}", events.size(), bucket, key);
        } catch (Exception e) {
            log.warn("Failed to export outbox events to S3: {}", e.getMessage());
        }
    }

    @SneakyThrows
    private String toJsonLine(OutboxEvent event) {
        return objectMapper.writeValueAsString(java.util.Map.of(
            "id", event.getId().toString(),
            "eventType", event.getEventType(),
            "eventVersion", event.getEventVersion(),
            "aggregateType", event.getAggregateType(),
            "aggregateId", event.getAggregateId(),
            "payload", objectMapper.readTree(event.getPayload()),
            "metadata", event.getMetadata() != null ? objectMapper.readTree(event.getMetadata()) : null,
            "createdAt", event.getCreatedAt().toString()
        ));
    }
}
