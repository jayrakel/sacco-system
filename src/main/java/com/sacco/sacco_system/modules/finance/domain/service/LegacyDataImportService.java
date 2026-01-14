package com.sacco.sacco_system.modules.finance.domain.service;

import com.sacco.sacco_system.modules.finance.domain.entity.Transaction;
import com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanProduct;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanProductRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.loan.domain.service.LoanAmortizationService;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.entity.MemberStatus;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import com.sacco.sacco_system.modules.savings.domain.entity.SavingsAccount;
import com.sacco.sacco_system.modules.savings.domain.repository.SavingsAccountRepository;
import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.users.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LegacyDataImportService {

    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final LoanRepository loanRepository;
    private final LoanProductRepository loanProductRepository;
    private final LoanAmortizationService loanAmortizationService;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public String importHistory(List<MultipartFile> files) {
        // 1. Sort files chronologically based on filename date
        List<ImportFile> sortedFiles = files.stream()
                .map(file -> new ImportFile(file, extractDate(file.getOriginalName())))
                .filter(f -> f.date != null)
                .sorted(Comparator.comparing(f -> f.date))
                .collect(Collectors.toList());

        if (sortedFiles.isEmpty()) return "No valid files found to import.";

        int totalRecords = 0;

        // Ensure default loan product
        LoanProduct defaultProduct = loanProductRepository.findAll().stream().findFirst()
                .orElseGet(this::createDefaultProduct);

        // 2. Process each file in order
        for (ImportFile importFile : sortedFiles) {
            log.info("Processing file for date: {}", importFile.date);
            totalRecords += processSingleFile(importFile.file, importFile.date, defaultProduct);
        }

        return "Successfully imported history from " + sortedFiles.size() + " files. Total records processed: " + totalRecords;
    }

    private int processSingleFile(MultipartFile file, LocalDate reportDate, LoanProduct defaultProduct) {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber < 5) continue; // Skip headers

                // Handle CSV parsing considering quoted strings (e.g. "Name Surname")
                String[] columns = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                if (columns.length < 14) continue;

                String rawName = columns[1].replace("\"", "").trim();
                if (rawName.isEmpty() || rawName.equalsIgnoreCase("NAMES") || rawName.equalsIgnoreCase("TOTALS")) continue;

                try {
                    // --- 1. MEMBER & SAVINGS ---
                    Member member = findOrCreateMember(rawName);

                    // Col 8: Weekly Savings Paid (Transaction)
                    String savingsPaidStr = columns[8].replace("\"", "").trim();
                    // Col 10: Savings Balance (State)
                    String savingsBalStr = columns[10].replace("\"", "").trim();

                    if (!savingsPaidStr.isEmpty()) {
                        BigDecimal amount = parseDecimal(savingsPaidStr);
                        if (amount.compareTo(BigDecimal.ZERO) > 0) {
                            recordTransaction(member, null, amount, Transaction.TransactionType.DEPOSIT, reportDate, "Weekly Savings");
                        }
                    }

                    // Update running balance to match snapshot
                    if (!savingsBalStr.isEmpty()) {
                        updateSavingsBalance(member, parseDecimal(savingsBalStr));
                    }

                    // --- 2. LOANS ---
                    // Col 2: Face Value (Principal)
                    // Col 11: Loan Amount Paid (Transaction)
                    // Col 13: Loan Amount Balance (State)
                    String faceValueStr = columns[2].replace("\"", "").trim();
                    String loanPaidStr = columns[11].replace("\"", "").trim();
                    String loanBalStr = columns[13].replace("\"", "").trim();

                    BigDecimal faceValue = parseDecimal(faceValueStr);
                    BigDecimal loanRepaid = parseDecimal(loanPaidStr);
                    BigDecimal loanBalance = parseDecimal(loanBalStr);

                    // Handle Loan Lifecycle
                    if (faceValue.compareTo(BigDecimal.ZERO) > 0) {
                        Loan loan = findOrCreateActiveLoan(member, defaultProduct, faceValue, reportDate);

                        // Record Repayment Transaction
                        if (loanRepaid.compareTo(BigDecimal.ZERO) > 0) {
                            recordTransaction(member, loan, loanRepaid, Transaction.TransactionType.LOAN_REPAYMENT, reportDate, "Weekly Repayment");
                        }

                        // Update Loan State
                        loan.setTotalOutstandingAmount(loanBalance);

                        // Close loan if balance is 0
                        if (loanBalance.compareTo(BigDecimal.ZERO) <= 0) {
                            loan.setLoanStatus(Loan.LoanStatus.CLOSED);
                            loan.setActive(false);
                        } else {
                            loan.setLoanStatus(Loan.LoanStatus.ACTIVE); // Ensure it's active
                            loan.setActive(true);
                        }
                        loanRepository.save(loan);
                    }

                    count++;
                } catch (Exception e) {
                    log.error("Error on line {} in {}: {}", lineNumber, file.getOriginalName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to read file", e);
        }
        return count;
    }

    private Member findOrCreateMember(String fullName) {
        // Simple normalization: remove newlines in names (e.g., "BENJAMIN \nMUKETHA")
        String cleanName = fullName.replace("\n", " ").replaceAll("\\s+", " ").trim();
        String[] parts = cleanName.split(" ");
        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[parts.length - 1] : "Member";
        String email = (firstName + "." + lastName + "@sacco.local").toLowerCase();

        return memberRepository.findByEmail(email).orElseGet(() -> {
            Member newMember = Member.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .phoneNumber("0700000000")
                    .memberNumber("M" + System.nanoTime() % 100000) // Temp ID
                    .memberStatus(MemberStatus.ACTIVE)
                    .registrationFeePaid(true)
                    .build();
            Member saved = memberRepository.save(newMember);

            // Create Login User
            if(userRepository.findByEmail(email).isEmpty()) {
                User user = User.builder()
                        .email(email)
                        .firstName(firstName)
                        .lastName(lastName)
                        .password(passwordEncoder.encode("123456"))
                        .role(User.Role.MEMBER)
                        .member(saved)
                        .enabled(true)
                        .build();
                userRepository.save(user);
            }
            return saved;
        });
    }

    private Loan findOrCreateActiveLoan(Member member, LoanProduct product, BigDecimal principal, LocalDate reportDate) {
        // Look for an existing loan with roughly the same principal (Face Value)
        // If Face Value changes in the CSV, it usually implies a new loan or top-up.
        List<Loan> loans = loanRepository.findByMemberId(member.getId());

        return loans.stream()
                .filter(l -> l.getPrincipalAmount().compareTo(principal) == 0 &&
                        (l.getLoanStatus() == Loan.LoanStatus.ACTIVE || l.getLoanStatus() == Loan.LoanStatus.DISBURSED))
                .findFirst()
                .orElseGet(() -> {
                    // If no matching active loan, perform "Disbursement"
                    Loan newLoan = new Loan();
                    newLoan.setMember(member);
                    newLoan.setProduct(product);
                    newLoan.setLoanNumber("LN-" + System.nanoTime() % 1000000);
                    newLoan.setPrincipalAmount(principal);
                    newLoan.setDisbursedAmount(principal);
                    newLoan.setApplicationDate(reportDate);
                    newLoan.setDisbursementDate(reportDate);
                    newLoan.setInterestRate(product.getInterestRate());
                    newLoan.setDurationWeeks(52);
                    newLoan.setLoanStatus(Loan.LoanStatus.DISBURSED); // Start as disbursed
                    newLoan.setActive(true);

                    Loan saved = loanRepository.save(newLoan);
                    loanAmortizationService.generateSchedule(saved); // Generate schedule

                    // Log Disbursement Transaction
                    recordTransaction(member, saved, principal, Transaction.TransactionType.LOAN_DISBURSEMENT, reportDate, "Historical Disbursement");

                    return saved;
                });
    }

    private void recordTransaction(Member member, Loan loan, BigDecimal amount, Transaction.TransactionType type, LocalDate date, String desc) {
        Transaction txn = new Transaction();
        txn.setTransactionId("HIST-" + UUID.randomUUID().toString().substring(0, 8));
        txn.setMember(member); // Assuming Transaction entity has member link (if not, link via Loan)
        txn.setLoan(loan);
        txn.setType(type);
        txn.setAmount(amount);
        txn.setTransactionDate(date.atStartOfDay());
        txn.setDescription(desc);
        txn.setPaymentMethod(Transaction.PaymentMethod.CASH); // Assume cash for legacy
        transactionRepository.save(txn);
    }

    private void updateSavingsBalance(Member member, BigDecimal balance) {
        SavingsAccount account = savingsAccountRepository.findByMemberId(member.getId())
                .stream().findFirst().orElseGet(() -> {
                    SavingsAccount newAcc = new SavingsAccount();
                    newAcc.setMember(member);
                    newAcc.setAccountNumber("SAV-" + member.getMemberNumber());
                    newAcc.setBalance(BigDecimal.ZERO);
                    return savingsAccountRepository.save(newAcc);
                });
        account.setBalance(balance);
        savingsAccountRepository.save(account);
    }

    private BigDecimal parseDecimal(String val) {
        if (val == null || val.trim().isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(val.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private LocalDate extractDate(String filename) {
        try {
            // Regex to find dates like "01 JAN 2026", "18 SEPT 25", "01 SEP 22"
            Pattern pattern = Pattern.compile("(\\d{1,2})\\s+([A-Z]+)\\s+(\\d{2,4})", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(filename);

            if (matcher.find()) {
                String day = matcher.group(1);
                String month = matcher.group(2).substring(0, 3); // Take first 3 chars (SEPT -> SEP)
                String year = matcher.group(3);

                // Normalize Year (22 -> 2022)
                if (year.length() == 2) year = "20" + year;

                String dateStr = day + " " + month + " " + year;
                DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendPattern("d MMM yyyy")
                        .toFormatter(Locale.ENGLISH);

                return LocalDate.parse(dateStr, formatter);
            }
        } catch (Exception e) {
            log.warn("Could not parse date from filename: " + filename);
        }
        return null;
    }

    private LoanProduct createDefaultProduct() {
        LoanProduct product = new LoanProduct();
        product.setProductName("Standard Loan");
        product.setProductCode("STD");
        product.setInterestRate(new BigDecimal("10"));
        product.setMaxAmount(new BigDecimal("1000000"));
        product.setMaxDurationWeeks(104);
        return loanProductRepository.save(product);
    }

    // Helper class for sorting
    private static class ImportFile {
        MultipartFile file;
        LocalDate date;

        public ImportFile(MultipartFile file, LocalDate date) {
            this.file = file;
            this.date = date;
        }
    }
}