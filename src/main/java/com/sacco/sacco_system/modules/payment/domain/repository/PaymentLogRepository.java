package com.sacco.sacco_system.modules.payment.domain.repository;

import com.sacco.sacco_system.modules.payment.domain.entity.PaymentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentLogRepository extends JpaRepository<PaymentLog, UUID> {
    Optional<PaymentLog> findByCheckoutRequestId(String checkoutRequestId);
}