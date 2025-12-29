package com.sacco.sacco_system.modules.loan.api.dto;

import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private BigDecimal principalAmount;
    private BigDecimal loanBalance;
    private Integer duration;
    private String durationUnit;
    private String status;
    private LocalDate applicationDate;
    private LocalDate disbursementDate;

    // Using String for simpler JSON serialization of date
    private String expectedRepaymentDate;

    // ✅ ADDED: For displaying exact meeting time in dashboard
    private String meetingDate;

    // ✅ ADDED: For tracking technical approval date
    private LocalDate approvalDate;

    private BigDecimal weeklyRepaymentAmount;

    // Fee & Voting info
    private boolean applicationFeePaid;
    private BigDecimal processingFee;
    private Integer votesYes;
    private Integer votesNo;

    // ✅ NEW FIELDS FOR LOAN OFFICER
    private BigDecimal memberSavings;
    private BigDecimal memberNetIncome;

    /**
     * ✅ STATIC MAPPER METHOD
     * Converts a Loan Entity to LoanDTO.
     */
    public static LoanDTO fromEntity(Loan loan) {
        Member member = loan.getMember();
        BigDecimal netIncome = BigDecimal.ZERO;

        // Safely extract income if available
        if (member.getEmploymentDetails() != null && member.getEmploymentDetails().getNetMonthlyIncome() != null) {
            netIncome = member.getEmploymentDetails().getNetMonthlyIncome();
        }

        return LoanDTO.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .memberId(member.getId())
                .memberName(member.getFirstName() + " " + member.getLastName())
                .productName(loan.getProduct().getName())
                .principalAmount(loan.getPrincipalAmount())
                .loanBalance(loan.getLoanBalance())
                .duration(loan.getDuration())
                .durationUnit(loan.getDurationUnit() != null ? loan.getDurationUnit().toString() : "MONTHS")
                .status(loan.getStatus().toString())
                .applicationDate(loan.getApplicationDate())
                .disbursementDate(loan.getDisbursementDate())
                .approvalDate(loan.getApprovalDate())
                .weeklyRepaymentAmount(loan.getWeeklyRepaymentAmount())
                .applicationFeePaid(loan.isApplicationFeePaid())
                .processingFee(loan.getProduct().getProcessingFee())
                .votesYes(loan.getVotesYes() != null ? loan.getVotesYes() : 0)
                .votesNo(loan.getVotesNo() != null ? loan.getVotesNo() : 0)
                // Convert Dates to Strings safely
                .expectedRepaymentDate(loan.getExpectedRepaymentDate() != null ? loan.getExpectedRepaymentDate().toString() : null)
                .meetingDate(loan.getMeetingDate() != null ? loan.getMeetingDate().toString() : null)
                // Member Financials
                .memberNetIncome(netIncome)
                .memberSavings(BigDecimal.ZERO) // NOTE: Needs Repository to calculate real savings.
                .build();
    }
}