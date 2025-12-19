# This script fixes all import statements to point to correct module locations

# Function to update a file's imports
function Update-Imports {
    param(
        [string]$FilePath,
        [hashtable]$ImportMap
    )
    
    if (Test-Path $FilePath) {
        $content = Get-Content $FilePath -Raw
        foreach ($old in $ImportMap.Keys) {
            $new = $ImportMap[$old]
            $content = $content -replace [regex]::Escape($old), $new
        }
        Set-Content $FilePath -Value $content -Encoding UTF8
    }
}

# Create import mappings
$adminImports = @{
    'import com.sacco.sacco_system.modules.Asset;' = 'import com.sacco.sacco_system.modules.admin.domain.entity.Asset;'
    'import com.sacco.sacco_system.modules.AssetRepository;' = 'import com.sacco.sacco_system.modules.admin.domain.repository.AssetRepository;'
    'import com.sacco.sacco_system.modules.AssetService;' = 'import com.sacco.sacco_system.modules.admin.domain.service.AssetService;'
    'import com.sacco.sacco_system.modules.AuditLog;' = 'import com.sacco.sacco_system.modules.admin.domain.entity.AuditLog;'
    'import com.sacco.sacco_system.modules.AuditLogRepository;' = 'import com.sacco.sacco_system.modules.admin.domain.repository.AuditLogRepository;'
    'import com.sacco.sacco_system.modules.AuditService;' = 'import com.sacco.sacco_system.modules.admin.domain.service.AuditService;'
    'import com.sacco.sacco_system.modules.SystemSetting;' = 'import com.sacco.sacco_system.modules.admin.domain.entity.SystemSetting;'
    'import com.sacco.sacco_system.modules.SystemSettingRepository;' = 'import com.sacco.sacco_system.modules.admin.domain.repository.SystemSettingRepository;'
    'import com.sacco.sacco_system.modules.SystemSettingService;' = 'import com.sacco.sacco_system.modules.admin.domain.service.SystemSettingService;'
}

$memberImports = @{
    'import com.sacco.sacco_system.modules.Member;' = 'import com.sacco.sacco_system.modules.member.domain.entity.Member;'
    'import com.sacco.sacco_system.modules.Guarantor;' = 'import com.sacco.sacco_system.modules.member.domain.entity.Guarantor;'
    'import com.sacco.sacco_system.modules.MemberRepository;' = 'import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;'
    'import com.sacco.sacco_system.modules.MemberService;' = 'import com.sacco.sacco_system.modules.member.domain.service.MemberService;'
}

$loanImports = @{
    'import com.sacco.sacco_system.modules.Loan;' = 'import com.sacco.sacco_system.modules.loan.domain.entity.Loan;'
    'import com.sacco.sacco_system.modules.LoanProduct;' = 'import com.sacco.sacco_system.modules.loan.domain.entity.LoanProduct;'
    'import com.sacco.sacco_system.modules.LoanRepayment;' = 'import com.sacco.sacco_system.modules.loan.domain.entity.LoanRepayment;'
    'import com.sacco.sacco_system.modules.LoanRepository;' = 'import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;'
    'import com.sacco.sacco_system.modules.LoanService;' = 'import com.sacco.sacco_system.modules.loan.domain.service.LoanService;'
}

$savingsImports = @{
    'import com.sacco.sacco_system.modules.SavingsAccount;' = 'import com.sacco.sacco_system.modules.savings.domain.entity.SavingsAccount;'
    'import com.sacco.sacco_system.modules.SavingsProduct;' = 'import com.sacco.sacco_system.modules.savings.domain.entity.SavingsProduct;'
    'import com.sacco.sacco_system.modules.Withdrawal;' = 'import com.sacco.sacco_system.modules.savings.domain.entity.Withdrawal;'
    'import com.sacco.sacco_system.modules.SavingsRepository;' = 'import com.sacco.sacco_system.modules.savings.domain.repository.SavingsRepository;'
    'import com.sacco.sacco_system.modules.SavingsService;' = 'import com.sacco.sacco_system.modules.savings.domain.service.SavingsService;'
}

$financeImports = @{
    'import com.sacco.sacco_system.modules.Charge;' = 'import com.sacco.sacco_system.modules.finance.domain.entity.Charge;'
    'import com.sacco.sacco_system.modules.FinancialReport;' = 'import com.sacco.sacco_system.modules.finance.domain.entity.FinancialReport;'
    'import com.sacco.sacco_system.modules.ShareCapital;' = 'import com.sacco.sacco_system.modules.finance.domain.entity.ShareCapital;'
    'import com.sacco.sacco_system.modules.Transaction;' = 'import com.sacco.sacco_system.modules.finance.domain.entity.Transaction;'
    'import com.sacco.sacco_system.modules.FinancialReportRepository;' = 'import com.sacco.sacco_system.modules.finance.domain.repository.FinancialReportRepository;'
    'import com.sacco.sacco_system.modules.TransactionRepository;' = 'import com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository;'
    'import com.sacco.sacco_system.modules.ChargeRepository;' = 'import com.sacco.sacco_system.modules.finance.domain.repository.ChargeRepository;'
    'import com.sacco.sacco_system.modules.FinancialReportService;' = 'import com.sacco.sacco_system.modules.finance.domain.service.FinancialReportService;'
    'import com.sacco.sacco_system.modules.TransactionService;' = 'import com.sacco.sacco_system.modules.finance.domain.service.TransactionService;'
}

# Fix admin files
Get-ChildItem -Path "src/main/java/com/sacco/sacco_system/modules/admin" -Recurse -Filter "*.java" | ForEach-Object {
    Update-Imports -FilePath $_.FullName -ImportMap $adminImports
}

# Fix member files
Get-ChildItem -Path "src/main/java/com/sacco/sacco_system/modules/member" -Recurse -Filter "*.java" | ForEach-Object {
    Update-Imports -FilePath $_.FullName -ImportMap $memberImports
}

# Fix loan files
Get-ChildItem -Path "src/main/java/com/sacco/sacco_system/modules/loan" -Recurse -Filter "*.java" | ForEach-Object {
    Update-Imports -FilePath $_.FullName -ImportMap $loanImports
}

# Fix savings files
Get-ChildItem -Path "src/main/java/com/sacco/sacco_system/modules/savings" -Recurse -Filter "*.java" | ForEach-Object {
    Update-Imports -FilePath $_.FullName -ImportMap $savingsImports
}

# Fix finance files
Get-ChildItem -Path "src/main/java/com/sacco/sacco_system/modules/finance" -Recurse -Filter "*.java" | ForEach-Object {
    Update-Imports -FilePath $_.FullName -ImportMap $financeImports
}

Write-Host "All imports have been fixed!"
