package com.sacco.sacco_system.service;

import com.sacco.sacco_system.entity.Asset;
import com.sacco.sacco_system.entity.Transaction;
import com.sacco.sacco_system.repository.AssetRepository;
import com.sacco.sacco_system.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;
    private final AccountingService accountingService;
    private final TransactionRepository transactionRepository; // Optional: If you want to log system transactions

    @Transactional
    public Asset purchaseAsset(Asset asset) {
        // 1. Save Asset
        Asset savedAsset = assetRepository.save(asset);

        // 2. Post to General Ledger
        // Event: ASSET_PURCHASE
        // Description: Purchase of [Laptop HP]
        // Ref: ASSET-[UUID]
        // Amount: [Cost]
        accountingService.postEvent(
                "ASSET_PURCHASE",
                "Purchase of " + savedAsset.getName(),
                "ASSET-" + savedAsset.getId().toString().substring(0, 8),
                savedAsset.getPurchaseCost()
        );

        return savedAsset;
    }
}