package com.sacco.sacco_system.service;

import com.sacco.sacco_system.dto.GuarantorDTO;
import com.sacco.sacco_system.dto.LoanDTO;
import com.sacco.sacco_system.entity.Guarantor;
import com.sacco.sacco_system.entity.Loan;
import com.sacco.sacco_system.entity.LoanRepayment;
import com.sacco.sacco_system.entity.Member;
import com.sacco.sacco_system.repository.LoanRepository;
import com.sacco.sacco_system.repository.LoanRepaymentRepository;
import com.sacco.sacco_system.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LoanService {
    
    private final LoanRepository loanRepository;
    private final LoanRepaymentRepository loanRepaymentRepository;
    private final MemberRepository memberRepository;
    
    public LoanDTO applyForLoan(UUID memberId, BigDecimal principalAmount,
                                BigDecimal interestRate, Integer durationMonths) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // --- NEW LOGIC: Enforce 3x Limit ---
        // A member can borrow max 3 times their (Savings + Shares)
        BigDecimal totalDeposits = member.getTotalSavings().add(member.getTotalShares());
        BigDecimal maxLoanLimit = totalDeposits.multiply(BigDecimal.valueOf(3));
        
        if (principalAmount.compareTo(maxLoanLimit) > 0) {
            throw new RuntimeException("Loan refused. Maximum limit is " + maxLoanLimit + 
                                     " (3x your savings of " + totalDeposits + ")");
        }        
        
        // Calculate total interest and monthly repayment
        BigDecimal totalInterest = principalAmount
                .multiply(interestRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(durationMonths))
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        
        BigDecimal totalAmount = principalAmount.add(totalInterest);
        BigDecimal monthlyRepayment = totalAmount
                .divide(BigDecimal.valueOf(durationMonths), 2, RoundingMode.HALF_UP);
        
        Loan loan = Loan.builder()
                .loanNumber(generateLoanNumber())
                .member(member)
                .principalAmount(principalAmount)
                .loanBalance(principalAmount)
                .interestRate(interestRate)
                .totalInterest(totalInterest)
                .durationMonths(durationMonths)
                .monthlyRepayment(monthlyRepayment)
                .status(Loan.LoanStatus.PENDING)
                .build();
        
        Loan savedLoan = loanRepository.save(loan);
        createRepaymentSchedule(savedLoan);
        
        return convertToDTO(savedLoan);
    }
    
    public LoanDTO getLoanById(UUID id) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
        return convertToDTO(loan);
    }
    
    public LoanDTO getLoanByNumber(String loanNumber) {
        Loan loan = loanRepository.findByLoanNumber(loanNumber)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
        return convertToDTO(loan);
    }
    
    public List<LoanDTO> getLoansByMemberId(UUID memberId) {
        return loanRepository.findByMemberId(memberId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<LoanDTO> getAllLoans() {
        return loanRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<LoanDTO> getLoansByStatus(Loan.LoanStatus status) {
        return loanRepository.findByStatus(status)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public LoanDTO approveLoan(UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
        
        loan.setStatus(Loan.LoanStatus.APPROVED);
        loan.setApprovalDate(LocalDate.now());
        
        Loan savedLoan = loanRepository.save(loan);
        return convertToDTO(savedLoan);
    }
    
    public LoanDTO disburseLoan(UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
        
        if (!loan.getStatus().equals(Loan.LoanStatus.APPROVED)) {
            throw new RuntimeException("Only approved loans can be disbursed");
        }
        
        loan.setStatus(Loan.LoanStatus.DISBURSED);
        loan.setDisbursementDate(LocalDate.now());
        loan.setExpectedRepaymentDate(LocalDate.now().plusMonths(loan.getDurationMonths()));
        
        Loan savedLoan = loanRepository.save(loan);
        return convertToDTO(savedLoan);
    }
    
    public LoanDTO rejectLoan(UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
        
        loan.setStatus(Loan.LoanStatus.REJECTED);
        Loan savedLoan = loanRepository.save(loan);
        return convertToDTO(savedLoan);
    }

    public GuarantorDTO addGuarantor(UUID loanId, UUID guarantorMemberId, BigDecimal amount) {
        // 1. Find the Loan
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        // 2. Find the Member who will be the guarantor
        Member guarantorMember = memberRepository.findById(guarantorMemberId)
                .orElseThrow(() -> new RuntimeException("Guarantor not found"));

        // 3. Validation: A member cannot guarantee their own loan
        if (loan.getMember().getId().equals(guarantorMember.getId())) {
            throw new RuntimeException("You cannot guarantee your own loan.");
        }

        // 4. Create and Save the Guarantor record
        Guarantor guarantor = Guarantor.builder()
                .loan(loan)
                .member(guarantorMember)
                .guaranteeAmount(amount)
                .build();
        
        // Note: We need a GuarantorRepository to save this directly, 
        // OR we can add it to the loan's list if we set up the relationship correctly.
        // For simplicity, let's assume we added a GuarantorRepository (Step 2.5 below).
        // For now, let's use the loan's list which cascades the save:
        loan.getGuarantors().add(guarantor);
        loanRepository.save(loan);

        return GuarantorDTO.builder()
                .loanId(loan.getId())
                .memberId(guarantorMember.getId())
                .memberName(guarantorMember.getFirstName() + " " + guarantorMember.getLastName())
                .guaranteeAmount(amount)
                .build();
    }

    public LoanRepayment repayLoan(UUID loanId, BigDecimal amount) {
        // 1. Find the next unpaid installment (PENDING)
        LoanRepayment repayment = loanRepaymentRepository.findFirstByLoanIdAndStatusOrderByDueDateAsc(
                        loanId, LoanRepayment.RepaymentStatus.PENDING)
                .orElseThrow(() -> new RuntimeException("No pending repayments found. Loan might be fully paid!"));

        // 2. Validate payment amount
        // (In a real system, you'd handle partial payments, but let's keep it strict for now)
        BigDecimal dueAmount = repayment.getLoan().getMonthlyRepayment();
        if (amount.compareTo(dueAmount) < 0) {
            throw new RuntimeException("Insufficient amount. Monthly installment is: " + dueAmount);
        }

        // 3. Mark as PAID
        repayment.setPrincipalPaid(amount);
        repayment.setTotalPaid(amount);
        repayment.setStatus(LoanRepayment.RepaymentStatus.PAID);
        repayment.setPaymentDate(LocalDate.now());
        loanRepaymentRepository.save(repayment);

        // 4. Update the Main Loan Balance
        Loan loan = repayment.getLoan();
        loan.setLoanBalance(loan.getLoanBalance().subtract(amount));

        // 5. Check if Loan is fully finished
        if (loan.getLoanBalance().compareTo(BigDecimal.ZERO) <= 0) {
            loan.setStatus(Loan.LoanStatus.COMPLETED);
        }
        loanRepository.save(loan);

        return repayment;
    }
    
    public void recordRepayment(Long repaymentId, BigDecimal amount) {
        LoanRepayment repayment = loanRepaymentRepository.findById(repaymentId)
                .orElseThrow(() -> new RuntimeException("Repayment not found"));
        
        Loan loan = repayment.getLoan();
        
        repayment.setPrincipalPaid(amount);
        repayment.setTotalPaid(amount);
        repayment.setStatus(LoanRepayment.RepaymentStatus.PAID);
        repayment.setPaymentDate(LocalDate.now());
        
        loanRepaymentRepository.save(repayment);
        
        // Update loan balance
        loan.setLoanBalance(loan.getLoanBalance().subtract(amount));
        if (loan.getLoanBalance().compareTo(BigDecimal.ZERO) <= 0) {
            loan.setStatus(Loan.LoanStatus.COMPLETED);
        }
        
        loanRepository.save(loan);
    }
    
    public BigDecimal getTotalDisbursedLoans() {
        BigDecimal total = loanRepository.getTotalDisbursedLoans();
        return total != null ? total : BigDecimal.ZERO;
    }
    
    public BigDecimal getTotalOutstandingLoans() {
        BigDecimal total = loanRepository.getTotalOutstandingLoans();
        return total != null ? total : BigDecimal.ZERO;
    }
    
    public BigDecimal getTotalInterestCollected() {
        BigDecimal total = loanRepository.getTotalInterest();
        return total != null ? total : BigDecimal.ZERO;
    }
    
    private String generateLoanNumber() {
        long count = loanRepository.count();
        return "LN" + String.format("%06d", count + 1);
    }
    
    private void createRepaymentSchedule(Loan loan) {
        LocalDate paymentDate = LocalDate.now().plusMonths(1);
        
        for (int i = 1; i <= loan.getDurationMonths(); i++) {
            LoanRepayment repayment = LoanRepayment.builder()
                    .loan(loan)
                    .repaymentNumber(i)
                    .dueDate(paymentDate)
                    .status(LoanRepayment.RepaymentStatus.PENDING)
                    .build();
            
            loanRepaymentRepository.save(repayment);
            paymentDate = paymentDate.plusMonths(1);
        }
    }
    
    private LoanDTO convertToDTO(Loan loan) {
        return LoanDTO.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .memberId(loan.getMember().getId())
                .memberName(loan.getMember().getFirstName() + " " + loan.getMember().getLastName())
                .principalAmount(loan.getPrincipalAmount())
                .loanBalance(loan.getLoanBalance())
                .interestRate(loan.getInterestRate())
                .totalInterest(loan.getTotalInterest())
                .durationMonths(loan.getDurationMonths())
                .monthlyRepayment(loan.getMonthlyRepayment())
                .status(loan.getStatus().toString())
                .approvalDate(loan.getApprovalDate())
                .disbursementDate(loan.getDisbursementDate())
                .expectedRepaymentDate(loan.getExpectedRepaymentDate())
                .build();
    }
}
