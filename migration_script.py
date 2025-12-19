#!/usr/bin/env python3
"""
SACCO System - Modular Monolith Migration Script
Migrates code from monolithic structure to modular architecture
"""

import os
import shutil
import re
from pathlib import Path

# Base paths
BASE_PATH = Path(r"c:\Users\JAY\OneDrive\Desktop\sacco-system")
SRC_PATH = BASE_PATH / "src/main/java/com/sacco/sacco_system"
MODULES_PATH = SRC_PATH / "modules"

# Migration mappings: (source_pattern, target_module, source_type, package_update_func)
MIGRATIONS = {
    # Member Module
    "entity/Member.java": ("member", "entity", 
        lambda: "com.sacco.sacco_system.modules.member.domain.entity"),
    "entity/MemberStatus.java": ("member", "entity",
        lambda: "com.sacco.sacco_system.modules.member.domain.entity"),
    "repository/MemberRepository.java": ("member", "repository",
        lambda: "com.sacco.sacco_system.modules.member.domain.repository"),
    "service/MemberService.java": ("member", "service",
        lambda: "com.sacco.sacco_system.modules.member.domain.service"),
    "controller/MemberController.java": ("member", "controller",
        lambda: "com.sacco.sacco_system.modules.member.api.controller"),
    "dto/MemberDTO.java": ("member", "dto",
        lambda: "com.sacco.sacco_system.modules.member.api.dto"),
    
    # Savings Module
    "entity/SavingsAccount.java": ("savings", "entity",
        lambda: "com.sacco.sacco_system.modules.savings.domain.entity"),
    "entity/SavingsProduct.java": ("savings", "entity",
        lambda: "com.sacco.sacco_system.modules.savings.domain.entity"),
    "entity/Withdrawal.java": ("savings", "entity",
        lambda: "com.sacco.sacco_system.modules.savings.domain.entity"),
    "repository/SavingsAccountRepository.java": ("savings", "repository",
        lambda: "com.sacco.sacco_system.modules.savings.domain.repository"),
    "repository/SavingsProductRepository.java": ("savings", "repository",
        lambda: "com.sacco.sacco_system.modules.savings.domain.repository"),
    "service/SavingsService.java": ("savings", "service",
        lambda: "com.sacco.sacco_system.modules.savings.domain.service"),
    "controller/SavingsController.java": ("savings", "controller",
        lambda: "com.sacco.sacco_system.modules.savings.api.controller"),
    "dto/SavingsAccountDTO.java": ("savings", "dto",
        lambda: "com.sacco.sacco_system.modules.savings.api.dto"),
    
    # Loan Module
    "entity/Loan.java": ("loan", "entity",
        lambda: "com.sacco.sacco_system.modules.loan.domain.entity"),
    "entity/LoanProduct.java": ("loan", "entity",
        lambda: "com.sacco.sacco_system.modules.loan.domain.entity"),
    "entity/LoanRepayment.java": ("loan", "entity",
        lambda: "com.sacco.sacco_system.modules.loan.domain.entity"),
    "entity/Guarantor.java": ("loan", "entity",
        lambda: "com.sacco.sacco_system.modules.loan.domain.entity"),
    "repository/LoanRepository.java": ("loan", "repository",
        lambda: "com.sacco.sacco_system.modules.loan.domain.repository"),
    "repository/LoanProductRepository.java": ("loan", "repository",
        lambda: "com.sacco.sacco_system.modules.loan.domain.repository"),
    "repository/LoanRepaymentRepository.java": ("loan", "repository",
        lambda: "com.sacco.sacco_system.modules.loan.domain.repository"),
    "repository/GuarantorRepository.java": ("loan", "repository",
        lambda: "com.sacco.sacco_system.modules.loan.domain.repository"),
    "service/LoanService.java": ("loan", "service",
        lambda: "com.sacco.sacco_system.modules.loan.domain.service"),
    "service/LoanRepaymentService.java": ("loan", "service",
        lambda: "com.sacco.sacco_system.modules.loan.domain.service"),
    "service/LoanLimitService.java": ("loan", "service",
        lambda: "com.sacco.sacco_system.modules.loan.domain.service"),
    "controller/LoanController.java": ("loan", "controller",
        lambda: "com.sacco.sacco_system.modules.loan.api.controller"),
    "dto/LoanDTO.java": ("loan", "dto",
        lambda: "com.sacco.sacco_system.modules.loan.api.dto"),
    
    # Finance Module
    "entity/FinancialReport.java": ("finance", "entity",
        lambda: "com.sacco.sacco_system.modules.finance.domain.entity"),
    "entity/Transaction.java": ("finance", "entity",
        lambda: "com.sacco.sacco_system.modules.finance.domain.entity"),
    "entity/Charge.java": ("finance", "entity",
        lambda: "com.sacco.sacco_system.modules.finance.domain.entity"),
    "entity/ShareCapital.java": ("finance", "entity",
        lambda: "com.sacco.sacco_system.modules.finance.domain.entity"),
    "repository/FinancialReportRepository.java": ("finance", "repository",
        lambda: "com.sacco.sacco_system.modules.finance.domain.repository"),
    "repository/TransactionRepository.java": ("finance", "repository",
        lambda: "com.sacco.sacco_system.modules.finance.domain.repository"),
    "repository/ChargeRepository.java": ("finance", "repository",
        lambda: "com.sacco.sacco_system.modules.finance.domain.repository"),
    "repository/ShareCapitalRepository.java": ("finance", "repository",
        lambda: "com.sacco.sacco_system.modules.finance.domain.repository"),
    "service/FinancialReportService.java": ("finance", "service",
        lambda: "com.sacco.sacco_system.modules.finance.domain.service"),
    "service/TransactionService.java": ("finance", "service",
        lambda: "com.sacco.sacco_system.modules.finance.domain.service"),
    "service/AccountingService.java": ("finance", "service",
        lambda: "com.sacco.sacco_system.modules.finance.domain.service"),
    "controller/FinancialReportController.java": ("finance", "controller",
        lambda: "com.sacco.sacco_system.modules.finance.api.controller"),
    "controller/TransactionController.java": ("finance", "controller",
        lambda: "com.sacco.sacco_system.modules.finance.api.controller"),
    "controller/AccountingController.java": ("finance", "controller",
        lambda: "com.sacco.sacco_system.modules.finance.api.controller"),
    
    # Payment Module
    "service/PaymentService.java": ("payment", "service",
        lambda: "com.sacco.sacco_system.modules.payment.domain.service"),
    "controller/PaymentController.java": ("payment", "controller",
        lambda: "com.sacco.sacco_system.modules.payment.api.controller"),
    
    # Admin Module
    "entity/Asset.java": ("admin", "entity",
        lambda: "com.sacco.sacco_system.modules.admin.domain.entity"),
    "entity/AuditLog.java": ("admin", "entity",
        lambda: "com.sacco.sacco_system.modules.admin.domain.entity"),
    "entity/SystemSetting.java": ("admin", "entity",
        lambda: "com.sacco.sacco_system.modules.admin.domain.entity"),
    "repository/AssetRepository.java": ("admin", "repository",
        lambda: "com.sacco.sacco_system.modules.admin.domain.repository"),
    "repository/AuditLogRepository.java": ("admin", "repository",
        lambda: "com.sacco.sacco_system.modules.admin.domain.repository"),
    "repository/SystemSettingRepository.java": ("admin", "repository",
        lambda: "com.sacco.sacco_system.modules.admin.domain.repository"),
    "service/AssetService.java": ("admin", "service",
        lambda: "com.sacco.sacco_system.modules.admin.domain.service"),
    "service/AuditService.java": ("admin", "service",
        lambda: "com.sacco.sacco_system.modules.admin.domain.service"),
    "service/SystemSettingService.java": ("admin", "service",
        lambda: "com.sacco.sacco_system.modules.admin.domain.service"),
    "controller/AssetController.java": ("admin", "controller",
        lambda: "com.sacco.sacco_system.modules.admin.api.controller"),
    "controller/AuditController.java": ("admin", "controller",
        lambda: "com.sacco.sacco_system.modules.admin.api.controller"),
    "controller/SystemSettingController.java": ("admin", "controller",
        lambda: "com.sacco.sacco_system.modules.admin.api.controller"),
    
    # Notification Module
    "service/EmailService.java": ("notification", "service",
        lambda: "com.sacco.sacco_system.modules.notification.domain.service"),
    "service/SmsService.java": ("notification", "service",
        lambda: "com.sacco.sacco_system.modules.notification.domain.service"),
    "service/NotificationService.java": ("notification", "service",
        lambda: "com.sacco.sacco_system.modules.notification.domain.service"),
    "controller/NotificationController.java": ("notification", "controller",
        lambda: "com.sacco.sacco_system.modules.notification.api.controller"),
    
    # Reporting Module
    "service/ReportingService.java": ("reporting", "service",
        lambda: "com.sacco.sacco_system.modules.reporting.domain.service"),
    "controller/ReportingController.java": ("reporting", "controller",
        lambda: "com.sacco.sacco_system.modules.reporting.api.controller"),
}

def update_package_in_file(file_path, new_package):
    """Update package statement and common imports in Java file"""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Update package statement
    content = re.sub(
        r'package\s+com\.sacco\.sacco_system\.([^;]+);',
        f'package {new_package};',
        content
    )
    
    # Common import replacements
    replacements = [
        (r'import com\.sacco\.sacco_system\.entity\.',
         'import com.sacco.sacco_system.modules.'),
        (r'import com\.sacco\.sacco_system\.repository\.',
         'import com.sacco.sacco_system.modules.'),
        (r'import com\.sacco\.sacco_system\.service\.',
         'import com.sacco.sacco_system.modules.'),
        (r'import com\.sacco\.sacco_system\.dto\.',
         'import com.sacco.sacco_system.modules.'),
    ]
    
    for pattern, replacement in replacements:
        content = re.sub(pattern, replacement, content)
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)

def migrate_files():
    """Execute the migration"""
    print("🚀 Starting SACCO Modular Migration...")
    
    migrated_count = 0
    failed_count = 0
    
    for source_pattern, (module, dest_type, pkg_func) in MIGRATIONS.items():
        source_file = SRC_PATH / source_pattern
        
        # Determine layer (domain, api, internal)
        if dest_type in ["entity", "repository", "service"]:
            layer = "domain"
        elif dest_type in ["controller", "dto"]:
            layer = "api"
        else:
            layer = "internal"
        
        # Build target path
        target_dir = MODULES_PATH / module / layer / dest_type
        target_file = target_dir / source_file.name
        
        try:
            # Skip if source doesn't exist
            if not source_file.exists():
                print(f"⊘ Skipped: {source_pattern} (file not found)")
                continue
            
            # Create target directory if needed
            target_dir.mkdir(parents=True, exist_ok=True)
            
            # Copy file
            shutil.copy2(source_file, target_file)
            
            # Update package and imports
            new_package = pkg_func()
            update_package_in_file(target_file, new_package)
            
            print(f"✓ Migrated: {source_pattern} → {module}/{layer}/{dest_type}")
            migrated_count += 1
            
        except Exception as e:
            print(f"✗ Failed to migrate {source_pattern}: {str(e)}")
            failed_count += 1
    
    print(f"\n📊 Migration Summary:")
    print(f"   ✓ Successfully migrated: {migrated_count}")
    print(f"   ✗ Failed: {failed_count}")
    print(f"   ⊘ Skipped: {len(MIGRATIONS) - migrated_count - failed_count}")

if __name__ == "__main__":
    migrate_files()
