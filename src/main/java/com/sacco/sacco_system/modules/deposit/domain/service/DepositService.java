package com.sacco.sacco_system.modules.deposit.domain.service;

import com.sacco.sacco_system.modules.deposit.api.dto.*;
import com.sacco.sacco_system.modules.deposit.domain.entity.*;
import com.sacco.sacco_system.modules.deposit.domain.repository.DepositAllocationRepository;
import com.sacco.sacco_system.modules.deposit.domain.repository.DepositProductRepository;
import com.sacco.sacco_system.modules.deposit.domain.repository.DepositRepository;
import com.sacco.sacco_system.modules.finance.domain.entity.Fine;
import com.sacco.sacco_system.modules.finance.domain.entity.ShareCapital;
import com.sacco.sacco_system.modules.finance.domain.entity.Transaction;
import com.sacco.sacco_system.modules.finance.domain.repository.FineRepository;
import com.sacco.sacco_system.modules.finance.domain.repository.ShareCapitalRepository;
import com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository;
import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
import com.sacco.sacco_system.modules.finance.domain.service.ReferenceCodeService;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.loan.domain.service.LoanRepaymentService;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import com.sacco.sacco_system.modules.savings.domain.entity.SavingsAccount;
import com.sacco.sacco_system.modules.savings.domain.repository.SavingsAccountRepository;
import com.sacco.sacco_system.modules.savings.domain.service.SavingsService;
import com.sacco.sacco_system.modules.admin.domain.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Deposit Service
 * Handles deposit processing with routing to multiple destinations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DepositService {

    private final DepositRepository depositRepository;
    private final DepositAllocationRepository allocationRepository;
    private final DepositProductRepository depositProductRepository;
    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final LoanRepository loanRepository;
    private final FineRepository fineRepository;
    private final ShareCapitalRepository shareCapitalRepository;
    private final TransactionRepository transactionRepository;
    private final AccountingService accountingService;
    private final ReferenceCodeService referenceCodeService;
    private final SavingsService savingsService;
    private final LoanRepaymentService loanRepaymentService;
    private final SystemSettingService systemSettingService;

    /**
     * Process a deposit with multiple allocations
     */
    @Transactional
    public DepositDTO processDeposit(Member member, CreateDepositRequest request) {
        // 1. Validate allocations sum equals total
        validateAllocations(request);

        // ✅ 2. Determine the Source GL Account ONCE based on Payment Method and optional Bank Code
        String sourceAccount = getDebitAccountForPayment(request.getPaymentMethod(), request.getBankAccountCode());

        // 3. Create deposit entity (Initial State)
        Deposit deposit = Deposit.builder()
                .member(member)
                .totalAmount(request.getTotalAmount())
                .status(DepositStatus.PROCESSING)
                .paymentMethod(request.getPaymentMethod())
                .paymentReference(request.getPaymentReference())
                .notes(request.getNotes())
                .allocations(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();

        // 4. Process each allocation (FAIL FAST STRATEGY)
        List<DepositAllocation> allocations = new ArrayList<>();

        for (AllocationRequest allocationReq : request.getAllocations()) {
            // ✅ Pass the determined sourceAccount to the processor
            DepositAllocation allocation = processAllocation(member, deposit, allocationReq, sourceAccount);
            allocations.add(allocation);
        }

        deposit.setAllocations(allocations);
        deposit.setStatus(DepositStatus.COMPLETED);
        deposit.setProcessedAt(LocalDateTime.now());

        // 5. Save deposit (Cascades allocations)
        Deposit savedDeposit = depositRepository.save(deposit);

        return convertToDTO(savedDeposit);
    }

    /**
     * Process a single allocation
     */
    private DepositAllocation processAllocation(Member member, Deposit deposit, AllocationRequest request, String sourceAccount) {
        DepositAllocation allocation = DepositAllocation.builder()
                .deposit(deposit)
                .destinationType(request.getDestinationType())
                .amount(request.getAmount())
                .notes(request.getNotes())
                .status(AllocationStatus.PENDING)
                .build();

        switch (request.getDestinationType()) {
            case SAVINGS_ACCOUNT:
                processSavingsAllocation(member, allocation, request, sourceAccount);
                break;
            case LOAN_REPAYMENT:
                processLoanAllocation(member, allocation, request, sourceAccount);
                break;
            case FINE_PAYMENT:
                processFineAllocation(member, allocation, request, sourceAccount);
                break;
            case CONTRIBUTION_PRODUCT:
                processContributionAllocation(member, allocation, request, sourceAccount);
                break;
            case SHARE_CAPITAL:
                processShareCapitalAllocation(member, allocation, request, sourceAccount);
                break;
            default:
                throw new IllegalArgumentException("Unknown destination type: " + request.getDestinationType());
        }

        return allocation;
    }

    /**
     * Route money to savings account
     */
    private void processSavingsAllocation(Member member, DepositAllocation allocation, AllocationRequest request, String sourceAccount) {
        if (request.getSavingsAccountId() == null) {
            throw new IllegalArgumentException("Savings account ID is required");
        }

        SavingsAccount account = savingsAccountRepository.findById(request.getSavingsAccountId())
                .orElseThrow(() -> new RuntimeException("Savings account not found"));

        if (!account.getMember().getId().equals(member.getId())) {
            throw new SecurityException("Cannot deposit to another member's account");
        }

        if (account.getStatus() != SavingsAccount.AccountStatus.ACTIVE) {
            throw new IllegalStateException("Savings account is not active");
        }

        // Deposit to savings account using the determined Source Account
        savingsService.deposit(
            account.getAccountNumber(), 
            allocation.getAmount(), 
            "Multi-deposit allocation", 
            sourceAccount
        );

        allocation.setSavingsAccount(account);
        allocation.setStatus(AllocationStatus.COMPLETED);
        
        log.info("Routed {} to savings account {} via {}", allocation.getAmount(), account.getAccountNumber(), sourceAccount);
    }

    /**
     * Route money to loan repayment
     */
    private void processLoanAllocation(Member member, DepositAllocation allocation, AllocationRequest request, String sourceAccount) {
        if (request.getLoanId() == null) {
            throw new IllegalArgumentException("Loan ID is required");
        }

        Loan loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (!loan.getMember().getId().equals(member.getId())) {
            throw new SecurityException("Cannot pay another member's loan");
        }

        if (loan.getStatus() != Loan.LoanStatus.ACTIVE) {
            throw new IllegalStateException("Loan is not active");
        }

        // Make loan repayment using the determined Source Account
        loanRepaymentService.processPayment(
            loan, 
            allocation.getAmount(), 
            sourceAccount
        );

        allocation.setLoan(loan);
        allocation.setStatus(AllocationStatus.COMPLETED);
        
        log.info("Routed {} to loan repayment for loan {} via {}", allocation.getAmount(), loan.getLoanNumber(), sourceAccount);
    }

    /**
     * Route money to fine payment
     */
    private void processFineAllocation(Member member, DepositAllocation allocation, AllocationRequest request, String sourceAccount) {
        if (request.getFineId() == null) {
            throw new IllegalArgumentException("Fine ID is required");
        }

        Fine fine = fineRepository.findById(request.getFineId())
                .orElseThrow(() -> new RuntimeException("Fine not found"));

        if (!fine.getMember().getId().equals(member.getId())) {
            throw new SecurityException("Cannot pay another member's fine");
        }

        if (fine.getStatus() == Fine.FineStatus.PAID) {
            throw new IllegalStateException("Fine is already paid");
        }

        // Pay fine
        fine.setStatus(Fine.FineStatus.PAID);
        fine.setPaymentDate(LocalDate.now());
        fine.setPaymentReference(allocation.getDeposit().getTransactionReference());
        fineRepository.save(fine);

        // Create accounting entry using the determined Source Account
        accountingService.postEvent(
                "FINE_PAYMENT",
                "Fine payment: " + fine.getDescription(),
                allocation.getDeposit().getTransactionReference(),
                allocation.getAmount(),
                sourceAccount 
        );

        // Create transaction record
        Transaction transaction = Transaction.builder()
                .member(member)
                .type(Transaction.TransactionType.FINE_PAYMENT)
                .amount(allocation.getAmount())
                .paymentMethod(convertPaymentMethod(allocation.getDeposit().getPaymentMethod()))
                .referenceCode(referenceCodeService.generateReferenceCode())
                .description("Fine payment: " + fine.getDescription())
                .balanceAfter(BigDecimal.ZERO)
                .build();
        transactionRepository.save(transaction);

        allocation.setFine(fine);
        allocation.setStatus(AllocationStatus.COMPLETED);
        
        log.info("Routed {} to fine payment via {}", allocation.getAmount(), sourceAccount);
    }

    /**
     * Route money to contribution product
     */
    private void processContributionAllocation(Member member, DepositAllocation allocation, AllocationRequest request, String sourceAccount) {
        if (request.getDepositProductId() == null) {
            throw new IllegalArgumentException("Deposit product ID is required");
        }

        DepositProduct product = depositProductRepository.findById(request.getDepositProductId())
                .orElseThrow(() -> new RuntimeException("Deposit product not found"));

        if (product.getStatus() != DepositProductStatus.ACTIVE) {
            throw new IllegalStateException("Product is not accepting contributions");
        }

        // Add to product's current amount
        product.setCurrentAmount(product.getCurrentAmount().add(allocation.getAmount()));
        
        // Check if target reached
        if (product.getTargetAmount() != null && 
            product.getCurrentAmount().compareTo(product.getTargetAmount()) >= 0) {
            product.setStatus(DepositProductStatus.COMPLETED);
        }
        
        depositProductRepository.save(product);

        // Create accounting entry using the determined Source Account
        accountingService.postEvent(
                "CONTRIBUTION_RECEIVED",
                "Contribution to: " + product.getName(),
                allocation.getDeposit().getTransactionReference(),
                allocation.getAmount(),
                sourceAccount
        );

        // Create transaction record
        Transaction transaction = Transaction.builder()
                .member(member)
                .type(Transaction.TransactionType.DEPOSIT)
                .amount(allocation.getAmount())
                .paymentMethod(convertPaymentMethod(allocation.getDeposit().getPaymentMethod()))
                .referenceCode(referenceCodeService.generateReferenceCode())
                .description("Contribution to " + product.getName())
                .balanceAfter(product.getCurrentAmount())
                .build();
        transactionRepository.save(transaction);

        allocation.setDepositProduct(product);
        allocation.setStatus(AllocationStatus.COMPLETED);
        
        log.info("Routed {} to contribution product: {} via {}", allocation.getAmount(), product.getName(), sourceAccount);
    }

    /**
     * Route money to share capital
     */
    private void processShareCapitalAllocation(Member member, DepositAllocation allocation, AllocationRequest request, String sourceAccount) {
        // Get configured share value from system settings
        BigDecimal shareValue = BigDecimal.valueOf(systemSettingService.getDouble("SHARE_VALUE", 100.0));
        
        // Find or create share capital record
        ShareCapital shareCapital = shareCapitalRepository.findByMemberId(member.getId())
                .orElse(ShareCapital.builder()
                        .member(member)
                        .shareValue(shareValue)
                        .totalShares(BigDecimal.ZERO)
                        .paidShares(BigDecimal.ZERO)
                        .paidAmount(BigDecimal.ZERO)
                        .build());

        // Update share value if it has changed in settings
        shareCapital.setShareValue(shareValue);

        // Add to share capital amount
        shareCapital.setPaidAmount(shareCapital.getPaidAmount().add(allocation.getAmount()));
        
        // Calculate total shares owned = paidAmount / shareValue
        BigDecimal sharesOwned = shareCapital.getPaidAmount().divide(shareValue, 2, java.math.RoundingMode.DOWN);
        shareCapital.setPaidShares(sharesOwned);
        shareCapital.setTotalShares(sharesOwned); 
        
        shareCapitalRepository.save(shareCapital);

        // Update member total shares
        member.setTotalShares(shareCapital.getPaidAmount());
        memberRepository.save(member);

        // Create accounting entry using the determined Source Account
        accountingService.postEvent(
                "SHARE_CAPITAL_PURCHASE", 
                "Share capital contribution",
                allocation.getDeposit().getTransactionReference(),
                allocation.getAmount(),
                sourceAccount
        );

        // Create transaction record
        Transaction transaction = Transaction.builder()
                .member(member)
                .type(Transaction.TransactionType.SHARE_PURCHASE)
                .amount(allocation.getAmount())
                .paymentMethod(convertPaymentMethod(allocation.getDeposit().getPaymentMethod()))
                .referenceCode(referenceCodeService.generateReferenceCode())
                .description("Share capital contribution - " + sharesOwned + " shares owned (@ KES " + shareValue + " per share)")
                .balanceAfter(shareCapital.getPaidAmount())
                .build();
        transactionRepository.save(transaction);

        allocation.setStatus(AllocationStatus.COMPLETED);
        
        log.info("Routed {} to share capital via {}", allocation.getAmount(), sourceAccount);
    }

    /**
     * Convert deposit payment method to transaction payment method
     */
    private Transaction.PaymentMethod convertPaymentMethod(String depositPaymentMethod) {
        if (depositPaymentMethod == null) {
            return Transaction.PaymentMethod.CASH;
        }
        try {
            return Transaction.PaymentMethod.valueOf(depositPaymentMethod.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Transaction.PaymentMethod.CASH;
        }
    }

    /**
     * Validate allocations sum equals total amount
     */
    private void validateAllocations(CreateDepositRequest request) {
        BigDecimal allocationsTotal = request.getAllocations().stream()
                .map(AllocationRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (allocationsTotal.compareTo(request.getTotalAmount()) != 0) {
            throw new IllegalArgumentException(
                    String.format("Allocations total (%.2f) does not match deposit amount (%.2f)", 
                            allocationsTotal, request.getTotalAmount())
            );
        }

        request.getAllocations().forEach(allocation -> {
            if (allocation.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("All allocation amounts must be greater than zero");
            }
        });
    }

    public List<DepositDTO> getMemberDeposits(Member member) {
        return depositRepository.findByMemberOrderByCreatedAtDesc(member).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public DepositDTO getDepositById(UUID id) {
        Deposit deposit = depositRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Deposit not found"));
        return convertToDTO(deposit);
    }

    private DepositDTO convertToDTO(Deposit deposit) {
        return DepositDTO.builder()
                .id(deposit.getId())
                .memberId(deposit.getMember().getId())
                .memberName(deposit.getMember().getFirstName() + " " + deposit.getMember().getLastName())
                .totalAmount(deposit.getTotalAmount())
                .status(deposit.getStatus())
                .transactionReference(deposit.getTransactionReference())
                .paymentMethod(deposit.getPaymentMethod())
                .paymentReference(deposit.getPaymentReference())
                .allocations(deposit.getAllocations().stream()
                        .map(this::convertAllocationToDTO)
                        .collect(Collectors.toList()))
                .notes(deposit.getNotes())
                .createdAt(deposit.getCreatedAt())
                .processedAt(deposit.getProcessedAt())
                .build();
    }

    private AllocationDTO convertAllocationToDTO(DepositAllocation allocation) {
        String destinationName = getDestinationName(allocation);
        
        return AllocationDTO.builder()
                .id(allocation.getId())
                .destinationType(allocation.getDestinationType())
                .amount(allocation.getAmount())
                .status(allocation.getStatus())
                .destinationName(destinationName)
                .notes(allocation.getNotes())
                .errorMessage(allocation.getErrorMessage())
                .build();
    }

    private String getDestinationName(DepositAllocation allocation) {
        switch (allocation.getDestinationType()) {
            case SAVINGS_ACCOUNT:
                return allocation.getSavingsAccount() != null 
                        ? allocation.getSavingsAccount().getAccountNumber() 
                        : "Savings Account";
            case LOAN_REPAYMENT:
                return allocation.getLoan() != null 
                        ? "Loan " + allocation.getLoan().getLoanNumber() 
                        : "Loan Repayment";
            case FINE_PAYMENT:
                return allocation.getFine() != null 
                        ? allocation.getFine().getDescription() 
                        : "Fine Payment";
            case CONTRIBUTION_PRODUCT:
                return allocation.getDepositProduct() != null 
                        ? allocation.getDepositProduct().getName() 
                        : "Contribution";
            case SHARE_CAPITAL:
                return "Share Capital";
            default:
                return allocation.getDestinationType().toString();
        }
    }

    /**
     * ✅ Helper method to get the correct Debit Account based on Payment Method
     * UPDATED: Accepts optional bankAccountCode to route to specific banks
     */
    private String getDebitAccountForPayment(String paymentMethod, String bankAccountCode) {
        if (paymentMethod == null) return "1001"; // Default to Cash
        
        switch (paymentMethod.toUpperCase()) {
            case "MPESA": return "1002"; // M-Pesa Paybill
            case "BANK": 
                // ✅ If user provided a specific bank code, USE IT.
                if (bankAccountCode != null && !bankAccountCode.isEmpty()) {
                    return bankAccountCode;
                }
                return "1010"; // Fallback to default Bank (Equity)
            case "CASH": return "1001";  // Cash on Hand
            default: return "1001";
        }
    }
}