package com.sacco.sacco_system.modules.core.event;

import lombok.Data;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all domain events
 * Enables event-driven communication between modules
 */
@Data
public abstract class DomainEvent extends ApplicationEvent {
    
    private final String eventId;
    private final LocalDateTime occurredAt;
    private final String aggregateId;
    
    public DomainEvent(Object source, String aggregateId) {
        super(source);
        this.eventId = UUID.randomUUID().toString();
        this.aggregateId = aggregateId;
        this.occurredAt = LocalDateTime.now();
    }
}


