package com.sacco.sacco_system.modules.assets.service;

import com.sacco.sacco_system.modules.assets.model.Asset;
import com.sacco.sacco_system.modules.savings.model.Transaction;
import com.sacco.sacco_system.modules.assets.repository.AssetRepository;
import com.sacco.sacco_system.modules.savings.repository.TransactionRepository;
import com.sacco.sacco_system.modules.accounting.service.AccountingService;
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