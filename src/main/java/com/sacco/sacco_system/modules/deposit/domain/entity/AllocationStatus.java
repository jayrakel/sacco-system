package com.sacco.sacco_system.modules.deposit.domain.entity;

/**
 * Allocation Status
 */
public enum AllocationStatus {
    PENDING,   // Awaiting processing
    COMPLETED, // Successfully routed
    FAILED     // Failed to route
}
