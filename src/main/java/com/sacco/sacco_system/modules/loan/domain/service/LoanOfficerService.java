package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.core.exception.ApiException;
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
        String memberEmail = loan.getMember().getEmail();
        String memberPhone = loan.getMember().getPhoneNumber();

        emailService.sendLoanStatusEmail(
                memberEmail,
                loan.getLoanNumber(),
                "Under Review - Your loan application is now being reviewed by our team."
        );

        smsService.sendSms(
                memberPhone,
                "Your loan application " + loan.getLoanNumber() + " is now under review. You will be notified of the decision soon."
        );

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
        String memberEmail = loan.getMember().getEmail();
        String memberPhone = loan.getMember().getPhoneNumber();
        String memberName = loan.getMember().getFirstName();

        emailService.sendLoanApprovalEmail(
                memberEmail,
                memberName,
                loan.getLoanNumber(),
                approvedAmount,
                loan.getProduct().getProductName()
        );

        smsService.sendSms(
                memberPhone,
                String.format("🎉 Congratulations! Your loan %s has been APPROVED for KES %s. Awaiting disbursement.",
                        loan.getLoanNumber(),
                        approvedAmount.toPlainString())
        );

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

        // Update loan
        loan.setLoanStatus(Loan.LoanStatus.REJECTED);
        loan.setUpdatedBy(officerEmail);

        Loan saved = loanRepository.save(loan);

        // Audit trail
        auditService.logLoanAction(
                officerEmail,
                "LOAN_REJECTED",
                String.format("Loan %s REJECTED. Reason: %s", loan.getLoanNumber(), rejectionReason),
                loan.getId().toString()
        );

        // Notify applicant
        String memberEmail = loan.getMember().getEmail();
        String memberPhone = loan.getMember().getPhoneNumber();
        String memberName = loan.getMember().getFirstName();

        emailService.sendLoanRejectionEmail(
                memberEmail,
                memberName,
                loan.getLoanNumber(),
                rejectionReason
        );

        smsService.sendSms(
                memberPhone,
                String.format("Your loan application %s has been declined. Reason: %s. Contact us for more information.",
                        loan.getLoanNumber(),
                        rejectionReason.length() > 100 ? rejectionReason.substring(0, 100) + "..." : rejectionReason)
        );

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
        String memberEmail = loan.getMember().getEmail();
        String memberPhone = loan.getMember().getPhoneNumber();

        emailService.sendGenericEmail(
                memberEmail,
                "Additional Information Required - " + loan.getLoanNumber(),
                String.format("Hello,\n\nWe need additional information for your loan application %s:\n\n%s\n\nPlease contact us or visit our office.\n\nThank you.",
                        loan.getLoanNumber(),
                        informationNeeded)
        );

        smsService.sendSms(
                memberPhone,
                String.format("Additional info needed for loan %s. Please check your email or contact us.",
                        loan.getLoanNumber())
        );

        log.info("📋 Additional info requested for loan {} by {}", loan.getLoanNumber(), officerEmail);

        return saved;
    }

    /**
     * Get loan details for review (with guarantor info, member history, etc.)
     */
    @Transactional(readOnly = true)
    public Loan getLoanForReview(UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ApiException("Loan not found", 404));
// start of copilot change
        // Force load lazy associations to avoid LazyInitializationException
        // Load member details
        if (loan.getMember() != null) {
            loan.getMember().getFirstName();
            loan.getMember().getLastName();
            loan.getMember().getEmail();
            loan.getMember().getPhoneNumber();
            loan.getMember().getMemberNumber();
            loan.getMember().getMemberStatus();
            loan.getMember().getCreatedAt();
        }

        // Load product details
        if (loan.getProduct() != null) {
            loan.getProduct().getProductName();
            loan.getProduct().getProductCode();
        }

        // Load guarantors and their member details
        if (loan.getGuarantors() != null && !loan.getGuarantors().isEmpty()) {
            loan.getGuarantors().forEach(guarantor -> {
                guarantor.getGuaranteedAmount();
                guarantor.getStatus();
                guarantor.getId();

                // Load guarantor's member details
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

        return loan;
    }
}

