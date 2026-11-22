package com.payshield.transaction.Services;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.payshield.transaction.Dto.EventDto.OutboxEventMessage;
import com.payshield.transaction.Entity.OutboxEvent;

import tools.jackson.databind.ObjectMapper;

@Service
public class OutboxPublisherService {

    private static final Logger log =
            LoggerFactory.getLogger(OutboxPublisherService.class);

    private static final String TOPIC = "transaction-submitted";

    private final OutboxClaimService claimService;
    private final OutboxDeliveryStateService deliveryStateService;
    private final RetryBackoffService retryBackoffService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisherService(
            OutboxClaimService claimService,
            OutboxDeliveryStateService deliveryStateService,
            RetryBackoffService retryBackoffService,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {

        this.claimService = claimService;
        this.deliveryStateService = deliveryStateService;
        this.retryBackoffService = retryBackoffService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {

        List<OutboxEvent> events = claimService.claimBatch(50);

        for (OutboxEvent event : events) {
            publishEvent(event);
        }
    }

    private void publishEvent(OutboxEvent event) {

        try {
            OutboxEventMessage message = new OutboxEventMessage(
                    event.getId(),
                    event.getEventType(),
                    event.getSchemaVersion(),
                    event.getAggregateType(),
                    event.getAggregateId(),
                    event.getCorrelationId(),
                    event.getCreatedAt(),
                    event.getPayload()
            );

            String json = objectMapper.writeValueAsString(message);

            kafkaTemplate.send(
                    TOPIC,
                    event.getAggregateId().toString(),
                    json
            ).get(130, TimeUnit.SECONDS);

            boolean published = deliveryStateService.markPublished(
                    event.getId(),
                    event.getClaimToken()
            );

            if (!published) {
                log.warn(
                        "Kafka acknowledged event but claim was lost. eventId={}",
                        event.getId()
                );
            } else {
                log.info(
                        "Event published successfully. eventId={}",
                        event.getId()
                );
            }

        } catch (Exception exception) {

            try {
                var nextAttempt = retryBackoffService
                        .calculateNextAttempt(event.getAttempts());

                boolean recorded = deliveryStateService.recordFailure(
                        event.getId(),
                        event.getClaimToken(),
                        nextAttempt,
                        exception.getMessage() == null
                                ? exception.getClass().getSimpleName()
                                : exception.getMessage()
                );

                if (recorded) {
                    log.error(
                            "Event publication failed. eventId={}, attempts={}, nextAttempt={}",
                            event.getId(),
                            event.getAttempts() + 1,
                            nextAttempt,
                            exception
                    );
                } else {
                    log.warn(
                            "Could not record failure because claim was lost. eventId={}",
                            event.getId()
                    );
                }

            } catch (Exception stateException) {
                log.error(
                        "Could not update failure state. eventId={}",
                        event.getId(),
                        stateException
                );
            }
        }
    }
}