# Fix cross-module imports
$crossModuleImports = @{
    'import com.sacco.sacco_system.modules.Member;' = 'import com.sacco.sacco_system.modules.member.domain.entity.Member;'
    'import com.sacco.sacco_system.modules.SavingsAccount;' = 'import com.sacco.sacco_system.modules.savings.domain.entity.SavingsAccount;'
    'import com.sacco.sacco_system.modules.Loan;' = 'import com.sacco.sacco_system.modules.loan.domain.entity.Loan;'
    'import com.sacco.sacco_system.modules.TransactionRepository;' = 'import com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository;'
    'import com.sacco.sacco_system.modules.TransactionService;' = 'import com.sacco.sacco_system.modules.finance.domain.service.TransactionService;'
    'import com.sacco.sacco_system.modules.SavingsService;' = 'import com.sacco.sacco_system.modules.savings.domain.service.SavingsService;'
}

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

Get-ChildItem -Path "src/main/java/com/sacco/sacco_system/modules" -Recurse -Filter "*.java" | ForEach-Object {
    Update-Imports -FilePath $_.FullName -ImportMap $crossModuleImports
}

Write-Host "Cross-module imports fixed!"
