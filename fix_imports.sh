#!/bin/bash

# Fix admin module imports
sed -i 's|import com\.sacco\.sacco_system\.modules\.Asset;|import com.sacco.sacco_system.modules.admin.domain.entity.Asset;|g' src/main/java/com/sacco/sacco_system/modules/admin/api/controller/*.java
sed -i 's|import com\.sacco\.sacco_system\.modules\.AssetRepository;|import com.sacco.sacco_system.modules.admin.domain.repository.AssetRepository;|g' src/main/java/com/sacco/sacco_system/modules/admin/**/*.java
sed -i 's|import com\.sacco\.sacco_system\.modules\.AssetService;|import com.sacco.sacco_system.modules.admin.domain.service.AssetService;|g' src/main/java/com/sacco/sacco_system/modules/admin/**/*.java

sed -i 's|import com\.sacco\.sacco_system\.modules\.AuditLog;|import com.sacco.sacco_system.modules.admin.domain.entity.AuditLog;|g' src/main/java/com/sacco/sacco_system/modules/admin/**/*.java
sed -i 's|import com\.sacco\.sacco_system\.modules\.AuditLogRepository;|import com.sacco.sacco_system.modules.admin.domain.repository.AuditLogRepository;|g' src/main/java/com/sacco/sacco_system/modules/admin/**/*.java
sed -i 's|import com\.sacco\.sacco_system\.modules\.AuditService;|import com.sacco.sacco_system.modules.admin.domain.service.AuditService;|g' src/main/java/com/sacco/sacco_system/modules/admin/**/*.java

sed -i 's|import com\.sacco\.sacco_system\.modules\.SystemSetting;|import com.sacco.sacco_system.modules.admin.domain.entity.SystemSetting;|g' src/main/java/com/sacco/sacco_system/modules/admin/**/*.java
sed -i 's|import com\.sacco\.sacco_system\.modules\.SystemSettingRepository;|import com.sacco.sacco_system.modules.admin.domain.repository.SystemSettingRepository;|g' src/main/java/com/sacco/sacco_system/modules/admin/**/*.java
sed -i 's|import com\.sacco\.sacco_system\.modules\.SystemSettingService;|import com.sacco.sacco_system.modules.admin.domain.service.SystemSettingService;|g' src/main/java/com/sacco/sacco_system/modules/admin/**/*.java

echo "Admin imports fixed"
