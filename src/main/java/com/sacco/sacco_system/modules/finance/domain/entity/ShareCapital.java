package com.sacco.sacco_system.modules.finance.domain.entity;
import com.sacco.sacco_system.modules.finance.domain.entity.ShareCapital;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import com.sacco.sacco_system.modules.member.domain.entity.Member;

@Entity
@Table(name = "share_capital")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareCapital {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
    
    private BigDecimal shareValue = BigDecimal.ZERO;
    
    private BigDecimal totalShares = BigDecimal.ZERO;
    
    private BigDecimal paidShares = BigDecimal.ZERO;
    
    private BigDecimal paidAmount = BigDecimal.ZERO;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}






