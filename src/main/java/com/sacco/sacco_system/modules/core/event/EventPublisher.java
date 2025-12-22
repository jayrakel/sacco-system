package com.sacco.sacco_system.modules.core.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Service to publish domain events across the application
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {
    
    private final ApplicationEventPublisher applicationEventPublisher;
    
    public void publish(DomainEvent event) {
        log.info("Publishing event: {} with aggregateId: {}", event.getClass().getSimpleName(), event.getAggregateId());
        applicationEventPublisher.publishEvent(event);
    }
}


