package com.sacco.sacco_system.modules.reporting.service;

import com.sacco.sacco_system.modules.loans.dto.LoanAgingDTO;
import com.sacco.sacco_system.modules.members.dto.MemberStatementDTO;
import com.sacco.sacco_system.modules.loans.model.Loan;
import com.sacco.sacco_system.modules.loans.model.LoanRepayment;
import com.sacco.sacco_system.modules.members.model.Member;
import com.sacco.sacco_system.modules.savings.model.Transaction;
import com.sacco.sacco_system.modules.loans.repository.LoanRepository;
import com.sacco.sacco_system.modules.loans.repository.LoanRepaymentRepository;
import com.sacco.sacco_system.modules.members.repository.MemberRepository;
import com.sacco.sacco_system.modules.savings.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportingService {

    private final TransactionRepository transactionRepository;
    private final MemberRepository memberRepository;
    private final LoanRepository loanRepository;
    private final LoanRepaymentRepository loanRepaymentRepository;

    /**
     * GENERATE MEMBER STATEMENT
     * Returns a chronological list of all transactions for a specific member.
     */
    @Transactional(readOnly = true)
    public List<MemberStatementDTO> getMemberStatement(UUID memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        List<Transaction> transactions = transactionRepository.findByMemberIdOrderByTransactionDateDesc(memberId);

        return transactions.stream().map(tx -> MemberStatementDTO.builder()
                .date(tx.getTransactionDate().toLocalDate())
                .reference(tx.getTransactionId())
                .description(tx.getDescription())
                .type(tx.getType().toString())
                .amount(tx.getAmount())
                .runningBalance(tx.getBalanceAfter())
                .build()).collect(Collectors.toList());
    }

    /**
     * GENERATE LOAN AGING (DELINQUENCY) REPORT
     * Categorizes loans based on how many days the oldest installment is overdue.
     */
    @Transactional(readOnly = true)
    public List<LoanAgingDTO> getLoanAgingReport() {
        List<Loan> activeLoans = loanRepository.findByStatus(Loan.LoanStatus.DISBURSED);
        List<LoanAgingDTO> report = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Loan loan : activeLoans) {
            // Find the OLDEST unpaid installment
            LoanRepayment oldestDue = loanRepaymentRepository.findFirstByLoanIdAndStatusOrderByDueDateAsc(
                    loan.getId(), LoanRepayment.RepaymentStatus.PENDING).orElse(null);

            // If there is an unpaid installment and it's past due
            if (oldestDue != null && oldestDue.getDueDate().isBefore(today)) {
                long daysOverdue = ChronoUnit.DAYS.between(oldestDue.getDueDate(), today);

                String category;
                if (daysOverdue <= 30) category = "1 - 30 Days (Watch)";
                else if (daysOverdue <= 60) category = "31 - 60 Days (Substandard)";
                else if (daysOverdue <= 90) category = "61 - 90 Days (Doubtful)";
                else category = "90+ Days (Loss)";

                report.add(LoanAgingDTO.builder()
                        .loanNumber(loan.getLoanNumber())
                        .memberName(loan.getMember().getFirstName() + " " + loan.getMember().getLastName())
                        .amountOutstanding(loan.getLoanBalance())
                        .daysOverdue((int) daysOverdue)
                        .category(category)
                        .build());
            }
        }
        return report;
    }
}