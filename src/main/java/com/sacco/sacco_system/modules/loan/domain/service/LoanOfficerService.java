package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.core.exception.ApiException;
import com.sacco.sacco_system.modules.loan.api.dto.LoanReviewDTO;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.notification.domain.service.EmailService;
import com.sacco.sacco_system.modules.notification.domain.service.SmsService;
import com.sacco.sacco_system.modules.audit.domain.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for Loan Officer actions: Review, Approve, Reject loans
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoanOfficerService {

    private final LoanRepository loanRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final AuditService auditService;

    /**
     * Loan Officer reviews a submitted loan and moves to UNDER_REVIEW status
     */
    @Transactional
    public Loan startReview(UUID loanId, String officerEmail) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ApiException("Loan not found", 404));

        if (loan.getLoanStatus() != Loan.LoanStatus.SUBMITTED) {
            throw new ApiException("Only SUBMITTED loans can be reviewed. Current status: " + loan.getLoanStatus(), 400);
        }

        loan.setLoanStatus(Loan.LoanStatus.UNDER_REVIEW);
        loan.setUpdatedBy(officerEmail);

        Loan saved = loanRepository.save(loan);

        // Audit trail
        auditService.logLoanAction(
                officerEmail,
                "LOAN_REVIEW_STARTED",
                "Loan " + loan.getLoanNumber() + " moved to Under Review",
                loan.getId().toString()
        );

        // Notify applicant
        try {
            emailService.sendLoanStatusEmail(
                    loan.getMember().getEmail(),
                    loan.getLoanNumber(),
                    "Under Review - Your loan application is now being reviewed by our team."
            );

            smsService.sendSms(
                    loan.getMember().getPhoneNumber(),
                    "Your loan application " + loan.getLoanNumber() + " is now under review. You will be notified of the decision soon."
            );
        } catch (Exception e) {
            log.error("Failed to send review notifications", e);
        }

        log.info("✅ Loan {} moved to UNDER_REVIEW by {}", loan.getLoanNumber(), officerEmail);

        return saved;
    }

    /**
     * Loan Officer approves a loan
     */
    @Transactional
    public Loan approveLoan(UUID loanId, String officerEmail, BigDecimal approvedAmount, String approvalNotes) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ApiException("Loan not found", 404));

        // Validate status
        if (loan.getLoanStatus() != Loan.LoanStatus.UNDER_REVIEW &&
                loan.getLoanStatus() != Loan.LoanStatus.SUBMITTED) {
            throw new ApiException("Only loans UNDER_REVIEW or SUBMITTED can be approved. Current: " + loan.getLoanStatus(), 400);
        }

        // Validate approved amount
        if (approvedAmount == null || approvedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("Approved amount must be greater than zero", 400);
        }

        if (approvedAmount.compareTo(loan.getPrincipalAmount()) > 0) {
            throw new ApiException("Approved amount cannot exceed requested amount", 400);
        }

        // Update loan
        loan.setLoanStatus(Loan.LoanStatus.APPROVED);
        loan.setApprovedAmount(approvedAmount);
        loan.setApprovalDate(LocalDate.now());
        loan.setUpdatedBy(officerEmail);

        Loan saved = loanRepository.save(loan);

        // Audit trail
        auditService.logLoanAction(
                officerEmail,
                "LOAN_APPROVED",
                String.format("Loan %s APPROVED. Requested: %s, Approved: %s. Notes: %s",
                        loan.getLoanNumber(),
                        loan.getPrincipalAmount(),
                        approvedAmount,
                        approvalNotes != null ? approvalNotes : "None"),
                loan.getId().toString()
        );

        // Notify applicant
        try {
            emailService.sendLoanApprovalEmail(
                    loan.getMember().getEmail(),
                    loan.getMember().getFirstName(),
                    loan.getLoanNumber(),
                    approvedAmount,
                    loan.getProduct().getProductName()
            );

            smsService.sendSms(
                    loan.getMember().getPhoneNumber(),
                    String.format("🎉 Congratulations! Your loan %s has been APPROVED for KES %s. Awaiting disbursement.",
                            loan.getLoanNumber(),
                            approvedAmount.toPlainString())
            );
        } catch (Exception e) {
            log.error("Failed to send approval notifications", e);
        }

        log.info("✅ Loan {} APPROVED by {} for amount {}", loan.getLoanNumber(), officerEmail, approvedAmount);

        return saved;
    }

    /**
     * Loan Officer rejects a loan
     */
    @Transactional
    public Loan rejectLoan(UUID loanId, String officerEmail, String rejectionReason) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ApiException("Loan not found", 404));

        // Validate status
        if (loan.getLoanStatus() != Loan.LoanStatus.UNDER_REVIEW &&
                loan.getLoanStatus() != Loan.LoanStatus.SUBMITTED) {
            throw new ApiException("Only loans UNDER_REVIEW or SUBMITTED can be rejected. Current: " + loan.getLoanStatus(), 400);
        }

        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new ApiException("Rejection reason is required", 400);
        }

        // Update loan status
        loan.setLoanStatus(Loan.LoanStatus.REJECTED);
        loan.setUpdatedBy(officerEmail);

        // ✅ CRITICAL FIX: Automatically deactivate the loan and zero balances
        loan.setActive(false);
        loan.setTotalOutstandingAmount(BigDecimal.ZERO);
        loan.setOutstandingPrincipal(BigDecimal.ZERO);
        loan.setOutstandingInterest(BigDecimal.ZERO);

        Loan saved = loanRepository.save(loan);

        // Audit trail
        auditService.logLoanAction(
                officerEmail,
                "LOAN_REJECTED",
                String.format("Loan %s REJECTED. Reason: %s", loan.getLoanNumber(), rejectionReason),
                loan.getId().toString()
        );

        // Notify applicant
        try {
            emailService.sendLoanRejectionEmail(
                    loan.getMember().getEmail(),
                    loan.getMember().getFirstName(),
                    loan.getLoanNumber(),
                    rejectionReason
            );

            smsService.sendSms(
                    loan.getMember().getPhoneNumber(),
                    String.format("Your loan application %s has been declined. Reason: %s. Contact us for more information.",
                            loan.getLoanNumber(),
                            rejectionReason.length() > 100 ? rejectionReason.substring(0, 100) + "..." : rejectionReason)
            );
        } catch (Exception e) {
            log.error("Failed to send rejection notifications", e);
        }

        log.warn("❌ Loan {} REJECTED by {}. Reason: {}", loan.getLoanNumber(), officerEmail, rejectionReason);

        return saved;
    }

    /**
     * Request more information from applicant
     */
    @Transactional
    public Loan requestMoreInformation(UUID loanId, String officerEmail, String informationNeeded) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ApiException("Loan not found", 404));

        if (informationNeeded == null || informationNeeded.trim().isEmpty()) {
            throw new ApiException("Please specify what information is needed", 400);
        }

        loan.setUpdatedBy(officerEmail);
        Loan saved = loanRepository.save(loan);

        // Audit trail
        auditService.logLoanAction(
                officerEmail,
                "LOAN_INFO_REQUESTED",
                String.format("Additional info requested for loan %s: %s", loan.getLoanNumber(), informationNeeded),
                loan.getId().toString()
        );

        // Notify applicant
        try {
            emailService.sendGenericEmail(
                    loan.getMember().getEmail(),
                    "Additional Information Required - " + loan.getLoanNumber(),
                    String.format("Hello,\n\nWe need additional information for your loan application %s:\n\n%s\n\nPlease contact us or visit our office.\n\nThank you.",
                            loan.getLoanNumber(),
                            informationNeeded)
            );

            smsService.sendSms(
                    loan.getMember().getPhoneNumber(),
                    String.format("Additional info needed for loan %s. Please check your email or contact us.",
                            loan.getLoanNumber())
            );
        } catch (Exception e) {
            log.error("Failed to send info request notifications", e);
        }

        log.info("📋 Additional info requested for loan {} by {}", loan.getLoanNumber(), officerEmail);

        return saved;
    }

    /**
     * Get loan details for review (returns DTO to avoid circular references)
     */
    @Transactional(readOnly = true)
    public LoanReviewDTO getLoanForReview(UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ApiException("Loan not found", 404));

        // Force load lazy associations to avoid LazyInitializationException
        if (loan.getMember() != null) {
            loan.getMember().getFirstName();
            loan.getMember().getLastName();
            loan.getMember().getEmail();
            loan.getMember().getPhoneNumber();
            loan.getMember().getMemberNumber();
            loan.getMember().getMemberStatus();
            loan.getMember().getCreatedAt();
        }

        if (loan.getProduct() != null) {
            loan.getProduct().getProductName();
            loan.getProduct().getProductCode();
        }

        if (loan.getGuarantors() != null && !loan.getGuarantors().isEmpty()) {
            loan.getGuarantors().forEach(guarantor -> {
                guarantor.getGuaranteedAmount();
                guarantor.getStatus();
                guarantor.getId();

                if (guarantor.getMember() != null) {
                    guarantor.getMember().getFirstName();
                    guarantor.getMember().getLastName();
                    guarantor.getMember().getMemberNumber();
                    guarantor.getMember().getEmail();
                }
            });
        }

        log.info("✅ Loaded loan {} for review by officer with {} guarantors",
                loan.getLoanNumber(),
                loan.getGuarantors() != null ? loan.getGuarantors().size() : 0);

        return convertToReviewDTO(loan);
    }

    private LoanReviewDTO convertToReviewDTO(Loan loan) {
        return LoanReviewDTO.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .principalAmount(loan.getPrincipalAmount())
                .interestRate(loan.getInterestRate())
                .durationWeeks(loan.getDurationWeeks())
                .weeklyRepaymentAmount(loan.getWeeklyRepaymentAmount())
                .loanStatus(loan.getLoanStatus().name())
                .applicationDate(loan.getApplicationDate())
                .approvalDate(loan.getApprovalDate())
                .approvedAmount(loan.getApprovedAmount())
                .product(LoanReviewDTO.ProductInfo.builder()
                        .id(loan.getProduct().getId())
                        .productCode(loan.getProduct().getProductCode())
                        .productName(loan.getProduct().getProductName())
                        .interestRate(loan.getProduct().getInterestRate())
                        .build())
                .member(LoanReviewDTO.MemberInfo.builder()
                        .id(loan.getMember().getId())
                        .memberNumber(loan.getMember().getMemberNumber())
                        .firstName(loan.getMember().getFirstName())
                        .lastName(loan.getMember().getLastName())
                        .email(loan.getMember().getEmail())
                        .phoneNumber(loan.getMember().getPhoneNumber())
                        .memberStatus(loan.getMember().getMemberStatus().name())
                        .createdAt(loan.getMember().getCreatedAt() != null ? loan.getMember().getCreatedAt().toString() : null)
                        .build())
                .guarantors(loan.getGuarantors() != null ?
                        loan.getGuarantors().stream()
                                .map(g -> LoanReviewDTO.GuarantorInfo.builder()
                                        .id(g.getId())
                                        .guaranteedAmount(g.getGuaranteedAmount())
                                        .status(g.getStatus().name())
                                        .member(LoanReviewDTO.MemberInfo.builder()
                                                .id(g.getMember().getId())
                                                .memberNumber(g.getMember().getMemberNumber())
                                                .firstName(g.getMember().getFirstName())
                                                .lastName(g.getMember().getLastName())
                                                .email(g.getMember().getEmail())
                                                .phoneNumber(g.getMember().getPhoneNumber())
                                                .memberStatus(g.getMember().getMemberStatus().name())
                                                .createdAt(null)
                                                .build())
                                        .build())
                                .collect(Collectors.toList())
                        : null)
                .build();
    }
}