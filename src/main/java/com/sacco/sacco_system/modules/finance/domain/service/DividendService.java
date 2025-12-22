package com.sacco.sacco_system.modules.finance.domain.service;

import com.sacco.sacco_system.modules.finance.domain.entity.Dividend;
import com.sacco.sacco_system.modules.finance.domain.entity.ShareCapital;
import com.sacco.sacco_system.modules.finance.domain.repository.DividendRepository;
import com.sacco.sacco_system.modules.finance.domain.repository.ShareCapitalRepository;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dividend Service
 * Calculates and distributes profits to members based on share ownership
 * Integrated with accounting system
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DividendService {

    private final DividendRepository dividendRepository;
    private final ShareCapitalRepository shareCapitalRepository;
    private final MemberRepository memberRepository;
    private final ShareCapitalService shareCapitalService;
    private final AccountingService accountingService;

    /**
     * Declare dividends for a fiscal year
     * Allocates profits proportionally based on share ownership
     */
    public List<Dividend> declareDividends(Integer fiscalYear, BigDecimal totalDividendPool, String notes) {
        if (totalDividendPool.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Dividend pool must be positive");
        }

        // Get total share capital
        BigDecimal totalShareCapital = shareCapitalService.getTotalShareCapital();
        if (totalShareCapital.compareTo(BigDecimal.ZERO) == 0) {
            throw new RuntimeException("No share capital exists. Cannot declare dividends.");
        }

        // Get all members with shares
        List<ShareCapital> allShares = shareCapitalRepository.findAll();
        List<Dividend> dividends = new ArrayList<>();

        LocalDate today = LocalDate.now();

        for (ShareCapital shareCapital : allShares) {
            if (shareCapital.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
                // Calculate member's share percentage
                BigDecimal sharePercentage = shareCapital.getPaidAmount()
                        .divide(totalShareCapital, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

                // Calculate dividend amount
                BigDecimal dividendAmount = totalDividendPool
                        .multiply(shareCapital.getPaidAmount())
                        .divide(totalShareCapital, 2, RoundingMode.HALF_UP);

                // Create dividend record
                Dividend dividend = Dividend.builder()
                        .member(shareCapital.getMember())
                        .fiscalYear(fiscalYear)
                        .declarationDate(today)
                        .totalDividendPool(totalDividendPool)
                        .memberSharePercentage(sharePercentage)
                        .dividendAmount(dividendAmount)
                        .status(Dividend.DividendStatus.DECLARED)
                        .notes(notes)
                        .build();

                dividends.add(dividendRepository.save(dividend));

                log.info("Dividend declared for member {}: KES {} ({}% of pool)",
                        shareCapital.getMember().getMemberNumber(),
                        dividendAmount,
                        sharePercentage);
            }
        }

        log.info("Dividends declared for {} members. Total pool: KES {}",
                dividends.size(), totalDividendPool);

        return dividends;
    }

    /**
     * Pay dividend to a specific member
     */
    public Dividend payDividend(UUID dividendId, String paymentReference) {
        Dividend dividend = dividendRepository.findById(dividendId)
                .orElseThrow(() -> new RuntimeException("Dividend not found"));

        if (dividend.getStatus() == Dividend.DividendStatus.PAID) {
            throw new RuntimeException("Dividend already paid");
        }

        if (dividend.getStatus() == Dividend.DividendStatus.CANCELLED) {
            throw new RuntimeException("Dividend was cancelled");
        }

        // Update dividend status
        dividend.setStatus(Dividend.DividendStatus.PAID);
        dividend.setPaymentDate(LocalDate.now());
        dividend.setPaymentReference(paymentReference);

        Dividend saved = dividendRepository.save(dividend);

        // ✅ POST TO ACCOUNTING
        accountingService.postDividendPayment(dividend.getMember(), dividend.getDividendAmount());
        // Creates: DEBIT Dividends Payable (2030), CREDIT Cash (1020)

        log.info("Dividend paid to member {}: KES {}",
                dividend.getMember().getMemberNumber(),
                dividend.getDividendAmount());

        return saved;
    }

    /**
     * Pay all declared dividends for a fiscal year
     */
    public List<Dividend> payAllDividends(Integer fiscalYear) {
        List<Dividend> declaredDividends = dividendRepository
                .findByFiscalYearAndStatus(fiscalYear, Dividend.DividendStatus.DECLARED);

        List<Dividend> paidDividends = new ArrayList<>();

        for (Dividend dividend : declaredDividends) {
            String reference = "DIV-" + fiscalYear + "-" + System.currentTimeMillis();
            paidDividends.add(payDividend(dividend.getId(), reference));
        }

        log.info("Paid {} dividends for fiscal year {}", paidDividends.size(), fiscalYear);

        return paidDividends;
    }

    /**
     * Get member's dividend history
     */
    public List<Dividend> getMemberDividends(UUID memberId) {
        return dividendRepository.findByMemberId(memberId);
    }

    /**
     * Get dividends for a fiscal year
     */
    public List<Dividend> getDividendsByYear(Integer fiscalYear) {
        return dividendRepository.findByFiscalYear(fiscalYear);
    }

    /**
     * Get total dividends paid to a member
     */
    public BigDecimal getTotalDividendsReceived(UUID memberId) {
        BigDecimal total = dividendRepository.getTotalDividendsReceivedByMember(memberId);
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Get dividend statistics for a year
     */
    public Map<String, Object> getDividendStatistics(Integer fiscalYear) {
        List<Dividend> allDividends = dividendRepository.findByFiscalYear(fiscalYear);

        long declared = allDividends.stream()
                .filter(d -> d.getStatus() == Dividend.DividendStatus.DECLARED)
                .count();

        long paid = allDividends.stream()
                .filter(d -> d.getStatus() == Dividend.DividendStatus.PAID)
                .count();

        BigDecimal totalDeclared = allDividends.stream()
                .map(Dividend::getDividendAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = dividendRepository.getTotalPaidDividendsByYear(fiscalYear);
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;

        return Map.of(
                "fiscalYear", fiscalYear,
                "totalMembers", allDividends.size(),
                "declaredCount", declared,
                "paidCount", paid,
                "totalDeclared", totalDeclared,
                "totalPaid", totalPaid,
                "pending", totalDeclared.subtract(totalPaid)
        );
    }

    /**
     * Cancel a dividend (before payment)
     */
    public Dividend cancelDividend(UUID dividendId, String reason) {
        Dividend dividend = dividendRepository.findById(dividendId)
                .orElseThrow(() -> new RuntimeException("Dividend not found"));

        if (dividend.getStatus() == Dividend.DividendStatus.PAID) {
            throw new RuntimeException("Cannot cancel paid dividend");
        }

        dividend.setStatus(Dividend.DividendStatus.CANCELLED);
        dividend.setNotes(dividend.getNotes() + "\nCancelled: " + reason);

        return dividendRepository.save(dividend);
    }
}

