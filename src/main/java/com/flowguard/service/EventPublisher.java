package com.flowguard.service;

import com.flowguard.event.RequestEvent;

/**
 * Interface for publishing request events.
 * - Dev profile: InMemoryEventPublisher (no Kafka needed)
 * - Prod profile: KafkaEventPublisher (real Kafka)
 */
public interface EventPublisher {
    void publishRequestEvent(RequestEvent event);
}
