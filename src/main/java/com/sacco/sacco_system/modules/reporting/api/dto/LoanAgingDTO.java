package com.sacco.sacco_system.modules.reporting.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Minimal placeholder LoanAgingDTO to satisfy reporting references after Loans removal.
 */
public class LoanAgingDTO {
    private UUID loanId;
    private String loanNumber;
    private BigDecimal outstandingBalance = BigDecimal.ZERO;

    public LoanAgingDTO() {}

    public LoanAgingDTO(UUID loanId, String loanNumber, BigDecimal outstandingBalance) {
        this.loanId = loanId;
        this.loanNumber = loanNumber;
        this.outstandingBalance = outstandingBalance;
    }

    // getters/setters
    public UUID getLoanId() { return loanId; }
    public void setLoanId(UUID loanId) { this.loanId = loanId; }
    public String getLoanNumber() { return loanNumber; }
    public void setLoanNumber(String loanNumber) { this.loanNumber = loanNumber; }
    public BigDecimal getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(BigDecimal outstandingBalance) { this.outstandingBalance = outstandingBalance; }
}
