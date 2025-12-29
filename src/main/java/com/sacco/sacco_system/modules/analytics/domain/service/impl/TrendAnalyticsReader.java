package com.sacco.sacco_system.modules.analytics.domain.service.impl;

import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrendAnalyticsReader {

    private final MemberRepository memberRepository;
    private final LoanRepository loanRepository;

    public Map<String, Object> getPerformanceTrends(int months) {
        LocalDate startDate = LocalDate.now().minusMonths(months);
        List<Map<String, Object>> trends = new ArrayList<>();

        for (int i = 0; i < months; i++) {
            LocalDate monthStart = startDate.plusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

            long membersJoined = memberRepository.findAll().stream()
                    .filter(m -> {
                        LocalDate createdDate = m.getCreatedAt().toLocalDate();
                        return !createdDate.isBefore(monthStart) && !createdDate.isAfter(monthEnd);
                    })
                    .count();

            long loansDisbursed = loanRepository.findAll().stream()
                    .filter(l -> l.getDisbursementDate() != null)
                    .filter(l -> !l.getDisbursementDate().isBefore(monthStart) && !l.getDisbursementDate().isAfter(monthEnd))
                    .count();

            trends.add(Map.of(
                    "month", monthStart.toString(),
                    "membersJoined", membersJoined,
                    "loansDisbursed", loansDisbursed
            ));
        }

        return Map.of(
                "period", months + " months",
                "trends", trends
        );
    }
}