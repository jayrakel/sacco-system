package com.sacco.sacco_system.modules.deposit.domain.entity;

/**
 * Deposit Status
 */
public enum DepositStatus {
    PENDING,     // Awaiting processing
    PROCESSING,  // Currently being routed
    COMPLETED,   // Successfully processed
    FAILED,      // Processing failed
    REVERSED     // Deposit was reversed
}
