package com.sacco.sacco_system.modules.loan.api.dto;

import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanRepayment;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanDTO {
    private UUID id;
    private String loanNumber;
    private UUID memberId;
    private String memberName;
    private String productName;

    // Loan Financials
    private BigDecimal principalAmount;
    private BigDecimal loanBalance;       // Total Outstanding (Principal + Interest)
    private BigDecimal weeklyRepaymentAmount; // Amount due per installment
    private BigDecimal totalArrears;      // ✅ NEW: Amount overdue
    private BigDecimal totalPrepaid;      // ✅ NEW: Excess paid (Wallet)

    // Timeline
    private Integer duration;
    private String durationUnit;
    private String status;
    private LocalDate applicationDate;
    private LocalDate disbursementDate;
    private String expectedRepaymentDate;
    private LocalDate nextPaymentDate;    // ✅ NEW: Critical for "Days Remaining" countdown

    // Metadata
    private String meetingDate;
    private LocalDate approvalDate;
    private boolean applicationFeePaid;
    private BigDecimal processingFee;
    private Integer votesYes;
    private Integer votesNo;
    private BigDecimal memberNetIncome;

    public static LoanDTO fromEntity(Loan loan) {
        Member member = loan.getMember();
        BigDecimal netIncome = BigDecimal.ZERO;
        if (member.getEmploymentDetails() != null && member.getEmploymentDetails().getNetMonthlyIncome() != null) {
            netIncome = member.getEmploymentDetails().getNetMonthlyIncome();
        }

        // ✅ Calculate Next Payment Date (First Pending Installment)
        LocalDate nextDue = null;
        if (loan.getRepayments() != null) {
            nextDue = loan.getRepayments().stream()
                    .filter(r -> r.getStatus() == LoanRepayment.RepaymentStatus.PENDING ||
                            r.getStatus() == LoanRepayment.RepaymentStatus.PARTIALLY_PAID)
                    .min(Comparator.comparing(LoanRepayment::getDueDate))
                    .map(LoanRepayment::getDueDate)
                    .orElse(null);
        }

        return LoanDTO.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .memberId(member.getId())
                .memberName(member.getFirstName() + " " + member.getLastName())
                .productName(loan.getProduct().getName())
                .principalAmount(loan.getPrincipalAmount())
                .loanBalance(loan.getLoanBalance())

                // ✅ Map Financials for Dashboard
                .weeklyRepaymentAmount(loan.getWeeklyRepaymentAmount())
                .totalArrears(loan.getTotalArrears() != null ? loan.getTotalArrears() : BigDecimal.ZERO)
                .totalPrepaid(loan.getTotalPrepaid() != null ? loan.getTotalPrepaid() : BigDecimal.ZERO)
                .nextPaymentDate(nextDue) // Send to frontend

                .duration(loan.getDuration())
                .durationUnit(loan.getDurationUnit() != null ? loan.getDurationUnit().toString() : "MONTHS")
                .status(loan.getStatus().toString())
                .applicationDate(loan.getApplicationDate())
                .disbursementDate(loan.getDisbursementDate())
                .approvalDate(loan.getApprovalDate())
                .applicationFeePaid(loan.isApplicationFeePaid())
                .processingFee(loan.getProduct().getProcessingFee())
                .votesYes(loan.getVotesYes() != null ? loan.getVotesYes() : 0)
                .votesNo(loan.getVotesNo() != null ? loan.getVotesNo() : 0)
                .expectedRepaymentDate(loan.getExpectedRepaymentDate() != null ? loan.getExpectedRepaymentDate().toString() : null)
                .meetingDate(loan.getMeetingDate() != null ? loan.getMeetingDate().toString() : null)
                .memberNetIncome(netIncome)
                .build();
    }
}