# Fix accounting module imports (they're in finance/domain/entity/accounting)
$accountingImports = @{
    'import com.sacco.sacco_system.modules.accounting.GLAccount;' = 'import com.sacco.sacco_system.modules.finance.domain.entity.accounting.GLAccount;'
    'import com.sacco.sacco_system.modules.accounting.JournalEntry;' = 'import com.sacco.sacco_system.modules.finance.domain.entity.accounting.JournalEntry;'
    'import com.sacco.sacco_system.modules.accounting.JournalLine;' = 'import com.sacco.sacco_system.modules.finance.domain.entity.accounting.JournalLine;'
    'import com.sacco.sacco_system.modules.accounting.FiscalPeriod;' = 'import com.sacco.sacco_system.modules.finance.domain.entity.accounting.FiscalPeriod;'
    'import com.sacco.sacco_system.modules.accounting.GlMapping;' = 'import com.sacco.sacco_system.modules.finance.domain.entity.accounting.GlMapping;'
    'import com.sacco.sacco_system.modules.accounting.GLAccountRepository;' = 'import com.sacco.sacco_system.modules.finance.domain.repository.GLAccountRepository;'
    'import com.sacco.sacco_system.modules.accounting.JournalEntryRepository;' = 'import com.sacco.sacco_system.modules.finance.domain.repository.JournalEntryRepository;'
    'import com.sacco.sacco_system.modules.accounting.FiscalPeriodRepository;' = 'import com.sacco.sacco_system.modules.finance.domain.repository.FiscalPeriodRepository;'
    'import com.sacco.sacco_system.modules.accounting.GlMappingRepository;' = 'import com.sacco.sacco_system.modules.finance.domain.repository.GlMappingRepository;'
    'import com.sacco.sacco_system.modules.accounting.AccountingService;' = 'import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;'
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

Get-ChildItem -Path "src/main/java/com/sacco/sacco_system/modules/finance" -Recurse -Filter "*.java" | ForEach-Object {
    Update-Imports -FilePath $_.FullName -ImportMap $accountingImports
}

Write-Host "Accounting imports fixed!"
