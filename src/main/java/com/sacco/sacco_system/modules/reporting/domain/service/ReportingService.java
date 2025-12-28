package com.sacco.sacco_system.modules.reporting.domain.service;

import com.sacco.sacco_system.modules.admin.domain.entity.SystemSetting;
import com.sacco.sacco_system.modules.admin.domain.repository.SystemSettingRepository;
import com.sacco.sacco_system.modules.reporting.api.dto.LoanAgingDTO;
import com.sacco.sacco_system.modules.reporting.api.dto.MemberStatementDTO;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.finance.domain.entity.Transaction;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportingService {

    private final TransactionRepository transactionRepository;
    private final MemberRepository memberRepository;
    private final SystemSettingRepository systemSettingRepository;

    @Transactional(readOnly = true)
    public MemberStatementDTO getMemberStatement(UUID memberId, LocalDate startDate, LocalDate endDate) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // 1. Organization Config
        List<SystemSetting> settings = systemSettingRepository.findAll();
        Map<String, String> config = settings.stream()
                .collect(Collectors.toMap(SystemSetting::getKey, SystemSetting::getValue, (a, b) -> b));

        String orgName = config.getOrDefault("ORGANIZATION_NAME", "Sacco System");
        String orgAddress = config.getOrDefault("ORGANIZATION_ADDRESS", "P.O. Box 0000");
        String orgEmail = config.getOrDefault("ORGANIZATION_EMAIL", "info@sacco.com");
        String orgLogo = config.getOrDefault("ORGANIZATION_LOGO", "");

        // 2. Fetch All Transactions
        List<Transaction> allTransactions = transactionRepository.findByMemberIdOrderByTransactionDateDesc(memberId);

        // 3. Calculate Opening Balance (Replay history)
        BigDecimal openingBalance = BigDecimal.ZERO;
        List<Transaction> chronological = allTransactions.stream()
                .sorted(Comparator.comparing(Transaction::getTransactionDate))
                .collect(Collectors.toList());

        for (Transaction tx : chronological) {
            if (tx.getTransactionDate().toLocalDate().isBefore(startDate)) {
                openingBalance = applyTransactionToBalance(openingBalance, tx);
            }
        }

        // 4. Filter for Period
        List<Transaction> periodTransactions = chronological.stream()
                .filter(tx -> {
                    LocalDate txDate = tx.getTransactionDate().toLocalDate();
                    return !txDate.isBefore(startDate) && !txDate.isAfter(endDate);
                })
                .collect(Collectors.toList());

        // 5. Generate Statement Lines
        BigDecimal currentBalance = openingBalance;
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        List<MemberStatementDTO.StatementTransaction> dtos = new ArrayList<>();

        for (Transaction tx : periodTransactions) {
            BigDecimal amount = tx.getAmount();

            // Visual Totals
            if (isDebit(tx)) {
                totalDebits = totalDebits.add(amount.abs());
            } else {
                totalCredits = totalCredits.add(amount);
            }

            // Running Balance
            currentBalance = applyTransactionToBalance(currentBalance, tx);

            dtos.add(MemberStatementDTO.StatementTransaction.builder()
                    .date(tx.getTransactionDate().toLocalDate())
                    .reference(tx.getReferenceCode()) // System Ref
                    .externalReference(tx.getExternalReference()) // User Ref
                    .description(tx.getDescription())
                    .type(tx.getType().toString())
                    .amount(isDebit(tx) ? amount.negate() : amount) // Sign correctly
                    .runningBalance(currentBalance)
                    .build());
        }

        return MemberStatementDTO.builder()
                .organizationName(orgName)
                .organizationAddress(orgAddress)
                .organizationEmail(orgEmail)
                .organizationLogoUrl(orgLogo)
                .memberName(member.getFirstName() + " " + member.getLastName())
                .memberNumber(member.getMemberNumber())
                .memberAddress(member.getEmail())
                .statementReference("STMT-" + System.currentTimeMillis() % 1000000)
                .generatedDate(LocalDate.now())
                .openingBalance(openingBalance)
                .totalDebits(totalDebits)
                .totalCredits(totalCredits)
                .closingBalance(currentBalance)
                .transactions(dtos)
                .build();
    }

    /**
     * Calculates impact on Savings Balance.
     * External payments (M-Pesa/Bank) do NOT affect the Savings Balance.
     */
    private BigDecimal applyTransactionToBalance(BigDecimal currentBalance, Transaction tx) {
        BigDecimal amount = tx.getAmount();

        if (isExternalPayment(tx)) {
            return currentBalance; // Balance unchanged
        }

        if (isDebit(tx)) {
            return currentBalance.subtract(amount);
        } else {
            return currentBalance.add(amount);
        }
    }

    /**
     * Determines if transaction is Money Out (Debit) for the MEMBER.
     */
    private boolean isDebit(Transaction tx) {
        switch (tx.getType()) {
            case WITHDRAWAL:
            case TRANSFER:
            case PROCESSING_FEE:
            case REGISTRATION_FEE:
            case LATE_PAYMENT_PENALTY:
            case FINE_PAYMENT:
            case LOAN_REPAYMENT: // ✅ ADDED: Repayment is money leaving the member
                return true;
            default:
                return false; // Deposits, Loan Disbursements, Interest Earned = Credit
        }
    }

    /**
     * Checks if money moved via external channel (M-Pesa/Bank/Cash)
     * instead of the internal Savings Account.
     */
    private boolean isExternalPayment(Transaction tx) {
        // These types can be paid externally
        boolean canBeExternal =
                tx.getType() == Transaction.TransactionType.PROCESSING_FEE ||
                        tx.getType() == Transaction.TransactionType.REGISTRATION_FEE ||
                        tx.getType() == Transaction.TransactionType.FINE_PAYMENT ||
                        tx.getType() == Transaction.TransactionType.LOAN_REPAYMENT || // ✅ ADDED
                        tx.getType() == Transaction.TransactionType.LOAN_DISBURSEMENT; // ✅ ADDED

        // If payment method is NOT System/Null, it's external
        boolean isExternalMethod = tx.getPaymentMethod() != Transaction.PaymentMethod.SYSTEM &&
                tx.getPaymentMethod() != null;

        return canBeExternal && isExternalMethod;
    }

    @Transactional(readOnly = true)
    public List<LoanAgingDTO> getLoanAgingReport() {
        return new ArrayList<>();
    }
}