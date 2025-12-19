import glob

# Map entity files to their correct packages
entity_packages = {
    'src/main/java/com/sacco/sacco_system/modules/admin/domain/entity/Asset.java': 'com.sacco.sacco_system.modules.admin.domain.entity',
    'src/main/java/com/sacco/sacco_system/modules/admin/domain/entity/AuditLog.java': 'com.sacco.sacco_system.modules.admin.domain.entity',
    'src/main/java/com/sacco/sacco_system/modules/admin/domain/entity/SystemSetting.java': 'com.sacco.sacco_system.modules.admin.domain.entity',
    'src/main/java/com/sacco/sacco_system/modules/finance/domain/entity/Charge.java': 'com.sacco.sacco_system.modules.finance.domain.entity',
    'src/main/java/com/sacco/sacco_system/modules/finance/domain/entity/FinancialReport.java': 'com.sacco.sacco_system.modules.finance.domain.entity',
    'src/main/java/com/sacco/sacco_system/modules/finance/domain/entity/ShareCapital.java': 'com.sacco.sacco_system.modules.finance.domain.entity',
    'src/main/java/com/sacco/sacco_system/modules/finance/domain/entity/Transaction.java': 'com.sacco.sacco_system.modules.finance.domain.entity',
    'src/main/java/com/sacco/sacco_system/modules/finance/domain/entity/accounting/AccountType.java': 'com.sacco.sacco_system.modules.finance.domain.entity.accounting',
    'src/main/java/com/sacco/sacco_system/modules/finance/domain/entity/accounting/FiscalPeriod.java': 'com.sacco.sacco_system.modules.finance.domain.entity.accounting',
    'src/main/java/com/sacco/sacco_system/modules/finance/domain/entity/accounting/GLAccount.java': 'com.sacco.sacco_system.modules.finance.domain.entity.accounting',
    'src/main/java/com/sacco/sacco_system/modules/finance/domain/entity/accounting/GlMapping.java': 'com.sacco.sacco_system.modules.finance.domain.entity.accounting',
    'src/main/java/com/sacco/sacco_system/modules/finance/domain/entity/accounting/JournalEntry.java': 'com.sacco.sacco_system.modules.finance.domain.entity.accounting',
    'src/main/java/com/sacco/sacco_system/modules/finance/domain/entity/accounting/JournalLine.java': 'com.sacco.sacco_system.modules.finance.domain.entity.accounting',
    'src/main/java/com/sacco/sacco_system/modules/loan/domain/entity/Guarantor.java': 'com.sacco.sacco_system.modules.loan.domain.entity',
    'src/main/java/com/sacco/sacco_system/modules/loan/domain/entity/Loan.java': 'com.sacco.sacco_system.modules.loan.domain.entity',
    'src/main/java/com/sacco/sacco_system/modules/loan/domain/entity/LoanProduct.java': 'com.sacco.sacco_system.modules.loan.domain.entity',
    'src/main/java/com/sacco/sacco_system/modules/loan/domain/entity/LoanRepayment.java': 'com.sacco.sacco_system.modules.loan.domain.entity',
    'src/main/java/com/sacco/sacco_system/modules/member/domain/entity/Guarantor.java': 'com.sacco.sacco_system.modules.member.domain.entity',
    'src/main/java/com/sacco/sacco_system/modules/member/domain/entity/Member.java': 'com.sacco.sacco_system.modules.member.domain.entity',
    'src/main/java/com/sacco/sacco_system/modules/member/domain/entity/MemberStatus.java': 'com.sacco.sacco_system.modules.member.domain.entity',
    'src/main/java/com/sacco/sacco_system/modules/notification/domain/entity/Notification.java': 'com.sacco.sacco_system.modules.notification.domain.entity',
    'src/main/java/com/sacco/sacco_system/modules/savings/domain/entity/SavingsAccount.java': 'com.sacco.sacco_system.modules.savings.domain.entity',
    'src/main/java/com/sacco/sacco_system/modules/savings/domain/entity/SavingsProduct.java': 'com.sacco.sacco_system.modules.savings.domain.entity',
    'src/main/java/com/sacco/sacco_system/modules/savings/domain/entity/Withdrawal.java': 'com.sacco.sacco_system.modules.savings.domain.entity',
}

fixed_count = 0
for file_path, correct_package in entity_packages.items():
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Find the old package declaration
        lines = content.split('\n')
        if lines[0].startswith('package '):
            old_package = lines[0]
            new_first_line = f'package {correct_package};'
            content = content.replace(old_package, new_first_line, 1)
            
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            fixed_count += 1
            print(f"Fixed: {file_path}")
    except Exception as e:
        print(f"Error processing {file_path}: {e}")

print(f"\nTotal entity files fixed: {fixed_count}")
