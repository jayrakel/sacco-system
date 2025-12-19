#!/usr/bin/env python3
"""
Fix all incorrect imports in the modular monolith refactoring.
This script replaces old import paths with correct module-based paths.
"""

import os
import re

# Define the import mappings: old_pattern -> new_import
IMPORT_MAPPINGS = {
    # DTOs
    r'import com\.sacco\.sacco_system\.modules\.GuarantorDTO;': 'import com.sacco.sacco_system.modules.loan.api.dto.GuarantorDTO;',
    r'import com\.sacco\.sacco_system\.modules\.LoanDTO;': 'import com.sacco.sacco_system.modules.loan.api.dto.LoanDTO;',
    r'import com\.sacco\.sacco_system\.modules\.MemberDTO;': 'import com.sacco.sacco_system.modules.member.api.dto.MemberDTO;',
    r'import com\.sacco\.sacco_system\.modules\.SavingsAccountDTO;': 'import com.sacco.sacco_system.modules.savings.api.dto.SavingsAccountDTO;',
    r'import com\.sacco\.sacco_system\.modules\.LoanAgingDTO;': 'import com.sacco.sacco_system.modules.reporting.api.dto.LoanAgingDTO;',
    r'import com\.sacco\.sacco_system\.modules\.MemberStatementDTO;': 'import com.sacco.sacco_system.modules.reporting.api.dto.MemberStatementDTO;',

    # Entities
    r'import com\.sacco\.sacco_system\.modules\.Notification;': 'import com.sacco.sacco_system.modules.notification.domain.entity.Notification;',
    r'import com\.sacco\.sacco_system\.modules\.Guarantor;': 'import com.sacco.sacco_system.modules.loan.domain.entity.Guarantor;',
    r'import com\.sacco\.sacco_system\.modules\.Transaction;': 'import com.sacco.sacco_system.modules.finance.domain.entity.Transaction;',
    r'import com\.sacco\.sacco_system\.modules\.LoanRepayment;': 'import com.sacco.sacco_system.modules.loan.domain.entity.LoanRepayment;',

    # Repositories
    r'import com\.sacco\.sacco_system\.modules\.NotificationRepository;': 'import com.sacco.sacco_system.modules.notification.domain.repository.NotificationRepository;',
    r'import com\.sacco\.sacco_system\.modules\.MemberRepository;': 'import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;',
    r'import com\.sacco\.sacco_system\.modules\.SavingsAccountRepository;': 'import com.sacco.sacco_system.modules.savings.domain.repository.SavingsAccountRepository;',
    r'import com\.sacco\.sacco_system\.modules\.SavingsProductRepository;': 'import com.sacco.sacco_system.modules.savings.domain.repository.SavingsProductRepository;',
    r'import com\.sacco\.sacco_system\.modules\.LoanRepository;': 'import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;',
    r'import com\.sacco\.sacco_system\.modules\.LoanRepaymentRepository;': 'import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepaymentRepository;',
    r'import com\.sacco\.sacco_system\.modules\.LoanProductRepository;': 'import com.sacco.sacco_system.modules.loan.domain.repository.LoanProductRepository;',

    # Services
    r'import com\.sacco\.sacco_system\.modules\.NotificationService;': 'import com.sacco.sacco_system.modules.notification.domain.service.NotificationService;',
    r'import com\.sacco\.sacco_system\.modules\.PaymentService;': 'import com.sacco.sacco_system.modules.payment.domain.service.PaymentService;',
    r'import com\.sacco\.sacco_system\.modules\.ReportingService;': 'import com.sacco.sacco_system.modules.reporting.domain.service.ReportingService;',
    r'import com\.sacco\.sacco_system\.modules\.AccountingService;': 'import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;',
    r'import com\.sacco\.sacco_system\.modules\.AuditService;': 'import com.sacco.sacco_system.modules.admin.domain.service.AuditService;',
    r'import com\.sacco\.sacco_system\.modules\.LoanLimitService;': 'import com.sacco.sacco_system.modules.loan.domain.service.LoanLimitService;',
}

# Self-referencing imports to remove (patterns)
SELF_REFERENCE_PATTERNS = [
    r'import com\.sacco\.sacco_system\.modules\.(\w+)\.domain\.service\.(\w+Service);\s*\n(?=.*class \2)',
    r'import com\.sacco\.sacco_system\.modules\.(\w+)\.domain\.repository\.(\w+Repository);\s*\n(?=.*interface \2)',
    r'import com\.sacco\.sacco_system\.modules\.(\w+)\.api\.controller\.(\w+Controller);\s*\n(?=.*class \2)',
]

def fix_file_imports(filepath):
    """Fix imports in a single Java file."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()

        original_content = content

        # Apply import mappings
        for old_pattern, new_import in IMPORT_MAPPINGS.items():
            content = re.sub(old_pattern, new_import, content)

        # Remove duplicate imports
        lines = content.split('\n')
        seen_imports = set()
        new_lines = []
        for line in lines:
            if line.strip().startswith('import '):
                if line.strip() not in seen_imports:
                    seen_imports.add(line.strip())
                    new_lines.append(line)
            else:
                new_lines.append(line)
        content = '\n'.join(new_lines)

        # Only write if changed
        if content != original_content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            return True
        return False
    except Exception as e:
        print(f"Error processing {filepath}: {e}")
        return False

def main():
    """Main function to process all Java files."""
    src_dir = r'C:\Users\JAY\OneDrive\Desktop\sacco-system\src\main\java'

    fixed_count = 0
    total_count = 0

    for root, dirs, files in os.walk(src_dir):
        for file in files:
            if file.endswith('.java'):
                total_count += 1
                filepath = os.path.join(root, file)
                if fix_file_imports(filepath):
                    fixed_count += 1
                    print(f"Fixed: {filepath}")

    print(f"\n✅ Processed {total_count} files, fixed {fixed_count} files")

if __name__ == '__main__':
    main()

