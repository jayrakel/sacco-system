package com.sacco.sacco_system.modules.admin.domain.service;

import com.sacco.sacco_system.modules.admin.domain.entity.Asset;
import com.sacco.sacco_system.modules.finance.domain.entity.Transaction;
import com.sacco.sacco_system.modules.admin.domain.repository.AssetRepository;
import com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;
    // private final AccountingService accountingService;  // Commented out temporarily
    private final TransactionRepository transactionRepository;

    @Transactional
    public Asset purchaseAsset(Asset asset) {
        // 1. Save Asset
        Asset savedAsset = assetRepository.save(asset);

        // 2. Post to General Ledger - Commented out for now
        // Event: ASSET_PURCHASE
        // Description: Purchase of [Laptop HP]
        // Ref: ASSET-[UUID]
        // Amount: [Cost]
        // accountingService.postEvent(
        //         "ASSET_PURCHASE",
        //         "Purchase of " + savedAsset.getName(),
        //         "ASSET-" + savedAsset.getId().toString().substring(0, 8),
        //         savedAsset.getPurchaseCost()
        // );

        return savedAsset;
    }
}



