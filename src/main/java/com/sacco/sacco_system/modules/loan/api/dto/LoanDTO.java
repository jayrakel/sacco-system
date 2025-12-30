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
    private BigDecimal loanBalance;
    private BigDecimal weeklyRepaymentAmount;
    private BigDecimal totalArrears;
    private BigDecimal totalPrepaid;

    // ✅ NEW: Member Financial Context (For Loan Officer)
    private BigDecimal memberSavings;
    private BigDecimal memberNetIncome;

    // Timeline
    private Integer duration;
    private String durationUnit;
    private String status;
    private LocalDate applicationDate;
    private LocalDate disbursementDate;
    private String expectedRepaymentDate;
    private LocalDate nextPaymentDate;

    // Metadata
    private String meetingDate;
    private LocalDate approvalDate;
    private boolean applicationFeePaid;
    private BigDecimal processingFee;
    private Integer votesYes;
    private Integer votesNo;

    public static LoanDTO fromEntity(Loan loan) {
        if (loan == null) return null;

        Member member = loan.getMember();
        BigDecimal netIncome = BigDecimal.ZERO;

        // Extract Income safely
        if (member != null && member.getEmploymentDetails() != null && member.getEmploymentDetails().getNetMonthlyIncome() != null) {
            netIncome = member.getEmploymentDetails().getNetMonthlyIncome();
        }

        LocalDate nextDue = null;
        if (loan.getRepayments() != null) {
            nextDue = loan.getRepayments().stream()
                    .filter(r -> r.getStatus() == LoanRepayment.RepaymentStatus.PENDING ||
                            r.getStatus() == LoanRepayment.RepaymentStatus.PARTIALLY_PAID)
                    .min(Comparator.comparing(LoanRepayment::getDueDate))
                    .map(LoanRepayment::getDueDate)
                    .orElse(null);
        }

        String productName = (loan.getProduct() != null) ? loan.getProduct().getName() : "Unknown Product";
        BigDecimal procFee = (loan.getProduct() != null) ? loan.getProduct().getProcessingFee() : BigDecimal.ZERO;

        return LoanDTO.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .memberId(member != null ? member.getId() : null)
                .memberName(member != null ? member.getFirstName() + " " + member.getLastName() : "Unknown Member")
                .productName(productName)

                .principalAmount(loan.getPrincipalAmount() != null ? loan.getPrincipalAmount() : BigDecimal.ZERO)
                .loanBalance(loan.getLoanBalance() != null ? loan.getLoanBalance() : BigDecimal.ZERO)
                .weeklyRepaymentAmount(loan.getWeeklyRepaymentAmount() != null ? loan.getWeeklyRepaymentAmount() : BigDecimal.ZERO)
                .totalArrears(loan.getTotalArrears() != null ? loan.getTotalArrears() : BigDecimal.ZERO)
                .totalPrepaid(loan.getTotalPrepaid() != null ? loan.getTotalPrepaid() : BigDecimal.ZERO)
                .nextPaymentDate(nextDue)

                // ✅ NOTE: savings is set to 0 here, but we will populate it in the Service!
                .memberSavings(BigDecimal.ZERO)
                .memberNetIncome(netIncome)

                .duration(loan.getDuration())
                .durationUnit(loan.getDurationUnit() != null ? loan.getDurationUnit().toString() : "MONTHS")
                .status(loan.getStatus() != null ? loan.getStatus().toString() : "DRAFT")
                .applicationDate(loan.getApplicationDate())
                .disbursementDate(loan.getDisbursementDate())
                .approvalDate(loan.getApprovalDate())
                .applicationFeePaid(loan.isApplicationFeePaid())
                .processingFee(procFee)
                .votesYes(loan.getVotesYes() != null ? loan.getVotesYes() : 0)
                .votesNo(loan.getVotesNo() != null ? loan.getVotesNo() : 0)
                .expectedRepaymentDate(loan.getExpectedRepaymentDate() != null ? loan.getExpectedRepaymentDate().toString() : null)
                .meetingDate(loan.getMeetingDate() != null ? loan.getMeetingDate().toString() : null)
                .build();
    }
}