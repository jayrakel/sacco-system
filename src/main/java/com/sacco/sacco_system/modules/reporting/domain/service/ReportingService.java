package com.sacco.sacco_system.modules.reporting.domain.service;

import com.sacco.sacco_system.modules.admin.domain.entity.SystemSetting;
import com.sacco.sacco_system.modules.admin.domain.repository.SystemSettingRepository;
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
import java.time.temporal.ChronoUnit;
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
    private final SystemSettingRepository systemSettingRepository; // Inject Settings

    @Transactional(readOnly = true)
    public MemberStatementDTO getMemberStatement(UUID memberId, LocalDate startDate, LocalDate endDate) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // 1. Fetch Organization Details (Dynamic Logo & Info)
        // Assuming keys: 'organization_name', 'organization_logo', etc. Adjust keys to match your DB.
        List<SystemSetting> settings = systemSettingRepository.findAll();
        Map<String, String> config = settings.stream()
                .collect(Collectors.toMap(SystemSetting::getKey, SystemSetting::getValue, (a, b) -> b));

        String orgName = config.getOrDefault("ORGANIZATION_NAME", "Sacco System");
        String orgAddress = config.getOrDefault("ORGANIZATION_ADDRESS", "P.O. Box 0000");
        String orgEmail = config.getOrDefault("ORGANIZATION_EMAIL", "info@sacco.com");
        String orgLogo = config.getOrDefault("ORGANIZATION_LOGO", ""); // URL or Base64

        // 2. Fetch Transactions
        List<Transaction> allTransactions = transactionRepository.findByMemberIdOrderByTransactionDateDesc(memberId);

        // 3. Calculate Opening Balance
        // Get the balance AFTER the last transaction that occurred BEFORE the start date.
        BigDecimal openingBalance = allTransactions.stream()
                .filter(tx -> tx.getTransactionDate().toLocalDate().isBefore(startDate))
                .max(Comparator.comparing(Transaction::getTransactionDate))
                .map(Transaction::getBalanceAfter)
                .orElse(BigDecimal.ZERO);

        // 4. Filter for Period
        List<Transaction> periodTransactions = allTransactions.stream()
                .filter(tx -> {
                    LocalDate txDate = tx.getTransactionDate().toLocalDate();
                    return !txDate.isBefore(startDate) && !txDate.isAfter(endDate);
                })
                .sorted(Comparator.comparing(Transaction::getTransactionDate)) // Must be Ascending for running balance
                .collect(Collectors.toList());

        // 5. Calculate Running Balance Iteratively (Fixes the "Wrong Calculation" issue)
        BigDecimal currentBalance = openingBalance;
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        List<MemberStatementDTO.StatementTransaction> dtos = new ArrayList<>();

        for (Transaction tx : periodTransactions) {
            BigDecimal amount = tx.getAmount();
            
            // Logic: Negative = Debit, Positive = Credit
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                totalDebits = totalDebits.add(amount.abs());
            } else {
                totalCredits = totalCredits.add(amount);
            }

            // Update Running Balance
            // New Balance = Old Balance + Amount (since amount is signed)
            currentBalance = currentBalance.add(amount);

            dtos.add(MemberStatementDTO.StatementTransaction.builder()
                    .date(tx.getTransactionDate().toLocalDate())
                    .reference(tx.getTransactionId())
                    .description(tx.getDescription())
                    .type(tx.getType().toString())
                    .amount(amount)
                    .runningBalance(currentBalance) // Use calculated balance, not DB snapshot
                    .build());
        }

        return MemberStatementDTO.builder()
                .organizationName(orgName)
                .organizationAddress(orgAddress)
                .organizationEmail(orgEmail)
                .organizationLogoUrl(orgLogo) // Pass to frontend
                .memberName(member.getFirstName() + " " + member.getLastName())
                .memberNumber(member.getMemberNumber())
                .memberAddress(member.getEmail())
                .statementReference("STMT-" + System.currentTimeMillis() % 1000000)
                .generatedDate(LocalDate.now())
                .openingBalance(openingBalance)
                .totalDebits(totalDebits)
                .totalCredits(totalCredits)
                .closingBalance(currentBalance) // Ensure matches final running balance
                .transactions(dtos)
                .build();
    }


}