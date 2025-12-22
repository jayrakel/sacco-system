package com.sacco.sacco_system.modules.member.internal.event;

import com.sacco.sacco_system.modules.core.event.DomainEvent;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Event published when a new member is created
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MemberCreatedEvent extends DomainEvent {
    
    private final Member member;
    
    public MemberCreatedEvent(Object source, String aggregateId, Member member) {
        super(source, aggregateId);
        this.member = member;
    }
}




