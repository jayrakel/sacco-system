import re
import os
import glob

# Define cross-module import mappings
IMPORT_MAPPINGS = {
    # From finance module - fixing imports to other modules
    'MemberRepository': 'com.sacco.sacco_system.modules.member.domain.repository.MemberRepository',
    'SavingsAccountRepository': 'com.sacco.sacco_system.modules.savings.domain.repository.SavingsAccountRepository',
    'SavingsProductRepository': 'com.sacco.sacco_system.modules.savings.domain.repository.SavingsProductRepository',
    'LoanRepository': 'com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository',
    'LoanRepaymentRepository': 'com.sacco.sacco_system.modules.loan.domain.repository.LoanRepaymentRepository',
    'WithdrawalRepository': 'com.sacco.sacco_system.modules.savings.domain.repository.WithdrawalRepository',
    'SavingsAccount': 'com.sacco.sacco_system.modules.savings.domain.entity.SavingsAccount',
    'Loan': 'com.sacco.sacco_system.modules.loan.domain.entity.Loan',
    'LoanRepayment': 'com.sacco.sacco_system.modules.loan.domain.entity.LoanRepayment',
    'Member': 'com.sacco.sacco_system.modules.member.domain.entity.Member',
    'SavingsProduct': 'com.sacco.sacco_system.modules.savings.domain.entity.SavingsProduct',
    'ShareCapitalRepository': 'com.sacco.sacco_system.modules.finance.domain.repository.ShareCapitalRepository',
    'TransactionRepository': 'com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository',
    'ChargeRepository': 'com.sacco.sacco_system.modules.finance.domain.repository.ChargeRepository',
    'FinancialReportRepository': 'com.sacco.sacco_system.modules.finance.domain.repository.FinancialReportRepository',
    'AssetRepository': 'com.sacco.sacco_system.modules.admin.domain.repository.AssetRepository',
    'AuditLogRepository': 'com.sacco.sacco_system.modules.admin.domain.repository.AuditLogRepository',
    
    # For savings module
    'SavingsAccountDTO': 'com.sacco.sacco_system.modules.savings.api.dto.SavingsAccountDTO',
    'AccountingService': 'com.sacco.sacco_system.modules.finance.domain.service.AccountingService',
    'GuarantorRepository': 'com.sacco.sacco_system.modules.loan.domain.repository.GuarantorRepository',
    'LoanProductRepository': 'com.sacco.sacco_system.modules.loan.domain.repository.LoanProductRepository',
}

def fix_imports_in_file(file_path):
    """Fix imports in a single Java file"""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Remove wildcard imports from modules package
    content = re.sub(r'import com\.sacco\.sacco_system\.modules\.\*;?\n', '', content)
    
    # Fix incomplete imports and add missing imports
    missing_imports = []
    
    for class_name, full_import in IMPORT_MAPPINGS.items():
        # Check if class is used but not imported
        if re.search(rf'\b{class_name}\b', content):
            # Check if it's already imported (with different path)
            import_pattern = rf'import.*{class_name};'
            if not re.search(import_pattern, content):
                # Check if it's not imported at all or with wrong path
                if f'import {full_import}' not in content:
                    missing_imports.append(f'import {full_import};')
    
    # Add missing imports after existing imports
    if missing_imports:
        # Find the last import statement
        last_import_match = None
        for match in re.finditer(r'^import .*;$', content, re.MULTILINE):
            last_import_match = match
        
        if last_import_match:
            pos = last_import_match.end()
            # Add missing imports
            for imp in sorted(set(missing_imports)):
                content = content[:pos] + '\n' + imp + content[pos:]
                pos += len(imp) + 1
        else:
            # No imports exist, add after package declaration
            package_match = re.search(r'^package .*;$', content, re.MULTILINE)
            if package_match:
                pos = package_match.end()
                for imp in sorted(set(missing_imports)):
                    content = content[:pos] + '\n' + imp + content[pos:]
                    pos += len(imp) + 1
    
    # Write back
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)
    
    return len(missing_imports) > 0

# Process all Java files in modules
java_files = glob.glob(r'src/main/java/com/sacco/sacco_system/modules/**/*.java', recursive=True)

fixed_count = 0
for file_path in sorted(java_files):
    try:
        if fix_imports_in_file(file_path):
            fixed_count += 1
            print(f"Fixed imports: {file_path}")
    except Exception as e:
        print(f"Error processing {file_path}: {e}")

print(f"\nTotal files with import fixes: {fixed_count}")
