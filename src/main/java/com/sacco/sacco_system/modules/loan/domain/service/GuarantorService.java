package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.core.exception.ApiException;
import com.sacco.sacco_system.modules.loan.domain.entity.Guarantor;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.repository.GuarantorRepository;
import com.sacco.sacco_system.modules.savings.domain.entity.SavingsAccount;
import com.sacco.sacco_system.modules.savings.domain.repository.SavingsAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuarantorService {

    private final GuarantorRepository guarantorRepository;
    private final SavingsAccountRepository savingsAccountRepository;

    /**
     * ✅ Lock funds for all accepted guarantors when a loan is Disbursed
     */
    @Transactional
    public void lockGuarantorFunds(Loan loan) {
        log.info("🔒 Locking guarantor funds for Loan: {}", loan.getLoanNumber());

        for (Guarantor guarantor : loan.getGuarantors()) {
            if (guarantor.getStatus() == Guarantor.GuarantorStatus.ACCEPTED) {
                // Find member's primary/ordinary savings account
                SavingsAccount account = findPrimarySavingsAccount(guarantor.getMember().getId());

                // Update locked amount
                BigDecimal currentLock = account.getLockedAmount();
                account.setLockedAmount(currentLock.add(guarantor.getGuaranteedAmount()));

                savingsAccountRepository.save(account);
                log.info("   Locked {} from Member {}", guarantor.getGuaranteedAmount(), guarantor.getMember().getMemberNumber());
            }
        }
    }

    /**
     * ✅ Release funds when a loan is Closed
     */
    @Transactional
    public void unlockGuarantorFunds(Loan loan) {
        log.info("🔓 Unlocking guarantor funds for Loan: {}", loan.getLoanNumber());

        for (Guarantor guarantor : loan.getGuarantors()) {
            if (guarantor.getStatus() == Guarantor.GuarantorStatus.ACCEPTED) {
                SavingsAccount account = findPrimarySavingsAccount(guarantor.getMember().getId());

                // Release the lock
                BigDecimal currentLock = account.getLockedAmount();
                BigDecimal newLock = currentLock.subtract(guarantor.getGuaranteedAmount());

                // Prevent negative locks (sanity check)
                if (newLock.compareTo(BigDecimal.ZERO) < 0) newLock = BigDecimal.ZERO;

                account.setLockedAmount(newLock);
                savingsAccountRepository.save(account);
                log.info("   Released {} for Member {}", guarantor.getGuaranteedAmount(), guarantor.getMember().getMemberNumber());
            }
        }
    }

    /**
     * Helper: Find the savings account to lock funds in.
     * Logic: Prioritize "Ordinary Savings", fallback to first active account.
     */
    private SavingsAccount findPrimarySavingsAccount(java.util.UUID memberId) {
        List<SavingsAccount> accounts = savingsAccountRepository.findByMemberId(memberId);

        return accounts.stream()
                .filter(a -> a.getProduct() != null && "Ordinary Savings".equalsIgnoreCase(a.getProduct().getProductName()))
                .findFirst()
                .orElse(accounts.stream().findFirst()
                        .orElseThrow(() -> new ApiException("Guarantor has no savings account to lock funds!", 400)));
    }
}