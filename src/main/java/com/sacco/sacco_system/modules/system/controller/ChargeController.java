package com.sacco.sacco_system.modules.system.controller;

import com.sacco.sacco_system.modules.system.model.Charge;
import com.sacco.sacco_system.modules.system.repository.ChargeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/charges")
@RequiredArgsConstructor
public class ChargeController {

    private final ChargeRepository chargeRepository;

    @GetMapping("/loan/{loanId}")
    public ResponseEntity<Map<String, Object>> getLoanCharges(@PathVariable UUID loanId) {
        List<Charge> charges = chargeRepository.findByLoanId(loanId);
        return ResponseEntity.ok(Map.of("success", true, "data", charges));
    }

    @PostMapping("/{id}/waive")
    public ResponseEntity<Map<String, Object>> waiveCharge(@PathVariable UUID id, @RequestParam String reason) {
        Charge charge = chargeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Charge not found"));

        if (charge.getStatus() == Charge.ChargeStatus.PAID) {
            throw new RuntimeException("Cannot waive a paid charge.");
        }

        charge.setStatus(Charge.ChargeStatus.WAIVED);
        charge.setWaived(true);
        charge.setWaiverReason(reason);
        chargeRepository.save(charge);

        return ResponseEntity.ok(Map.of("success", true, "message", "Charge Waived Successfully"));
    }
}