package com.sacco.sacco_system.modules.member.internal.event;

import com.sacco.sacco_system.modules.core.event.DomainEvent;
import com.sacco.sacco_system.modules.member.domain.entity.MemberStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Event published when member status changes
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MemberStatusChangedEvent extends DomainEvent {
    
    private final MemberStatus oldStatus;
    
    private final MemberStatus newStatus;
    
    public MemberStatusChangedEvent(Object source, String aggregateId, MemberStatus oldStatus, MemberStatus newStatus) {
        super(source, aggregateId);
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }
}




