package com.sacco.sacco_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String loanNumber;
    
    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
    
    private BigDecimal principalAmount;
    
    private BigDecimal loanBalance;
    
    private BigDecimal interestRate; // percentage
    
    private BigDecimal totalInterest;
    
    private Integer durationMonths;
    
    private BigDecimal monthlyRepayment;
    
    @Enumerated(EnumType.STRING)
    private LoanStatus status = LoanStatus.PENDING;
    
    private LocalDate approvalDate;
    
    private LocalDate disbursementDate;
    
    private LocalDate expectedRepaymentDate;
    
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
    
    public enum LoanStatus {
        PENDING, APPROVED, REJECTED, DISBURSED, COMPLETED, DEFAULTED
    }
}
