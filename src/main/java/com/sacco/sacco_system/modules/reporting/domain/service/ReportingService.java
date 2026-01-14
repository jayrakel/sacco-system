package com.sacco.sacco_system.modules.reporting.domain.service;

import com.sacco.sacco_system.modules.admin.domain.entity.SystemSetting;
import com.sacco.sacco_system.modules.admin.domain.repository.SystemSettingRepository;
import com.sacco.sacco_system.modules.reporting.api.dto.LoanAgingDTO;
import com.sacco.sacco_system.modules.reporting.api.dto.MemberStatementDTO;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.finance.domain.entity.Transaction;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository;
import com.sacco.sacco_system.modules.savings.domain.entity.SavingsAccount;
import com.sacco.sacco_system.modules.savings.domain.repository.SavingsAccountRepository;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
    private final SavingsAccountRepository savingsAccountRepository;
    private final LoanRepository loanRepository;

    // ✅ Section 31 Compliance: Read-Only Transaction (No Mutation)
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats() {
        // --- 1. Base Totals ---
        long totalMembers = memberRepository.count();

        BigDecimal totalSavings = savingsAccountRepository.findAll().stream()
                .map(SavingsAccount::getBalanceAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalLoansIssued = loanRepository.findAll().stream()
                .map(Loan::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // --- 2. Income & Trends Calculation ---
        List<Transaction> allTransactions = transactionRepository.findAll();
        LocalDate now = LocalDate.now();
        LocalDate startThisMonth = now.withDayOfMonth(1);
        LocalDate startLastMonth = now.minusMonths(1).withDayOfMonth(1);
        LocalDate endLastMonth = startThisMonth.minusDays(1);

        // A. Net Income (Total)
        BigDecimal totalIncome = calculateSum(allTransactions, t -> isIncomeType(t.getType()));
        BigDecimal totalExpenses = calculateSum(allTransactions, t -> t.getType().toString().equals("INTEREST_EARNED"));
        BigDecimal netIncome = totalIncome.subtract(totalExpenses);

        // B. Income Trend (This Month vs Last Month)
        BigDecimal incomeThisMonth = calculatePeriodSum(allTransactions, startThisMonth, now, t -> isIncomeType(t.getType()));
        BigDecimal incomeLastMonth = calculatePeriodSum(allTransactions, startLastMonth, endLastMonth, t -> isIncomeType(t.getType()));
        String incomeTrend = calculateTrendPercentage(incomeThisMonth, incomeLastMonth);

        // C. Savings Trend (Net Deposits This Month vs Last Month)
        BigDecimal savingsGrowthThisMonth = calculateNetSavingsChange(allTransactions, startThisMonth, now);
        BigDecimal savingsGrowthLastMonth = calculateNetSavingsChange(allTransactions, startLastMonth, endLastMonth);
        String savingsTrend = calculateTrendPercentage(savingsGrowthThisMonth, savingsGrowthLastMonth);

        // D. Pending Loans Count (Fixed: Uses getLoanStatus() and correct Enum values)
        long pendingLoans = loanRepository.findAll().stream()
                .filter(l -> {
                    String status = String.valueOf(l.getLoanStatus());
                    return "SUBMITTED".equalsIgnoreCase(status) || "UNDER_REVIEW".equalsIgnoreCase(status);
                })
                .count();

        // E. New Members (Last 7 Days)
        long newMembers = memberRepository.findAll().stream()
                .filter(m -> m.getCreatedAt() != null && m.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7)))
                .count();

        // --- 3. Construct Response ---
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMembers", totalMembers);
        stats.put("totalSavings", totalSavings);
        stats.put("totalLoansIssued", totalLoansIssued);
        stats.put("netIncome", netIncome);

        // Dynamic Trends
        stats.put("incomeTrend", incomeTrend + " vs last month");
        stats.put("savingsTrend", savingsTrend + " vs last month");
        stats.put("pendingLoansCount", pendingLoans + " Pending Approval");
        stats.put("newMembersCount", "+" + newMembers + " New this week");

        return stats;
    }

    // --- ✅ ADDED: System Diagnostics Method ---
    public Map<String, Object> getSystemDiagnostics() {
        Map<String, Object> diagnostics = new HashMap<>();

        // 1. Database Latency Check
        long start = System.currentTimeMillis();
        try {
            systemSettingRepository.count(); // Simple lightweight query
            long latency = System.currentTimeMillis() - start;
            diagnostics.put("dbStatus", "Connected");
            diagnostics.put("dbLatency", latency + "ms");
        } catch (Exception e) {
            diagnostics.put("dbStatus", "Disconnected");
            diagnostics.put("dbLatency", "N/A");
        }

        // 2. Memory Usage (JVM)
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        diagnostics.put("memoryUsed", formatBytes(usedMemory));
        diagnostics.put("memoryTotal", formatBytes(totalMemory));
        diagnostics.put("memoryUsagePercent", (totalMemory > 0) ? (usedMemory * 100) / totalMemory : 0);

        // 3. Disk Space
        File disk = new File(".");
        long totalSpace = disk.getTotalSpace();
        long freeSpace = disk.getFreeSpace();
        long usedSpace = totalSpace - freeSpace;

        diagnostics.put("diskUsed", formatBytes(usedSpace));
        diagnostics.put("diskTotal", formatBytes(totalSpace));
        diagnostics.put("diskUsagePercent", (totalSpace > 0) ? (usedSpace * 100) / totalSpace : 0);

        // 4. Services (Mocked for now, but wired for expansion)
        diagnostics.put("emailService", "Active");
        diagnostics.put("smsService", "Standby");

        diagnostics.put("timestamp", LocalDateTime.now().toString());

        return diagnostics;
    }

    private String formatBytes(long bytes) {
        long limit = 1024 * 1024;
        long limitGb = 1024 * 1024 * 1024;
        if (bytes > limitGb) {
            return String.format("%.2f GB", (double) bytes / limitGb);
        }
        return String.format("%d MB", bytes / limit);
    }

    // --- Helper Methods ---

    private BigDecimal calculateSum(List<Transaction> txs, java.util.function.Predicate<Transaction> filter) {
        return txs.stream().filter(filter).map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculatePeriodSum(List<Transaction> txs, LocalDate start, LocalDate end, java.util.function.Predicate<Transaction> filter) {
        return txs.stream()
                .filter(t -> !t.getTransactionDate().toLocalDate().isBefore(start) && !t.getTransactionDate().toLocalDate().isAfter(end))
                .filter(filter)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateNetSavingsChange(List<Transaction> txs, LocalDate start, LocalDate end) {
        return txs.stream()
                .filter(t -> !t.getTransactionDate().toLocalDate().isBefore(start) && !t.getTransactionDate().toLocalDate().isAfter(end))
                .map(t -> {
                    if (t.getType() == Transaction.TransactionType.DEPOSIT) return t.getAmount();
                    if (t.getType() == Transaction.TransactionType.WITHDRAWAL) return t.getAmount().negate();
                    return BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String calculateTrendPercentage(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? "+100%" : "0%";
        }
        BigDecimal diff = current.subtract(previous);
        // Calculate percentage: (diff / previous) * 100
        BigDecimal percent = diff.divide(previous, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        return (percent.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + percent + "%";
    }

    private boolean isIncomeType(Transaction.TransactionType type) {
        return type == Transaction.TransactionType.PROCESSING_FEE ||
                type == Transaction.TransactionType.REGISTRATION_FEE ||
                type == Transaction.TransactionType.LATE_PAYMENT_PENALTY ||
                type == Transaction.TransactionType.FINE_PAYMENT;
    }

    @Transactional(readOnly = true)
    public MemberStatementDTO getMemberStatement(UUID memberId, LocalDate startDate, LocalDate endDate) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        List<SystemSetting> settings = systemSettingRepository.findAll();
        Map<String, String> config = settings.stream()
                .collect(Collectors.toMap(SystemSetting::getKey, SystemSetting::getValue, (a, b) -> b));

        String orgName = config.getOrDefault("ORGANIZATION_NAME", "Sacco System");
        String orgAddress = config.getOrDefault("ORGANIZATION_ADDRESS", "P.O. Box 0000");
        String orgEmail = config.getOrDefault("ORGANIZATION_EMAIL", "info@sacco.com");
        String orgLogo = config.getOrDefault("ORGANIZATION_LOGO", "");

        List<Transaction> allTransactions = transactionRepository.findByMemberIdOrderByTransactionDateDesc(memberId);

        BigDecimal openingBalance = BigDecimal.ZERO;
        List<Transaction> chronological = allTransactions.stream()
                .sorted(Comparator.comparing(Transaction::getTransactionDate))
                .collect(Collectors.toList());

        for (Transaction tx : chronological) {
            if (tx.getTransactionDate().toLocalDate().isBefore(startDate)) {
                openingBalance = applyTransactionToBalance(openingBalance, tx);
            }
        }

        List<Transaction> periodTransactions = chronological.stream()
                .filter(tx -> {
                    LocalDate txDate = tx.getTransactionDate().toLocalDate();
                    return !txDate.isBefore(startDate) && !txDate.isAfter(endDate);
                })
                .collect(Collectors.toList());

        BigDecimal currentBalance = openingBalance;
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        List<MemberStatementDTO.StatementTransaction> dtos = new ArrayList<>();

        for (Transaction tx : periodTransactions) {
            BigDecimal amount = tx.getAmount();

            if (isDebit(tx)) {
                totalDebits = totalDebits.add(amount.abs());
            } else {
                totalCredits = totalCredits.add(amount);
            }

            currentBalance = applyTransactionToBalance(currentBalance, tx);

            dtos.add(MemberStatementDTO.StatementTransaction.builder()
                    .date(tx.getTransactionDate().toLocalDate())
                    .reference(tx.getReferenceCode())
                    .externalReference(tx.getExternalReference())
                    .description(tx.getDescription())
                    .type(tx.getType().toString())
                    .amount(isDebit(tx) ? amount.negate() : amount)
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

    private BigDecimal applyTransactionToBalance(BigDecimal currentBalance, Transaction tx) {
        BigDecimal amount = tx.getAmount();

        if (isExternalPayment(tx)) {
            return currentBalance;
        }

        if (isDebit(tx)) {
            return currentBalance.subtract(amount);
        } else {
            return currentBalance.add(amount);
        }
    }

    private boolean isDebit(Transaction tx) {
        switch (tx.getType()) {
            case WITHDRAWAL:
            case TRANSFER:
            case PROCESSING_FEE:
            case REGISTRATION_FEE:
            case LATE_PAYMENT_PENALTY:
            case FINE_PAYMENT:
            case LOAN_REPAYMENT:
                return true;
            default:
                return false;
        }
    }

    private boolean isExternalPayment(Transaction tx) {
        boolean canBeExternal =
                tx.getType() == Transaction.TransactionType.PROCESSING_FEE ||
                        tx.getType() == Transaction.TransactionType.REGISTRATION_FEE ||
                        tx.getType() == Transaction.TransactionType.FINE_PAYMENT ||
                        tx.getType() == Transaction.TransactionType.LOAN_REPAYMENT ||
                        tx.getType() == Transaction.TransactionType.LOAN_DISBURSEMENT;

        boolean isExternalMethod = tx.getPaymentMethod() != Transaction.PaymentMethod.SYSTEM &&
                tx.getPaymentMethod() != null;

        return canBeExternal && isExternalMethod;
    }

    @Transactional(readOnly = true)
    public List<LoanAgingDTO> getLoanAgingReport() {
        return new ArrayList<>();
    }
}