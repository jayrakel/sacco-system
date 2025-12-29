package com.sacco.sacco_system.modules.auth.service.setup;

import com.sacco.sacco_system.modules.auth.controller.setup.SetupController.CriticalAdminRequest;
import com.sacco.sacco_system.modules.auth.service.setup.impl.SetupAdminWorker;
import com.sacco.sacco_system.modules.auth.service.setup.impl.SetupSystemWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SetupService {

    private final SetupAdminWorker adminWorker;
    private final SetupSystemWorker systemWorker;

    public Map<String, Object> performSystemSetup(List<CriticalAdminRequest> admins) {
        // 1. Validate Duplicates
        List<String> duplicates = findDuplicates(admins);
        if (!duplicates.isEmpty()) {
            return Map.of(
                    "success", false,
                    "message", "Duplicate emails found: " + String.join(", ", duplicates)
            );
        }

        // 2. Create Admins
        List<Map<String, Object>> userResults = new ArrayList<>();
        for (CriticalAdminRequest adminRequest : admins) {
            userResults.add(adminWorker.createCriticalAdmin(adminRequest));
        }

        // 3. Initialize Accounting
        try {
            systemWorker.initializeChartOfAccounts();
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "results", userResults
            );
        }

        // 4. Mark Complete
        systemWorker.markSetupAsComplete();

        return Map.of(
                "success", true,
                "message", "Setup process finished. Chart of Accounts initialized.",
                "results", userResults
        );
    }

    private List<String> findDuplicates(List<CriticalAdminRequest> admins) {
        Set<String> emailSet = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (CriticalAdminRequest admin : admins) {
            if (!emailSet.add(admin.getEmail())) {
                duplicates.add(admin.getEmail());
            }
        }
        return duplicates;
    }
}