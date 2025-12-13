package com.sacco.sacco_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "savings_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsAccount {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String accountNumber;
    
    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
    
    private BigDecimal balance = BigDecimal.ZERO;
    
    private BigDecimal totalDeposits = BigDecimal.ZERO;
    
    private BigDecimal totalWithdrawals = BigDecimal.ZERO;
    
    @Enumerated(EnumType.STRING)
    private AccountStatus status = AccountStatus.ACTIVE;
    
    private LocalDateTime accountOpenDate;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        accountOpenDate = LocalDateTime.now();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum AccountStatus {
        ACTIVE, CLOSED, FROZEN
    }
}
