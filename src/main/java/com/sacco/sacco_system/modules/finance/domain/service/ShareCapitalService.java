package com.sacco.sacco_system.modules.finance.domain.service;

import com.sacco.sacco_system.modules.finance.domain.entity.ShareCapital;
import com.sacco.sacco_system.modules.finance.domain.repository.ShareCapitalRepository;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Share Capital Service
 * Manages member share purchases and tracking
 * Integrated with accounting system
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ShareCapitalService {

    private final ShareCapitalRepository shareCapitalRepository;
    private final MemberRepository memberRepository;
    private final AccountingService accountingService;

    /**
     * Purchase shares for a member
     * Creates journal entry: DEBIT Cash, CREDIT Share Capital
     */
    public ShareCapital purchaseShares(UUID memberId, BigDecimal amount, String paymentReference) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Share purchase amount must be positive");
        }

        // Get or create share capital record for member
        ShareCapital shareCapital = shareCapitalRepository.findByMemberId(memberId)
                .orElse(ShareCapital.builder()
                        .member(member)
                        .shareValue(BigDecimal.valueOf(100)) // Default share value KES 100
                        .totalShares(BigDecimal.ZERO)
                        .paidShares(BigDecimal.ZERO)
                        .paidAmount(BigDecimal.ZERO)
                        .build());

        // Calculate number of shares purchased
        BigDecimal sharesPurchased = amount.divide(shareCapital.getShareValue(), 2, java.math.RoundingMode.DOWN);

        // Update share capital
        shareCapital.setPaidShares(shareCapital.getPaidShares().add(sharesPurchased));
        shareCapital.setPaidAmount(shareCapital.getPaidAmount().add(amount));
        shareCapital.setTotalShares(shareCapital.getPaidShares()); // Total = Paid for now

        ShareCapital saved = shareCapitalRepository.save(shareCapital);

        // Update member total shares
        member.setTotalShares(member.getTotalShares() != null ?
                member.getTotalShares().add(amount) : amount);
        memberRepository.save(member);

        // ✅ POST TO ACCOUNTING
        accountingService.postShareCapitalPurchase(member, amount);
        // Creates: DEBIT Cash (1020), CREDIT Share Capital (2020)

        log.info("Member {} purchased {} shares for KES {}",
                member.getMemberNumber(), sharesPurchased, amount);

        return saved;
    }

    /**
     * Get member's share capital details
     */
    public ShareCapital getMemberShares(UUID memberId) {
        return shareCapitalRepository.findByMemberId(memberId)
                .orElse(ShareCapital.builder()
                        .member(memberRepository.findById(memberId)
                                .orElseThrow(() -> new RuntimeException("Member not found")))
                        .shareValue(BigDecimal.valueOf(100))
                        .totalShares(BigDecimal.ZERO)
                        .paidShares(BigDecimal.ZERO)
                        .paidAmount(BigDecimal.ZERO)
                        .build());
    }

    /**
     * Get total share capital in the SACCO
     */
    public BigDecimal getTotalShareCapital() {
        BigDecimal total = shareCapitalRepository.getTotalShareCapital();
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Get all share capital records (for reports)
     */
    public List<ShareCapital> getAllShareCapital() {
        return shareCapitalRepository.findAll();
    }

    /**
     * Calculate member's share percentage in SACCO
     */
    public BigDecimal getMemberSharePercentage(UUID memberId) {
        BigDecimal memberShares = getMemberShares(memberId).getPaidAmount();
        BigDecimal totalShares = getTotalShareCapital();

        if (totalShares.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return memberShares.divide(totalShares, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Get share value (price per share)
     */
    public BigDecimal getShareValue() {
        // For now, fixed at KES 100 per share
        // Could be dynamic based on SACCO valuation in future
        return BigDecimal.valueOf(100);
    }
}

