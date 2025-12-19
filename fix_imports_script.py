#!/usr/bin/env python3
"""
SACCO System - Import Fixer
Fixes all import statements after migration
"""

import os
import re
from pathlib import Path

BASE_PATH = Path(r"c:\Users\JAY\OneDrive\Desktop\sacco-system")
MODULES_PATH = BASE_PATH / "src/main/java/com/sacco/sacco_system/modules"

# Module-specific import mappings
IMPORT_MAPPINGS = {
    # Generic monolithic package replacements
    "com.sacco.sacco_system.entity.": [
        ("Member", "com.sacco.sacco_system.modules.member.domain.entity.Member"),
        ("SavingsAccount", "com.sacco.sacco_system.modules.savings.domain.entity.SavingsAccount"),
        ("SavingsProduct", "com.sacco.sacco_system.modules.savings.domain.entity.SavingsProduct"),
        ("Withdrawal", "com.sacco.sacco_system.modules.savings.domain.entity.Withdrawal"),
        ("Loan", "com.sacco.sacco_system.modules.loan.domain.entity.Loan"),
        ("LoanProduct", "com.sacco.sacco_system.modules.loan.domain.entity.LoanProduct"),
        ("LoanRepayment", "com.sacco.sacco_system.modules.loan.domain.entity.LoanRepayment"),
        ("Guarantor", "com.sacco.sacco_system.modules.loan.domain.entity.Guarantor"),
        ("FinancialReport", "com.sacco.sacco_system.modules.finance.domain.entity.FinancialReport"),
        ("Transaction", "com.sacco.sacco_system.modules.finance.domain.entity.Transaction"),
        ("Charge", "com.sacco.sacco_system.modules.finance.domain.entity.Charge"),
        ("ShareCapital", "com.sacco.sacco_system.modules.finance.domain.entity.ShareCapital"),
        ("Asset", "com.sacco.sacco_system.modules.admin.domain.entity.Asset"),
        ("AuditLog", "com.sacco.sacco_system.modules.admin.domain.entity.AuditLog"),
        ("SystemSetting", "com.sacco.sacco_system.modules.admin.domain.entity.SystemSetting"),
        ("Notification", "com.sacco.sacco_system.modules.notification.domain.entity.Notification"),
    ],
    "com.sacco.sacco_system.repository.": [
        ("MemberRepository", "com.sacco.sacco_system.modules.member.domain.repository.MemberRepository"),
        ("SavingsAccountRepository", "com.sacco.sacco_system.modules.savings.domain.repository.SavingsAccountRepository"),
        ("SavingsProductRepository", "com.sacco.sacco_system.modules.savings.domain.repository.SavingsProductRepository"),
        ("LoanRepository", "com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository"),
        ("LoanProductRepository", "com.sacco.sacco_system.modules.loan.domain.repository.LoanProductRepository"),
        ("LoanRepaymentRepository", "com.sacco.sacco_system.modules.loan.domain.repository.LoanRepaymentRepository"),
        ("GuarantorRepository", "com.sacco.sacco_system.modules.loan.domain.repository.GuarantorRepository"),
        ("FinancialReportRepository", "com.sacco.sacco_system.modules.finance.domain.repository.FinancialReportRepository"),
        ("TransactionRepository", "com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository"),
        ("ChargeRepository", "com.sacco.sacco_system.modules.finance.domain.repository.ChargeRepository"),
        ("ShareCapitalRepository", "com.sacco.sacco_system.modules.finance.domain.repository.ShareCapitalRepository"),
        ("AssetRepository", "com.sacco.sacco_system.modules.admin.domain.repository.AssetRepository"),
        ("AuditLogRepository", "com.sacco.sacco_system.modules.admin.domain.repository.AuditLogRepository"),
        ("SystemSettingRepository", "com.sacco.sacco_system.modules.admin.domain.repository.SystemSettingRepository"),
    ],
    "com.sacco.sacco_system.service.": [
        ("MemberService", "com.sacco.sacco_system.modules.member.domain.service.MemberService"),
        ("SavingsService", "com.sacco.sacco_system.modules.savings.domain.service.SavingsService"),
        ("LoanService", "com.sacco.sacco_system.modules.loan.domain.service.LoanService"),
        ("LoanRepaymentService", "com.sacco.sacco_system.modules.loan.domain.service.LoanRepaymentService"),
        ("LoanLimitService", "com.sacco.sacco_system.modules.loan.domain.service.LoanLimitService"),
        ("FinancialReportService", "com.sacco.sacco_system.modules.finance.domain.service.FinancialReportService"),
        ("TransactionService", "com.sacco.sacco_system.modules.finance.domain.service.TransactionService"),
        ("AccountingService", "com.sacco.sacco_system.modules.finance.domain.service.AccountingService"),
        ("PaymentService", "com.sacco.sacco_system.modules.payment.domain.service.PaymentService"),
        ("AssetService", "com.sacco.sacco_system.modules.admin.domain.service.AssetService"),
        ("AuditService", "com.sacco.sacco_system.modules.admin.domain.service.AuditService"),
        ("SystemSettingService", "com.sacco.sacco_system.modules.admin.domain.service.SystemSettingService"),
        ("EmailService", "com.sacco.sacco_system.modules.notification.domain.service.EmailService"),
        ("SmsService", "com.sacco.sacco_system.modules.notification.domain.service.SmsService"),
        ("NotificationService", "com.sacco.sacco_system.modules.notification.domain.service.NotificationService"),
        ("ReportingService", "com.sacco.sacco_system.modules.reporting.domain.service.ReportingService"),
    ],
    "com.sacco.sacco_system.dto.": [
        ("MemberDTO", "com.sacco.sacco_system.modules.member.api.dto.MemberDTO"),
        ("SavingsAccountDTO", "com.sacco.sacco_system.modules.savings.api.dto.SavingsAccountDTO"),
        ("LoanDTO", "com.sacco.sacco_system.modules.loan.api.dto.LoanDTO"),
    ],
}

def fix_imports_in_file(file_path):
    """Fix all imports in a Java file"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original_content = content
        
        # Process each import mapping
        for old_package, replacements in IMPORT_MAPPINGS.items():
            for class_name, new_import in replacements:
                # Match various import patterns
                pattern = f"import\\s+{re.escape(old_package)}{re.escape(class_name)};"
                replacement = f"import {new_import};"
                content = re.sub(pattern, replacement, content)
        
        # Remove duplicate imports
        lines = content.split('\n')
        seen = set()
        new_lines = []
        for line in lines:
            if line.startswith('import'):
                if line not in seen:
                    seen.add(line)
                    new_lines.append(line)
            else:
                new_lines.append(line)
        
        content = '\n'.join(new_lines)
        
        # Only write if changed
        if content != original_content:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            return True
        return False
    except Exception as e:
        print(f"  Error processing {file_path}: {e}")
        return False

def fix_all_imports():
    """Fix all imports recursively in modules directory"""
    print("🔧 Fixing import statements...")
    
    fixed_count = 0
    for root, dirs, files in os.walk(MODULES_PATH):
        for file in files:
            if file.endswith('.java'):
                file_path = Path(root) / file
                if fix_imports_in_file(file_path):
                    fixed_count += 1
                    print(f"  ✓ Fixed: {file_path.relative_to(MODULES_PATH)}")
    
    print(f"\n✓ Import fixes completed: {fixed_count} files updated")

if __name__ == "__main__":
    fix_all_imports()
