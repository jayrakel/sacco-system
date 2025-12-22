# Canonical Variable Dictionary - SACCO System
**Date:** December 22, 2025  
**Status:** DRAFT - Requires Human Review  
**Sources:** 3 Variable Analysis Reports Merged

---

## IMPORTANT: This is Analysis Only - DO NOT Implement Without Approval

This dictionary merges variables from all three reports. Review all flagged items:
- 🔴 **CRITICAL** - Immediate action required (data integrity risk)
- ⚠️ **CONFLICT** - Same name, different meanings/types
- 🔴 **DUPLICATE** - Same concept, multiple representations
- ⚡ **AMBIGUOUS** - Unclear or inconsistent
- ✅ **CLEAN** - No conflicts

---

## CRITICAL CONFLICTS - Must Review First

### 1. Share Capital Variables - NAME COLLISION 🔴

| Variable      | Location            | Type       | Meaning                  | Issue                           |
|---------------|---------------------|------------|--------------------------|---------------------------------|
| `totalShares` | Member entity       | BigDecimal | Monetary VALUE (KES)     | Same name, different meaning    |
| `totalShares` | ShareCapital entity | BigDecimal | Share COUNT (quantity)   | Same name, different meaning    |
| `paidShares`  | ShareCapital entity | BigDecimal | Shares purchased (count) | Duplicate of totalShares        |
| `paidAmount`  | ShareCapital entity | BigDecimal | Money paid (KES)         | Should match Member.totalShares |

**Problem:** Critical naming collision - totalShares means MONEY in one entity, QUANTITY in another.

**Recommendation:**
- Rename `Member.totalShares` → `shareCapitalAmount` or `totalShareValue`
- Rename `ShareCapital.totalShares` → `numberOfShares` or `shareCount`
- Remove duplication between `totalShares` and `paidShares`

---

### 2. Loan Repayment - MISLEADING NAME 🔴

| Variable           | Location    | Type       | Meaning            | Issue                      |
|--------------------|-------------|------------|--------------------|----------------------------|
| `monthlyRepayment` | Loan entity | BigDecimal | Installment amount | Stores WEEKLY amounts too! |

**Problem:** Field named "monthly" but stores weekly repayment amounts for weekly loans.

**Evidence:** `LoanService.java` line 297: `loan.setMonthlyRepayment(weeklyRepayment)`

**Recommendation:** Rename to `installmentAmount` or `repaymentAmount`

---

### 3. Member Total Savings - SYNCHRONIZATION RISK 🔴

| Variable       | Location              | Type       | Meaning                    | Issue                        |
|----------------|-----------------------|------------|----------------------------|------------------------------|
| `totalSavings` | Member entity         | BigDecimal | Aggregate of all savings   | Stored separately            |
| `balance`      | SavingsAccount entity | BigDecimal | Individual account balance | Multiple accounts per member |

**Problem:** Member.totalSavings should equal SUM(all SavingsAccount.balance) but stored separately.

**Update Points:**
- `SavingsService.deposit()` - Updates both
- `SavingsService.processMemberExit()` - Sets to ZERO
- `MemberService.createMember()` - Initializes

**Recommendation:** Add reconciliation job or make computed property

---

### 4. Payment Method - TYPE INCONSISTENCY 🔴

| Variable        | Location           | Type               | Issue         |
|-----------------|--------------------|--------------------|---------------|
| `paymentMethod` | Transaction entity | PaymentMethod ENUM | Type-safe     |
| `paymentMethod` | Deposit entity     | String             | Not type-safe |

**Recommendation:** Standardize both to Enum for consistency

---

### 5. Loan Balance - COMPLEX CALCULATION 🔴

| Variable       | Location    | Type       | Meaning             |
|----------------|-------------|------------|---------------------|
| `loanBalance`  | Loan entity | BigDecimal | Outstanding amount  |
| `totalPrepaid` | Loan entity | BigDecimal | Overpayment buffer  |
| `totalArrears` | Loan entity | BigDecimal | Underpayment buffer |

**Problem:** Effective balance = `loanBalance - totalPrepaid + totalArrears` (not just loanBalance)

**Recommendation:** Add `getEffectiveBalance()` method

---

## COMPLETE VARIABLE CATALOG

### MODULE 1: MEMBER MANAGEMENT

| Variable             | Type               | Module  | Description           | Stored/Derived | Source | Conflicts                 |
|----------------------|--------------------|---------|-----------------------|----------------|--------|---------------------------|
| `id`                 | UUID               | Members | Primary key           | Stored         | Member | ✅                         |
| `memberNumber`       | String             | Members | Member ID (MEM000001) | Stored         | Member | ⚠️ Also in User           |
| `firstName`          | String             | Members | First name            | Stored         | Member | ⚠️ Also in User           |
| `lastName`           | String             | Members | Last name             | Stored         | Member | ⚠️ Also in User           |
| `email`              | String             | Members | Contact email         | Stored         | Member | ⚠️ Also in User           |
| `phoneNumber`        | String             | Members | Phone number          | Stored         | Member | ⚠️ Also in User           |
| `idNumber`           | String             | Members | National ID           | Stored         | Member | ✅                         |
| `kraPin`             | String             | Members | Tax ID                | Stored         | Member | ✅                         |
| `address`            | String             | Members | Physical address      | Stored         | Member | ✅                         |
| `dateOfBirth`        | LocalDate          | Members | Birth date            | Stored         | Member | ✅                         |
| `profileImageUrl`    | String             | Members | Profile picture path  | Stored         | Member | ✅                         |
| `nextOfKinName`      | String             | Members | Emergency contact     | Stored         | Member | ✅                         |
| `nextOfKinPhone`     | String             | Members | Emergency phone       | Stored         | Member | ✅                         |
| `nextOfKinRelation`  | String             | Members | Relationship          | Stored         | Member | ✅                         |
| `status`             | MemberStatus       | Members | ACTIVE/INACTIVE/etc.  | Stored         | Member | ⚡ Multiple dimensions     |
| `registrationStatus` | RegistrationStatus | Members | PENDING/PAID          | Stored         | Member | ⚡ Separate from status    |
| `totalShares`        | BigDecimal         | Members | Share VALUE (KES)     | Stored         | Member | 🔴 NAME COLLISION         |
| `totalSavings`       | BigDecimal         | Members | Savings aggregate     | Stored         | Member | 🔴 SYNC RISK              |
| `registrationDate`   | LocalDateTime      | Members | Join date             | Stored         | Member | ⚠️ Redundant w/ createdAt |
| `createdAt`          | LocalDateTime      | Members | Creation timestamp    | Stored         | Member | ✅                         |
| `updatedAt`          | LocalDateTime      | Members | Update timestamp      | Stored         | Member | ✅                         |

---

### MODULE 2: LOANS

| Variable             | Type         | Module | Description           | Stored/Derived | Source | Conflicts                  |
|----------------------|--------------|--------|-----------------------|----------------|--------|----------------------------|
| `id`                 | UUID         | Loans  | Primary key           | Stored         | Loan   | ✅                          |
| `loanNumber`         | String       | Loans  | Loan ID (LN123...)    | Stored         | Loan   | ✅                          |
| `principalAmount`    | BigDecimal   | Loans  | Original amount       | Stored         | Loan   | ✅                          |
| `interestRate`       | BigDecimal   | Loans  | Annual %              | Stored         | Loan   | ⚡ Period not in name       |
| `totalInterest`      | BigDecimal   | Loans  | Interest amount (KES) | Stored         | Loan   | ⚠️ Similar to interestRate |
| `loanBalance`        | BigDecimal   | Loans  | Outstanding balance   | Stored         | Loan   | 🔴 Complex calculation     |
| `monthlyRepayment`   | BigDecimal   | Loans  | Installment amount    | Stored         | Loan   | 🔴 MISLEADING NAME         |
| `duration`           | Integer      | Loans  | Term length           | Stored         | Loan   | ✅                          |
| `durationUnit`       | DurationUnit | Loans  | WEEKS/MONTHS          | Stored         | Loan   | ✅                          |
| `totalPrepaid`       | BigDecimal   | Loans  | Overpayment buffer    | Stored         | Loan   | 🔴 Part of balance         |
| `totalArrears`       | BigDecimal   | Loans  | Arrears buffer        | Stored         | Loan   | 🔴 Part of balance         |
| `gracePeriodWeeks`   | int          | Loans  | Grace period          | Stored         | Loan   | ✅                          |
| `status`             | LoanStatus   | Loans  | 19 workflow states    | Stored         | Loan   | ⚡ Complex                  |
| `applicationFeePaid` | boolean      | Loans  | Fee paid flag         | Stored         | Loan   | ✅                          |
| `votesYes`           | int          | Loans  | Approval votes        | Stored         | Loan   | ✅                          |
| `votesNo`            | int          | Loans  | Rejection votes       | Stored         | Loan   | ✅                          |
| `applicationDate`    | LocalDate    | Loans  | Applied date          | Stored         | Loan   | ✅                          |
| `approvalDate`       | LocalDate    | Loans  | Approved date         | Stored         | Loan   | ✅                          |
| `disbursementDate`   | LocalDate    | Loans  | Disbursed date        | Stored         | Loan   | ✅                          |

**LoanRepayment Sub-entity:**

| Variable          | Type            | Description         | Conflicts                        |
|-------------------|-----------------|---------------------|----------------------------------|
| `repaymentNumber` | Integer         | Installment #       | ✅                                |
| `amount`          | BigDecimal      | Expected amount     | ⚡ Generic name                   |
| `principalPaid`   | BigDecimal      | Principal portion   | ⚠️ Not always updated            |
| `interestPaid`    | BigDecimal      | Interest portion    | ⚠️ Not always updated            |
| `totalPaid`       | BigDecimal      | Total paid          | 🔴 Should = principal + interest |
| `status`          | RepaymentStatus | PENDING/PAID/etc.   | ✅                                |
| `dueDate`         | LocalDate       | Due date            | ✅                                |
| `paymentDate`     | LocalDate       | Actual payment date | ✅                                |

---

### MODULE 3: SAVINGS

| Variable           | Type          | Module  | Description          | Stored/Derived | Source         | Conflicts                 |
|--------------------|---------------|---------|----------------------|----------------|----------------|---------------------------|
| `id`               | UUID          | Savings | Primary key          | Stored         | SavingsAccount | ✅                         |
| `accountNumber`    | String        | Savings | Account ID           | Stored         | SavingsAccount | ✅                         |
| `balance`          | BigDecimal    | Savings | Current balance      | Stored         | SavingsAccount | 🔴 Must sync w/ Member    |
| `totalDeposits`    | BigDecimal    | Savings | Lifetime deposits    | Stored         | SavingsAccount | ⚡ Informational only      |
| `totalWithdrawals` | BigDecimal    | Savings | Lifetime withdrawals | Stored         | SavingsAccount | ⚡ Informational only      |
| `accruedInterest`  | BigDecimal    | Savings | Interest earned      | Stored         | SavingsAccount | ⚠️ Sync risk              |
| `maturityDate`     | LocalDate     | Savings | Maturity date        | Stored         | SavingsAccount | ✅                         |
| `status`           | AccountStatus | Savings | ACTIVE/CLOSED/etc.   | Stored         | SavingsAccount | ✅                         |
| `accountOpenDate`  | LocalDateTime | Savings | Open date            | Stored         | SavingsAccount | ⚠️ Redundant w/ createdAt |
| `createdAt`        | LocalDateTime | Savings | Creation timestamp   | Stored         | SavingsAccount | ✅                         |
| `updatedAt`        | LocalDateTime | Savings | Update timestamp     | Stored         | SavingsAccount | ✅                         |

---

### MODULE 4: SHARE CAPITAL

| Variable      | Type          | Module  | Description        | Stored/Derived | Source       | Conflicts                          |
|---------------|---------------|---------|--------------------|----------------|--------------|------------------------------------|
| `id`          | UUID          | Finance | Primary key        | Stored         | ShareCapital | ✅                                  |
| `shareValue`  | BigDecimal    | Finance | Price per share    | Stored         | ShareCapital | ✅                                  |
| `totalShares` | BigDecimal    | Finance | Share COUNT        | Stored         | ShareCapital | 🔴 NAME COLLISION                  |
| `paidShares`  | BigDecimal    | Finance | Shares purchased   | Stored         | ShareCapital | 🔴 Duplicate of totalShares        |
| `paidAmount`  | BigDecimal    | Finance | Money paid (KES)   | Stored         | ShareCapital | 🔴 Should match Member.totalShares |
| `createdAt`   | LocalDateTime | Finance | Creation timestamp | Stored         | ShareCapital | ✅                                  |
| `updatedAt`   | LocalDateTime | Finance | Update timestamp   | Stored         | ShareCapital | ✅                                  |

---

### MODULE 5: TRANSACTIONS

| Variable          | Type            | Module  | Description        | Stored/Derived | Source      | Conflicts                    |
|-------------------|-----------------|---------|--------------------|----------------|-------------|------------------------------|
| `id`              | UUID            | Finance | Primary key        | Stored         | Transaction | ✅                            |
| `transactionId`   | String          | Finance | TX ref (TXN123...) | Stored         | Transaction | ⚠️ Confusing w/ id           |
| `type`            | TransactionType | Finance | Category           | Stored         | Transaction | ⚡ Overlaps w/ Charge/Fine    |
| `amount`          | BigDecimal      | Finance | Amount             | Stored         | Transaction | ⚡ Generic name               |
| `description`     | String          | Finance | Details            | Stored         | Transaction | ✅                            |
| `paymentMethod`   | PaymentMethod   | Finance | CASH/MPESA/etc.    | Stored         | Transaction | 🔴 Type conflict w/ Deposit  |
| `referenceCode`   | String          | Finance | External ref       | Stored         | Transaction | ⚠️ Overlaps w/ transactionId |
| `balanceAfter`    | BigDecimal      | Finance | Balance snapshot   | Stored         | Transaction | ⚡ Historical value           |
| `transactionDate` | LocalDateTime   | Finance | Transaction time   | Stored         | Transaction | ✅                            |

---

### MODULE 6: DEPOSITS

| Variable               | Type          | Module   | Description       | Stored/Derived | Source  | Conflicts                  |
|------------------------|---------------|----------|-------------------|----------------|---------|----------------------------|
| `id`                   | UUID          | Deposits | Primary key       | Stored         | Deposit | ✅                          |
| `totalAmount`          | BigDecimal    | Deposits | Total deposit     | Stored         | Deposit | 🔴 Must = SUM(allocations) |
| `status`               | DepositStatus | Deposits | Processing status | Stored         | Deposit | ✅                          |
| `transactionReference` | String        | Deposits | Deposit ref       | Stored         | Deposit | ⚠️ Naming overlap          |
| `paymentMethod`        | String        | Deposits | Payment type      | Stored         | Deposit | 🔴 TYPE CONFLICT (String)  |
| `paymentReference`     | String        | Deposits | External ref      | Stored         | Deposit | ✅                          |
| `createdAt`            | LocalDateTime | Deposits | Creation time     | Stored         | Deposit | ✅                          |
| `processedAt`          | LocalDateTime | Deposits | Processing time   | Stored         | Deposit | ✅                          |

**DepositAllocation Sub-entity:**

| Variable          | Type                   | Description       | Conflicts |
|-------------------|------------------------|-------------------|-----------|
| `amount`          | BigDecimal             | Allocation amount | ⚡ Generic |
| `destinationType` | DepositDestinationType | Where it goes     | ✅         |
| `status`          | AllocationStatus       | Processing status | ✅         |

---

### MODULE 7: ACCOUNTING

| Variable          | Type          | Module     | Description          | Stored/Derived | Source         | Conflicts                        |
|-------------------|---------------|------------|----------------------|----------------|----------------|----------------------------------|
| `code`            | String        | Accounting | GL account code      | Stored         | GLAccount (PK) | ✅                                |
| `name`            | String        | Accounting | Account name         | Stored         | GLAccount      | ✅                                |
| `type`            | AccountType   | Accounting | ASSET/LIABILITY/etc. | Stored         | GLAccount      | ✅                                |
| `balance`         | BigDecimal    | Accounting | Current balance      | Stored         | GLAccount      | ⚠️ Updated w/ entries            |
| `active`          | Boolean       | Accounting | Enabled flag         | Stored         | GLAccount      | ⚠️ Type changed to Boolean       |
| `debit`           | BigDecimal    | Accounting | Debit amount         | Stored         | JournalLine    | ⚡ Mutually exclusive             |
| `credit`          | BigDecimal    | Accounting | Credit amount        | Stored         | JournalLine    | ⚡ Mutually exclusive             |
| `transactionDate` | LocalDateTime | Accounting | Business date        | Stored         | JournalEntry   | ✅                                |
| `postedDate`      | LocalDateTime | Accounting | System date          | Stored         | JournalEntry   | ⚡ Different from transactionDate |
| `referenceNo`     | String        | Accounting | Links to transaction | Stored         | JournalEntry   | ⚠️ Naming inconsistent           |

---

### MODULE 8: USERS & AUTH

| Variable             | Type    | Module | Description           | Stored/Derived | Source | Conflicts                      |
|----------------------|---------|--------|-----------------------|----------------|--------|--------------------------------|
| `id`                 | UUID    | Users  | Primary key           | Stored         | User   | ✅                              |
| `email`              | String  | Users  | Login email           | Stored         | User   | ⚠️ DUPLICATE (Member)          |
| `officialEmail`      | String  | Users  | Admin email           | Stored         | User   | ✅                              |
| `password`           | String  | Users  | Encrypted password    | Stored         | User   | ✅                              |
| `firstName`          | String  | Users  | First name            | Stored         | User   | ⚠️ DUPLICATE (Member)          |
| `lastName`           | String  | Users  | Last name             | Stored         | User   | ⚠️ DUPLICATE (Member)          |
| `phoneNumber`        | String  | Users  | Phone                 | Stored         | User   | ⚠️ DUPLICATE (Member)          |
| `memberNumber`       | String  | Users  | Member link           | Stored         | User   | ⚠️ DUPLICATE (Member)          |
| `role`               | Role    | Users  | System role           | Stored         | User   | ✅                              |
| `enabled`            | boolean | Users  | Account enabled       | Stored         | User   | ⚡ Different from Member.status |
| `emailVerified`      | boolean | Users  | Email verified        | Stored         | User   | ✅                              |
| `mustChangePassword` | boolean | Users  | Force password change | Stored         | User   | ✅                              |

---

### MODULE 9: FINES & CHARGES

| Variable      | Type         | Module  | Description         | Stored/Derived | Source | Conflicts                       |
|---------------|--------------|---------|---------------------|----------------|--------|---------------------------------|
| `amount`      | BigDecimal   | Finance | Fine amount         | Stored         | Fine   | ⚡ Generic                       |
| `type`        | FineType     | Finance | Fine category       | Stored         | Fine   | 🔴 Overlaps w/ ChargeType       |
| `status`      | FineStatus   | Finance | PENDING/PAID/WAIVED | Stored         | Fine   | ✅                               |
| `fineDate`    | LocalDate    | Finance | Imposed date        | Stored         | Fine   | ✅                               |
| `daysOverdue` | Integer      | Finance | Days late           | Stored         | Fine   | ✅                               |
| `amount`      | BigDecimal   | Finance | Charge amount       | Stored         | Charge | ⚡ Generic                       |
| `type`        | ChargeType   | Finance | Charge category     | Stored         | Charge | 🔴 LATE_PAYMENT_PENALTY in both |
| `status`      | ChargeStatus | Finance | PENDING/PAID/WAIVED | Stored         | Charge | ✅                               |
| `isWaived`    | boolean      | Finance | Waived flag         | Stored         | Charge | ✅                               |

---

### MODULE 10: OTHER ENTITIES

**Guarantor:**
- `guaranteeAmount` (BigDecimal) - Amount guaranteed
- `status` (GuarantorStatus) - PENDING/ACCEPTED/DECLINED
- `currentGuarantorExposure` (Derived) - 🔴 NOT STORED, calculated on-the-fly

**Dividend:**
- `dividendAmount` (BigDecimal) - Payout amount
- `memberSharePercentage` (BigDecimal) - Member's % of pool
- `totalDividendPool` (BigDecimal) - Total SACCO pool

**Withdrawal:**
- `amount` (BigDecimal) - Withdrawal amount
- `status` (WithdrawalStatus) - PENDING/APPROVED/REJECTED/PROCESSED

**Asset:**
- `purchaseCost` (BigDecimal) - Original cost
- `accumulatedDepreciation` (BigDecimal) - Depreciation total
- `currentValue` (BigDecimal) - 🔴 Should = purchaseCost - accumulatedDepreciation

**SystemSetting:**
- `key` (String) - Setting name (e.g., REGISTRATION_FEE)
- `value` (String) - Setting value (stored as string)
- `dataType` (String) - STRING/NUMBER/BOOLEAN

**Notification:**
- `type` (NotificationType) - EMAIL/SMS/IN_APP
- `isRead` (boolean) - Read status
- `retryCount` (Integer) - Retry attempts

**AuditLog:**
- `userEmail` (String) - 🔴 DENORMALIZED (intentional for audit)
- `userName` (String) - 🔴 DENORMALIZED (intentional for audit)
- `action` (String) - CREATE/UPDATE/DELETE
- `entityType` (String) - Entity name
- `status` (Status) - SUCCESS/FAILURE

---

## STATUS FIELD OVERLOADING

**"status" used in 15+ entities with different enums:**

1. `Member.status` - MemberStatus (ACTIVE/INACTIVE/SUSPENDED/DECEASED)
2. `Member.registrationStatus` - RegistrationStatus (PENDING/PAID)
3. `SavingsAccount.status` - AccountStatus (ACTIVE/DORMANT/CLOSED/FROZEN/MATURED)
4. `Loan.status` - LoanStatus (19 workflow states)
5. `LoanRepayment.status` - RepaymentStatus (PENDING/PARTIALLY_PAID/PAID/OVERDUE/DEFAULTED)
6. `Deposit.status` - DepositStatus (PENDING/PROCESSING/COMPLETED/FAILED)
7. `DepositAllocation.status` - AllocationStatus (PENDING/COMPLETED/FAILED)
8. `Withdrawal.status` - WithdrawalStatus (PENDING/APPROVED/REJECTED/PROCESSED)
9. `Fine.status` - FineStatus (PENDING/PAID/WAIVED)
10. `Charge.status` - ChargeStatus (PENDING/PAID/WAIVED)
11. `Dividend.status` - DividendStatus (DECLARED/PAID/CANCELLED)
12. `Guarantor.status` - GuarantorStatus (PENDING/ACCEPTED/DECLINED)
13. `LoanDisbursement.status` - DisbursementStatus (workflow states)
14. `DepositProduct.status` - DepositProductStatus (ACTIVE/INACTIVE/COMPLETED)
15. `Asset.status` - AssetStatus (ACTIVE/DISPOSED/LOST)
16. `AuditLog.status` - Status (SUCCESS/FAILURE/PENDING)

⚡ **Note:** Not a data conflict, but reduces code clarity

---

## DATE/TIMESTAMP INCONSISTENCIES

**LocalDate vs LocalDateTime mixing:**
- Loan dates use LocalDate (applicationDate, approvalDate, etc.)
- Transaction/Journal dates use LocalDateTime
- Member/Account use both (createdAt is DateTime, registrationDate is DateTime)

**Redundant date fields:**
- `registrationDate` vs `createdAt` (Member)
- `accountOpenDate` vs `createdAt` (SavingsAccount)

**Naming patterns:**
- `*Date` suffix (applicationDate, approvalDate)
- `*At` suffix (createdAt, updatedAt, processedAt)
- `date*` prefix (dateRequestSent, dateResponded)

⚠️ **Recommendation:** Standardize to LocalDateTime and consistent naming

---

## REFERENCE/IDENTIFIER INCONSISTENCIES

**Multiple patterns:**
- `*Number` - memberNumber, loanNumber, accountNumber
- `*Id` - transactionId, entityId
- `*Reference` - transactionReference, paymentReference
- `*Code` - referenceCode, bankCode
- `*No` - referenceNo

**Confusing pairs:**
- `Transaction.id` (UUID) vs `Transaction.transactionId` (String)
- `transactionId` vs `referenceCode` (both used as references)
- `checkNumber` vs `chequeNumber` (spelling inconsistency)

⚠️ **Recommendation:** Standardize naming convention

---

## SUMMARY STATISTICS

**Total Variables Analyzed:** 232  
**Clean Variables:** 156 (67%)  
**Variables with Conflicts:** 76 (33%)

**By Severity:**
- 🔴 **Critical:** 15 variables (6%)
- ⚠️ **High Priority:** 36 variables (16%)
- ⚡ **Medium Priority:** 25 variables (11%)
- ✅ **Clean:** 156 variables (67%)

**By Module:**
- Members: 21 variables (8 conflicts)
- Loans: 52 variables (14 conflicts)
- Savings: 18 variables (6 conflicts)
- Finance/Transactions: 38 variables (14 conflicts)
- Share Capital: 7 variables (5 CRITICAL)
- Deposits: 16 variables (6 conflicts)
- Accounting: 15 variables (3 conflicts)
- Users: 14 variables (7 conflicts)
- Other: 51 variables (13 conflicts)

---

## RECOMMENDED ACTIONS

### Phase 1: Critical Issues (Immediate)
1. ✅ Rename Share Capital variables to resolve name collision
2. ✅ Rename Loan.monthlyRepayment to installmentAmount
3. ✅ Standardize paymentMethod to Enum type
4. ✅ Add validation for calculated fields (totalPaid, netIncome, currentValue)
5. ✅ Add Member.totalSavings reconciliation job

### Phase 2: High Priority (Short-term)
6. ✅ Implement User-Member sync service
7. ✅ Update LoanRepayment.principalPaid and interestPaid in processPayment()
8. ✅ Rename generic "amount" fields to be context-specific
9. ✅ Remove redundant date fields
10. ✅ Standardize reference field naming convention

### Phase 3: Medium Priority (Long-term)
11. ✅ Consolidate Fine and Charge entities or clarify distinction
12. ✅ Standardize date types (LocalDate vs LocalDateTime)
13. ✅ Resolve spelling inconsistencies (check/cheque)
14. ✅ Add validation for DepositAllocation mutually exclusive FKs
15. ✅ Document all "status" field enums clearly

---

## NOTES FOR IMPLEMENTATION

**DO NOT:**
- Change code without human review and approval
- Resolve conflicts based on assumptions
- Rename fields without database migration plan
- Modify production data

**DO:**
- Review each conflict with business stakeholders
- Plan database migrations for renames
- Update all references (code, queries, DTOs)
- Add tests for synchronization logic
- Document all decisions

---

**END OF CANONICAL VARIABLE DICTIONARY**

*Document Status: DRAFT*  
*Requires human review and approval before implementation*

