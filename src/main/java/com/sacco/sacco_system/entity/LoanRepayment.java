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
@Table(name = "loan_repayments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRepayment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;
    
    private Integer repaymentNumber;
    
    private BigDecimal principalPaid = BigDecimal.ZERO;
    
    private BigDecimal interestPaid = BigDecimal.ZERO;
    
    private BigDecimal totalPaid = BigDecimal.ZERO;
    
    @Enumerated(EnumType.STRING)
    private RepaymentStatus status = RepaymentStatus.PENDING;
    
    private LocalDate dueDate;
    
    private LocalDate paymentDate;
    
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
    
    public enum RepaymentStatus {
        PENDING, PARTIAL, PAID, OVERDUE, DEFAULTED
    }
}
