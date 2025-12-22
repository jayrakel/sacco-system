package com.sacco.sacco_system.modules.deposit.domain.entity;

/**
 * Deposit Destination Type
 * Defines where the deposit money can be routed
 */
public enum DepositDestinationType {
    SAVINGS_ACCOUNT,      // To member's savings account
    LOAN_REPAYMENT,       // To pay off a loan
    FINE_PAYMENT,         // To clear fines/penalties
    CONTRIBUTION_PRODUCT, // To custom contribution products (meat, harambee, etc.)
    SHARE_CAPITAL         // To member's share capital
}
