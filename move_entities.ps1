# Move entity files to their respective modules
# Admin module
Move-Item -Path "src/main/java/com/sacco/sacco_system/entity/Asset.java" -Destination "src/main/java/com/sacco/sacco_system/modules/admin/domain/entity/" -Force
Move-Item -Path "src/main/java/com/sacco/sacco_system/entity/AuditLog.java" -Destination "src/main/java/com/sacco/sacco_system/modules/admin/domain/entity/" -Force
Move-Item -Path "src/main/java/com/sacco/sacco_system/entity/SystemSetting.java" -Destination "src/main/java/com/sacco/sacco_system/modules/admin/domain/entity/" -Force

# Member module
Move-Item -Path "src/main/java/com/sacco/sacco_system/entity/Member.java" -Destination "src/main/java/com/sacco/sacco_system/modules/member/domain/entity/" -Force
Move-Item -Path "src/main/java/com/sacco/sacco_system/entity/Guarantor.java" -Destination "src/main/java/com/sacco/sacco_system/modules/member/domain/entity/" -Force

# Loan module
Move-Item -Path "src/main/java/com/sacco/sacco_system/entity/Loan.java" -Destination "src/main/java/com/sacco/sacco_system/modules/loan/domain/entity/" -Force
Move-Item -Path "src/main/java/com/sacco/sacco_system/entity/LoanProduct.java" -Destination "src/main/java/com/sacco/sacco_system/modules/loan/domain/entity/" -Force
Move-Item -Path "src/main/java/com/sacco/sacco_system/entity/LoanRepayment.java" -Destination "src/main/java/com/sacco/sacco_system/modules/loan/domain/entity/" -Force

# Savings module
Move-Item -Path "src/main/java/com/sacco/sacco_system/entity/SavingsAccount.java" -Destination "src/main/java/com/sacco/sacco_system/modules/savings/domain/entity/" -Force
Move-Item -Path "src/main/java/com/sacco/sacco_system/entity/SavingsProduct.java" -Destination "src/main/java/com/sacco/sacco_system/modules/savings/domain/entity/" -Force
Move-Item -Path "src/main/java/com/sacco/sacco_system/entity/Withdrawal.java" -Destination "src/main/java/com/sacco/sacco_system/modules/savings/domain/entity/" -Force

# Finance module
Move-Item -Path "src/main/java/com/sacco/sacco_system/entity/Charge.java" -Destination "src/main/java/com/sacco/sacco_system/modules/finance/domain/entity/" -Force
Move-Item -Path "src/main/java/com/sacco/sacco_system/entity/FinancialReport.java" -Destination "src/main/java/com/sacco/sacco_system/modules/finance/domain/entity/" -Force
Move-Item -Path "src/main/java/com/sacco/sacco_system/entity/ShareCapital.java" -Destination "src/main/java/com/sacco/sacco_system/modules/finance/domain/entity/" -Force
Move-Item -Path "src/main/java/com/sacco/sacco_system/entity/Transaction.java" -Destination "src/main/java/com/sacco/sacco_system/modules/finance/domain/entity/" -Force
Move-Item -Path "src/main/java/com/sacco/sacco_system/entity/accounting" -Destination "src/main/java/com/sacco/sacco_system/modules/finance/domain/entity/" -Force

# Notification module  
Move-Item -Path "src/main/java/com/sacco/sacco_system/entity/Notification.java" -Destination "src/main/java/com/sacco/sacco_system/modules/notification/domain/entity/" -Force

Write-Host "Entities moved successfully"
