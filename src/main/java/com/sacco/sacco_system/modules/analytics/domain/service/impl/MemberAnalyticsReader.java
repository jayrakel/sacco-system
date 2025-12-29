package com.sacco.sacco_system.modules.analytics.domain.service.impl;

import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberAnalyticsReader {

    private final MemberRepository memberRepository;

    public Map<String, Object> getMemberGrowthAnalytics() {
        List<Member> allMembers = memberRepository.findAll();

        Map<String, Long> membersByMonth = allMembers.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getCreatedAt().toLocalDate().withDayOfMonth(1).toString(),
                        Collectors.counting()
                ));

        long totalMembers = allMembers.size();
        long activeMembers = allMembers.stream()
                .filter(m -> m.getStatus() == Member.MemberStatus.ACTIVE)
                .count();

        return Map.of(
                "totalMembers", totalMembers,
                "activeMembers", activeMembers,
                "inactiveMembers", totalMembers - activeMembers,
                "membersByMonth", membersByMonth,
                "activePercentage", totalMembers > 0 ? (activeMembers * 100.0 / totalMembers) : 0
        );
    }

    public Map<String, Object> getTopPerformers(int limit) {
        List<Map<String, Object>> topSavers = memberRepository.findAll().stream()
                .sorted((m1, m2) -> {
                    BigDecimal s1 = m1.getTotalSavings() != null ? m1.getTotalSavings() : BigDecimal.ZERO;
                    BigDecimal s2 = m2.getTotalSavings() != null ? m2.getTotalSavings() : BigDecimal.ZERO;
                    return s2.compareTo(s1);
                })
                .limit(limit)
                .map(m -> {
                    Map<String, Object> memberData = new HashMap<>();
                    memberData.put("memberNumber", m.getMemberNumber());
                    memberData.put("name", m.getFirstName() + " " + m.getLastName());
                    memberData.put("totalSavings", m.getTotalSavings() != null ? m.getTotalSavings() : BigDecimal.ZERO);
                    memberData.put("totalShares", m.getTotalShares() != null ? m.getTotalShares() : BigDecimal.ZERO);
                    return memberData;
                })
                .collect(Collectors.toList());

        return Map.of("topSavers", topSavers);
    }
}