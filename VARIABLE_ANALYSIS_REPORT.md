# Variable Analysis Report - SACCO System
**Generated on:** December 22, 2025  
**Analysis Type:** READ-ONLY - No code changes made  
**Scope:** Complete codebase review (Backend Java + Frontend React)

---

## Executive Summary

This report identifies all variables and field declarations across the SACCO system, grouping them by business concepts and highlighting potential conflicts, duplications, and naming inconsistencies.

**Key Findings:**
- 162 Java source files analyzed
- 50+ React/JSX frontend files analyzed
- Multiple instances of duplicate/overlapping business concepts
- Some naming inconsistencies between entities and DTOs
- Several fields representing the same business concept with different names

---

## 1. MEMBER MANAGEMENT MODULE

### 1.1 Member Entity (`Member.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/member/domain/entity/Member.java`

| Variable Name | Type | Business Meaning | Notes |
|--------------|------|------------------|-------|
| `id` | UUID | Unique member identifier | Primary key |
| `profileImageUrl` | String | Profile picture path | File reference |
| `memberNumber` | String | Human-readable member ID | Format: MEM000001 |
| `firstName` | String | Member's first name | Required field |
| `lastName` | String | Member's last name | Required field |
| `email` | String | Contact email | Unique, required |
| `phoneNumber` | String | Contact phone | Unique, required |
| `idNumber` | String | National ID number | Unique, required |
| `kraPin` | String | Tax identification | Optional |
| `address` | String | Physical address | Optional |
| `dateOfBirth` | LocalDate | Birth date | Optional |
| `nextOfKinName` | String | Emergency contact name | Optional |
| `nextOfKinPhone` | String | Emergency contact phone | Optional |
| `nextOfKinRelation` | String | Relationship to NOK | Optional |
| `status` | MemberStatus | Account status | ACTIVE/INACTIVE/SUSPENDED/DECEASED |
| `registrationStatus` | RegistrationStatus | Registration payment status | PENDING/PAID |
| `totalShares` | BigDecimal | Total share capital value | **DUPLICATE - See ShareCapital entity** |
| `totalSavings` | BigDecimal | Total savings balance | **DUPLICATE - See SavingsAccount entity** |
| `user` | User | Linked login account | OneToOne relationship |
| `savingsAccounts` | List<SavingsAccount> | Member's savings accounts | OneToMany relationship |
| `loans` | List<Loan> | Member's loans | OneToMany relationship |
| `transactions` | List<Transaction> | Member's transactions | OneToMany relationship |
| `registrationDate` | LocalDateTime | When member joined | **NOTE: Was previously 'joinDate'** |
| `createdAt` | LocalDateTime | Record creation timestamp | Audit field |
| `updatedAt` | LocalDateTime | Last update timestamp | Audit field |

**Potential Conflicts:**
1. ⚠️ **DUPLICATION**: `totalShares` in Member vs `paidAmount`/`totalShares` in ShareCapital entity
2. ⚠️ **DUPLICATION**: `totalSavings` in Member vs calculated balance in SavingsAccount
3. ⚠️ **NAMING**: `registrationDate` was previously called `joinDate` - inconsistency in codebase history

### 1.2 User Entity (`User.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/auth/model/User.java`

| Variable Name | Type | Business Meaning | Notes |
|--------------|------|------------------|-------|
| `id` | UUID | Unique user identifier | Primary key |
| `email` | String | Login email | **DUPLICATE with Member.email** |
| `officialEmail` | String | SACCO admin email | For admin/official roles |
| `password` | String | Encrypted password | Security field |
| `firstName` | String | User's first name | **DUPLICATE with Member.firstName** |
| `lastName` | String | User's last name | **DUPLICATE with Member.lastName** |
| `phoneNumber` | String | Contact phone | **DUPLICATE with Member.phoneNumber** |
| `memberNumber` | String | Link to member entity | **DUPLICATE with Member.memberNumber** |
| `role` | Role | System role | MEMBER/ADMIN/LOAN_OFFICER/etc. |
| `enabled` | boolean | Account enabled status | Security flag |
| `emailVerified` | boolean | Email verification status | Security flag |
| `mustChangePassword` | boolean | Force password change | Security flag |
| `createdAt` | LocalDateTime | Account creation | Audit field |
| `updatedAt` | LocalDateTime | Last update | Audit field |

**Potential Conflicts:**
1. ⚠️ **MAJOR DUPLICATION**: User and Member entities share: `email`, `firstName`, `lastName`, `phoneNumber`, `memberNumber`
2. **REASON**: User is for authentication, Member is for business logic. This is intentional but creates sync challenges.

---

## 2. FINANCIAL MANAGEMENT MODULE

### 2.1 Transaction Entity (`Transaction.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/finance/domain/entity/Transaction.java`

| Variable Name | Type | Business Meaning | Notes |
|--------------|------|------------------|-------|
| `id` | UUID | Unique transaction ID | Primary key |
| `transactionId` | String | Human-readable transaction ref | Format: TXN123456789 |
| `member` | Member | Member who made transaction | ManyToOne |
| `savingsAccount` | SavingsAccount | Related savings account | Optional |
| `loan` | Loan | Related loan | Optional |
| `type` | TransactionType | Transaction category | DEPOSIT/WITHDRAWAL/LOAN_REPAYMENT/etc. |
| `amount` | BigDecimal | Transaction amount | Always positive |
| `description` | String | Transaction details | Free text |
| `paymentMethod` | PaymentMethod | How payment was made | CASH/MPESA/BANK_TRANSFER/etc. |
| `referenceCode` | String | External reference | M-Pesa code, Check number, etc. |
| `balanceAfter` | BigDecimal | Account balance after transaction | Snapshot |
| `transactionDate` | LocalDateTime | When transaction occurred | Auto-generated |

**Potential Conflicts:**
1. ⚠️ **NAMING**: `transactionId` (string) vs `id` (UUID) - confusing naming
2. ⚠️ **AMBIGUITY**: `referenceCode` vs `transactionId` - both used as references
3. ⚠️ **OPTIONAL LINKS**: Can link to `savingsAccount` OR `loan` OR neither - requires validation

### 2.2 ShareCapital Entity (`ShareCapital.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/finance/domain/entity/ShareCapital.java`

| Variable Name | Type | Business Meaning | Notes |
|--------------|------|------------------|-------|
| `id` | UUID | Unique record ID | Primary key |
| `member` | Member | Share owner | ManyToOne |
| `shareValue` | BigDecimal | Price per share | Default: 100 |
| `totalShares` | BigDecimal | Number of shares owned | **DUPLICATE with paidShares** |
| `paidShares` | BigDecimal | Shares actually purchased | **DUPLICATE with totalShares** |
| `paidAmount` | BigDecimal | Total money paid for shares | **DUPLICATE with Member.totalShares** |
| `createdAt` | LocalDateTime | Record creation | Audit field |
| `updatedAt` | LocalDateTime | Last update | Audit field |

**Potential Conflicts:**
1. ⚠️ **CRITICAL DUPLICATION**: `totalShares` vs `paidShares` - both represent same value
2. ⚠️ **SYNC ISSUE**: `paidAmount` must match `Member.totalShares` but stored in two places
3. ⚠️ **CONFUSION**: `shareValue` * `totalShares` should equal `paidAmount` - validation needed

### 2.3 Fine Entity (`Fine.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/finance/domain/entity/Fine.java`

| Variable Name | Type | Business Meaning | Notes |
|--------------|------|------------------|-------|
| `id` | UUID | Unique fine ID | Primary key |
| `member` | Member | Member fined | ManyToOne |
| `loan` | Loan | Related loan (optional) | For loan penalties |
| `type` | FineType | Fine category | LATE_LOAN_PAYMENT/MISSED_MEETING/etc. |
| `amount` | BigDecimal | Fine amount | Penalty value |
| `description` | String | Fine reason | Free text |
| `fineDate` | LocalDate | When fine imposed | Date field |
| `status` | FineStatus | Payment status | PENDING/PAID/WAIVED |
| `paymentDate` | LocalDate | When fine paid | Optional |
| `paymentReference` | String | Payment ref code | Optional |
| `daysOverdue` | Integer | Days late (for loan fines) | Optional |
| `createdAt` | LocalDateTime | Record creation | Audit field |
| `updatedAt` | LocalDateTime | Last update | Audit field |

**Potential Conflicts:**
1. ⚠️ **OVERLAP**: Fine vs Charge entities both handle penalties
2. **NOTE**: Fines are penalties, Charges are fees - semantically different but overlapping

### 2.4 Charge Entity (`Charge.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/finance/domain/entity/Charge.java`

| Variable Name | Type | Business Meaning | Notes |
|--------------|------|------------------|-------|
| `id` | UUID | Unique charge ID | Primary key |
| `member` | Member | Member charged | ManyToOne |
| `loan` | Loan | Related loan (optional) | For loan fees |
| `type` | ChargeType | Charge category | PROCESSING_FEE/LATE_PAYMENT_PENALTY/etc. |
| `amount` | BigDecimal | Charge amount | Fee value |
| `description` | String | Charge reason | Free text |
| `status` | ChargeStatus | Payment status | PENDING/PAID/WAIVED |
| `createdAt` | LocalDateTime | Record creation | Audit field |
| `isWaived` | boolean | If charge waived | Flag |
| `waiverReason` | String | Why waived | Optional |

**Potential Conflicts:**
1. ⚠️ **SEMANTIC OVERLAP**: `Charge` (fees) vs `Fine` (penalties) - both can be LATE_PAYMENT_PENALTY
2. ⚠️ **DUPLICATION**: Both entities have `member`, `loan`, `amount`, `status` fields

### 2.5 Dividend Entity (`Dividend.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/finance/domain/entity/Dividend.java`

| Variable Name | Type | Business Meaning | Notes |
|--------------|------|------------------|-------|
| `id` | UUID | Unique dividend ID | Primary key |
| `member` | Member | Dividend recipient | ManyToOne |
| `fiscalYear` | Integer | Year of dividend | Business year |
| `declarationDate` | LocalDate | When declared | Date field |
| `totalDividendPool` | BigDecimal | Total pool to distribute | SACCO-wide total |
| `memberSharePercentage` | BigDecimal | Member's % of pool | Calculated value |
| `dividendAmount` | BigDecimal | Amount for this member | Actual payout |
| `status` | DividendStatus | Payment status | DECLARED/PAID/CANCELLED |
| `paymentDate` | LocalDate | When paid | Optional |
| `paymentReference` | String | Payment ref | Optional |
| `notes` | String | Additional info | Free text |
| `createdAt` | LocalDateTime | Record creation | Audit field |
| `updatedAt` | LocalDateTime | Last update | Audit field |

**No conflicts identified** - Well-defined entity

---

## 3. LOAN MANAGEMENT MODULE

### 3.1 Loan Entity (`Loan.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/loan/domain/entity/Loan.java`

| Variable Name | Type | Business Meaning | Notes |
|--------------|------|------------------|-------|
| `id` | UUID | Unique loan ID | Primary key |
| `loanNumber` | String | Human-readable loan ref | Format: LN123456789 |
| `member` | Member | Loan applicant | ManyToOne |
| `product` | LoanProduct | Loan product type | ManyToOne |
| `principalAmount` | BigDecimal | Original loan amount | Borrowed amount |
| `interestRate` | BigDecimal | Annual interest % | Product rate |
| `totalInterest` | BigDecimal | Total interest to pay | Calculated |
| `loanBalance` | BigDecimal | Outstanding balance | **KEY: Current debt** |
| `monthlyRepayment` | BigDecimal | Installment amount | **NAMING CONFLICT: Actually weekly repayment** |
| `duration` | Integer | Loan term length | Number of periods |
| `durationUnit` | DurationUnit | Period type | WEEKS/MONTHS |
| `totalPrepaid` | BigDecimal | Overpayment buffer | Prepayments |
| `totalArrears` | BigDecimal | Underpayment buffer | Arrears |
| `gracePeriodWeeks` | int | Grace period | Before repayment starts |
| `status` | LoanStatus | Workflow status | DRAFT/PENDING/APPROVED/ACTIVE/etc. |
| `applicationFeePaid` | boolean | Fee payment flag | Boolean |
| `meetingDate` | LocalDate | Voting meeting date | Approval process |
| `votingOpen` | boolean | Voting in progress | Flag |
| `votesYes` | int | Approval votes | Count |
| `votesNo` | int | Rejection votes | Count |
| `secretaryComments` | String | Secretary notes | Free text |
| `rejectionReason` | String | Why rejected | Free text |
| `checkNumber` | String | Treasurer's check ref | Disbursement ref |
| `applicationDate` | LocalDate | When applied | Date |
| `submissionDate` | LocalDate | When submitted | Date |
| `approvalDate` | LocalDate | When approved | Date |
| `disbursementDate` | LocalDate | When disbursed | Date |
| `expectedRepaymentDate` | LocalDate | When repayment starts | Date |
| `repayments` | List<LoanRepayment> | Repayment schedule | OneToMany |
| `guarantors` | List<Guarantor> | Loan guarantors | OneToMany |
| `votedUserIds` | List<UUID> | Users who voted | ElementCollection |

**Potential Conflicts:**
1. ⚠️ **CRITICAL NAMING ERROR**: `monthlyRepayment` stores WEEKLY repayment amount (see LoanService line 297)
2. ⚠️ **AMBIGUITY**: `loanBalance` vs calculated outstanding from repayments
3. ⚠️ **COMPLEX STATUS**: 19 different loan statuses - potential state management issues

### 3.2 LoanProduct Entity (`LoanProduct.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/loan/domain/entity/LoanProduct.java`

| Variable Name | Type | Business Meaning | Notes |
|--------------|------|------------------|-------|
| `id` | UUID | Product ID | Primary key |
| `name` | String | Product name | Unique, required |
| `description` | String | Product details | Free text |
| `interestRate` | BigDecimal | Annual % rate | Required |
| `interestType` | InterestType | Rate calculation method | FLAT_RATE/REDUCING_BALANCE |
| `maxTenureMonths` | Integer | Maximum term | In months |
| `maxLimit` | BigDecimal | Maximum loan amount | Upper limit |
| `processingFee` | BigDecimal | Application fee | Flat fee |
| `penaltyRate` | BigDecimal | Late payment % | Penalty rate |

**No conflicts identified** - Clean entity

### 3.3 LoanRepayment Entity (`LoanRepayment.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/loan/domain/entity/LoanRepayment.java`

| Variable Name | Type | Business Meaning | Notes |
|--------------|------|------------------|-------|
| `id` | UUID | Repayment ID | Primary key |
| `loan` | Loan | Parent loan | ManyToOne |
| `repaymentNumber` | Integer | Installment number | Sequence |
| `amount` | BigDecimal | Expected installment amount | **Scheduled amount** |
| `principalPaid` | BigDecimal | Principal portion paid | Actual payment |
| `interestPaid` | BigDecimal | Interest portion paid | Actual payment |
| `totalPaid` | BigDecimal | Total amount paid | **Sum of principal + interest** |
| `status` | RepaymentStatus | Payment status | PENDING/PAID/OVERDUE/etc. |
| `dueDate` | LocalDate | When payment due | Schedule date |
| `paymentDate` | LocalDate | When actually paid | Actual date |
| `createdAt` | LocalDateTime | Record creation | Audit |
| `updatedAt` | LocalDateTime | Last update | Audit |

**Potential Conflicts:**
1. ⚠️ **CALCULATED FIELD**: `totalPaid` should equal `principalPaid + interestPaid` - validation needed
2. **NOTE**: Status renamed from PARTIAL to PARTIALLY_PAID for consistency

### 3.4 LoanDisbursement Entity (`LoanDisbursement.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/loan/domain/entity/LoanDisbursement.java`

| Variable Name | Type | Business Meaning | Notes |
|--------------|------|------------------|-------|
| `id` | UUID | Disbursement ID | Primary key |
| `loan` | Loan | Associated loan | OneToOne |
| `member` | Member | Loan recipient | ManyToOne |
| `disbursementNumber` | String | Disbursement ref | Format: DISB-2024-001 |
| `amount` | BigDecimal | Amount disbursed | Should match loan principal |
| `method` | DisbursementMethod | How disbursed | CHEQUE/BANK_TRANSFER/MPESA/CASH/etc. |
| `status` | DisbursementStatus | Disbursement workflow | PENDING/PREPARED/APPROVED/DISBURSED/etc. |
| `chequeNumber` | String | Check number | For CHEQUE method |
| `bankName` | String | Bank name | For CHEQUE method |
| `chequeDate` | LocalDate | Check date | For CHEQUE method |
| `payableTo` | String | Payee name | For CHEQUE method |
| `accountNumber` | String | Bank account | For BANK_TRANSFER |
| `accountName` | String | Account holder | For BANK_TRANSFER |
| `bankCode` | String | Bank code | For BANK_TRANSFER |
| `transactionReference` | String | Transfer ref | For BANK_TRANSFER |
| `mpesaPhoneNumber` | String | M-Pesa phone | For MPESA method |
| `mpesaTransactionId` | String | M-Pesa ref | For MPESA method |
| `receivedBy` | String | Cash recipient | For CASH method |
| `witnessedBy` | String | Staff witness | For CASH method |
| `preparedBy` | UUID | User who prepared | Workflow |
| `preparedAt` | LocalDateTime | When prepared | Workflow |
| `approvedBy` | UUID | User who approved | Workflow |
| `approvedAt` | LocalDateTime | When approved | Workflow |
| `disbursedBy` | UUID | User who disbursed | Workflow |
| `disbursedAt` | LocalDateTime | When disbursed | Workflow |
| `notes` | String | Additional notes | Free text |

**Potential Conflicts:**
1. ⚠️ **CONDITIONAL FIELDS**: Many fields only apply to specific `method` values - validation needed
2. ⚠️ **AMBIGUITY**: `transactionReference` (for bank transfers) vs `disbursementNumber` (universal ref)

### 3.5 Guarantor Entity (`Guarantor.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/loan/domain/entity/Guarantor.java`

| Variable Name | Type | Business Meaning | Notes |
|--------------|------|------------------|-------|
| `id` | UUID | Guarantor record ID | Primary key |
| `loan` | Loan | Loan being guaranteed | ManyToOne |
| `member` | Member | Member guaranteeing | ManyToOne |
| `guaranteeAmount` | BigDecimal | Amount guaranteed | Portion of loan |
| `status` | GuarantorStatus | Response status | PENDING/ACCEPTED/DECLINED |
| `dateRequestSent` | LocalDate | When invited | Date |
| `dateResponded` | LocalDate | When responded | Date |

**No conflicts identified** - Clean entity

---

## 4. SAVINGS MANAGEMENT MODULE

### 4.1 SavingsAccount Entity (`SavingsAccount.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/savings/domain/entity/SavingsAccount.java`

| Variable Name | Type | Business Meaning | Notes |
|--------------|------|------------------|-------|
| `id` | UUID | Account ID | Primary key |
| `accountNumber` | String | Account reference | Format: SAV00001 or FD00001 |
| `member` | Member | Account owner | ManyToOne |
| `product` | SavingsProduct | Account type | ManyToOne |
| `balance` | BigDecimal | Current balance | **KEY: Main balance field** |
| `totalDeposits` | BigDecimal | Lifetime deposits | Cumulative |
| `totalWithdrawals` | BigDecimal | Lifetime withdrawals | Cumulative |
| `accruedInterest` | BigDecimal | Interest earned | Not yet paid |
| `maturityDate` | LocalDate | When account matures | For fixed deposits |
| `status` | AccountStatus | Account state | ACTIVE/DORMANT/CLOSED/FROZEN/MATURED |
| `accountOpenDate` | LocalDateTime | When opened | Date field |
| `createdAt` | LocalDateTime | Record creation | Audit |
| `updatedAt` | LocalDateTime | Last update | Audit |

**Potential Conflicts:**
1. ⚠️ **SYNC ISSUE**: `balance` should equal sum of all deposit/withdrawal transactions
2. ⚠️ **DUPLICATION**: Sum of all member's savings accounts should equal `Member.totalSavings`

### 4.2 SavingsProduct Entity (`SavingsProduct.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/savings/domain/entity/SavingsProduct.java`

| Variable Name | Type | Business Meaning | Notes |
|--------------|------|------------------|-------|
| `id` | UUID | Product ID | Primary key |
| `name` | String | Product name | Unique |
| `description` | String | Product details | Free text |
| `type` | ProductType | Product category | SAVINGS/FIXED_DEPOSIT/RECURRING_DEPOSIT |
| `interestRate` | BigDecimal | Annual % rate | Interest earned |
| `minBalance` | BigDecimal | Minimum balance required | Constraint |
| `minDurationMonths` | Integer | Lock-in period | For fixed accounts |
| `allowWithdrawal` | boolean | Can withdraw | Business rule |

**No conflicts identified** - Clean entity

### 4.3 Withdrawal Entity (`Withdrawal.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/savings/domain/entity/Withdrawal.java`

| Variable Name | Type | Business Meaning | Notes |
|--------------|------|------------------|-------|
| `id` | UUID | Withdrawal request ID | Primary key |
| `member` | Member | Requesting member | ManyToOne |
| `savingsAccount` | SavingsAccount | Account to withdraw from | ManyToOne |
| `amount` | BigDecimal | Amount requested | Withdrawal value |
| `status` | WithdrawalStatus | Request status | PENDING/APPROVED/REJECTED/PROCESSED |
| `reason` | String | Withdrawal reason | Free text |
| `requestDate` | LocalDateTime | When requested | Date |
| `approvalDate` | LocalDateTime | When approved | Optional |
| `processingDate` | LocalDateTime | When processed | Optional |
| `createdAt` | LocalDateTime | Record creation | Audit |

**Potential Conflicts:**
1. ⚠️ **WORKFLOW DATES**: Three separate date fields - complex workflow tracking

---

## 5. DEPOSIT MODULE (NEW)

### 5.1 Deposit Entity (`Deposit.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/deposit/domain/entity/Deposit.java`

| Variable Name | Type | Business Meaning | Notes |
|--------------|------|------------------|-------|
| `id` | UUID | Deposit ID | Primary key |
| `member` | Member | Depositing member | ManyToOne |
| `totalAmount` | BigDecimal | Total deposit | Sum of all allocations |
| `status` | DepositStatus | Processing status | PENDING/PROCESSING/COMPLETED/FAILED |
| `transactionReference` | String | Unique deposit ref | Format: DEP-XXXXXXXX |
| `paymentMethod` | String | How paid | MPESA/BANK/CASH |
| `paymentReference` | String | External ref | M-Pesa code, etc. |
| `allocations` | List<DepositAllocation> | How deposit split | OneToMany |
| `notes` | String | Additional info | Free text |
| `createdAt` | LocalDateTime | When created | Audit |
| `processedAt` | LocalDateTime | When processed | Audit |

**Potential Conflicts:**
1. ⚠️ **VALIDATION**: `totalAmount` must equal sum of all `allocations[].amount`
2. ⚠️ **NAMING**: `paymentMethod` is String here but Enum in Transaction entity

### 5.2 DepositAllocation Entity (`DepositAllocation.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/deposit/domain/entity/DepositAllocation.java`

| Variable Name | Type | Business Meaning | Notes |
|--------------|------|------------------|-------|
| `id` | UUID | Allocation ID | Primary key |
| `deposit` | Deposit | Parent deposit | ManyToOne |
| `destinationType` | DepositDestinationType | Where money goes | SAVINGS_ACCOUNT/LOAN_REPAYMENT/FINE_PAYMENT/etc. |
| `amount` | BigDecimal | Allocation amount | Portion of deposit |
| `savingsAccount` | SavingsAccount | Target savings account | Optional |
| `loan` | Loan | Target loan | Optional |
| `fine` | Fine | Target fine | Optional |
| `depositProduct` | DepositProduct | Target contribution | Optional |
| `status` | AllocationStatus | Processing status | PENDING/COMPLETED/FAILED |
| `notes` | String | Allocation notes | Free text |
| `errorMessage` | String | Failure reason | If failed |

**Potential Conflicts:**
1. ⚠️ **OPTIONAL LINKS**: Only ONE of (savingsAccount, loan, fine, depositProduct) should be set based on destinationType
2. ⚠️ **NO VALIDATION**: No constraint enforcing single destination

### 5.3 DepositProduct Entity (`DepositProduct.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/deposit/domain/entity/DepositProduct.java`

| Variable Name | Type | Business Meaning | Notes |
|--------------|------|------------------|-------|
| `id` | UUID | Product ID | Primary key |
| `name` | String | Product name | Unique |
| `description` | String | Product details | Free text |
| `targetAmount` | BigDecimal | Fundraising goal | Optional |
| `currentAmount` | BigDecimal | Amount collected | Running total |
| `status` | DepositProductStatus | Product state | ACTIVE/INACTIVE/COMPLETED |
| `createdBy` | Member | Who created product | Optional |
| `createdAt` | LocalDateTime | When created | Audit |
| `updatedAt` | LocalDateTime | Last update | Audit |

**Potential Conflicts:**
1. ⚠️ **NAMING CONFUSION**: "DepositProduct" vs "SavingsProduct" - semantically similar but different purposes
2. **NOTE**: DepositProduct is for custom contributions (Harambee, etc.), SavingsProduct is for account types

---

## 6. ACCOUNTING MODULE

### 6.1 GLAccount Entity (`GLAccount.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/finance/domain/entity/accounting/GLAccount.java`

| Variable Name | Type | Business Meaning | Notes |
|--------------|------|------------------|-------|
| `code` | String | Account code | Primary key (e.g., "1001") |
| `name` | String | Account name | Required |
| `type` | AccountType | Account category | ASSET/LIABILITY/EQUITY/INCOME/EXPENSE |
| `balance` | BigDecimal | Current balance | Calculated |
| `active` | Boolean | Account enabled | **NOTE: Changed from boolean to Boolean** |

**Potential Conflicts:**
1. ⚠️ **TYPE CHANGE**: `active` field changed from `boolean` to `Boolean` to fix JSON deserialization
2. **NOTE**: Helper method `isActive()` added for backward compatibility

### 6.2 JournalEntry Entity (`JournalEntry.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/finance/domain/entity/accounting/JournalEntry.java`

| Variable Name | Type | Business Meaning | Notes |
|--------------|------|------------------|-------|
| `id` | UUID | Entry ID | Primary key |
| `transactionDate` | LocalDateTime | Business date | When transaction occurred |
| `postedDate` | LocalDateTime | Posting date | When entered in system |
| `description` | String | Entry description | Free text |
| `referenceNo` | String | External reference | Links to TXN-... or LN... |
| `lines` | List<JournalLine> | Journal lines | OneToMany (Debit/Credit pairs) |

**No conflicts identified** - Clean entity

### 6.3 JournalLine Entity (`JournalLine.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/finance/domain/entity/accounting/JournalLine.java`

| Variable Name | Type | Business Meaning | Notes |
|--------------|------|------------------|-------|
| `id` | UUID | Line ID | Primary key |
| `journalEntry` | JournalEntry | Parent entry | ManyToOne |
| `account` | GLAccount | GL Account | ManyToOne |
| `debit` | BigDecimal | Debit amount | Default: ZERO |
| `credit` | BigDecimal | Credit amount | Default: ZERO |

**Potential Conflicts:**
1. ⚠️ **MUTUAL EXCLUSIVITY**: Either `debit` OR `credit` should be non-zero, not both
2. **NOTE**: JsonIgnore added to prevent infinite loop in serialization

### 6.4 GlMapping Entity (`GlMapping.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/finance/domain/entity/accounting/GlMapping.java`

| Variable Name | Type | Business Meaning | Notes |
|--------------|------|------------------|-------|
| `eventName` | String | Event identifier | Primary key (e.g., "LOAN_DISBURSEMENT") |
| `debitAccountCode` | String | Debit GL code | Account to debit |
| `creditAccountCode` | String | Credit GL code | Account to credit |
| `descriptionTemplate` | String | Description format | Template |

**No conflicts identified** - Configuration entity

### 6.5 FiscalPeriod Entity (`FiscalPeriod.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/finance/domain/entity/accounting/FiscalPeriod.java`

| Variable Name | Type | Business Meaning | Notes |
|--------------|------|------------------|-------|
| `id` | UUID | Period ID | Primary key |
| `name` | String | Period name | e.g., "FY 2024" |
| `startDate` | LocalDate | Period start | Date |
| `endDate` | LocalDate | Period end | Date |
| `active` | boolean | Currently active | Flag |
| `closed` | boolean | Period closed | Flag |

**No conflicts identified** - Clean entity

### 6.6 FinancialReport Entity (`FinancialReport.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/finance/domain/entity/FinancialReport.java`

| Variable Name | Type | Business Meaning | Notes |
|--------------|------|------------------|-------|
| `id` | UUID | Report ID | Primary key |
| `reportDate` | LocalDate | Report date | Date |
| `totalMembers` | BigDecimal | Member count | Metric |
| `totalSavings` | BigDecimal | Total savings | Metric |
| `totalLoansIssued` | BigDecimal | Loans disbursed | Metric |
| `totalLoansOutstanding` | BigDecimal | Active loans | Metric |
| `totalRepayments` | BigDecimal | Loan repayments | Metric |
| `totalShareCapital` | BigDecimal | Share capital | Metric |
| `totalInterestCollected` | BigDecimal | Interest income | Metric |
| `totalWithdrawals` | BigDecimal | Withdrawals | Metric |
| `totalIncome` | BigDecimal | Total revenue | Metric |
| `totalExpenses` | BigDecimal | Total costs | Metric |
| `netIncome` | BigDecimal | Profit/Loss | Calculated |
| `generatedAt` | LocalDateTime | Generation time | Audit |
| `createdAt` | LocalDateTime | Record creation | Audit |

**Potential Conflicts:**
1. ⚠️ **CALCULATED FIELD**: `netIncome` should equal `totalIncome - totalExpenses`

---

## 7. ADMIN/SYSTEM MODULE

### 7.1 SystemSetting Entity (`SystemSetting.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/admin/domain/entity/SystemSetting.java`

| Variable Name | Type | Business Meaning | Notes |
|--------------|------|------------------|-------|
| `key` | String | Setting identifier | Primary key (e.g., "REGISTRATION_FEE") |
| `value` | String | Setting value | Stored as string |
| `description` | String | Setting description | Free text |
| `dataType` | String | Value type hint | STRING/NUMBER/BOOLEAN |

**No conflicts identified** - Clean entity

### 7.2 Asset Entity (`Asset.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/admin/domain/entity/Asset.java`

| Variable Name | Type | Business Meaning | Notes |
|--------------|------|------------------|-------|
| `id` | UUID | Asset ID | Primary key |
| `name` | String | Asset name | Required |
| `category` | String | Asset category | e.g., "Furniture" |
| `serialNumber` | String | Serial/ID number | Optional |
| `purchaseCost` | BigDecimal | Original cost | Purchase price |
| `purchaseDate` | LocalDate | When purchased | Date |
| `usefulLifeYears` | Integer | Depreciation period | Years |
| `salvageValue` | BigDecimal | Residual value | End-of-life value |
| `accumulatedDepreciation` | BigDecimal | Total depreciation | Cumulative |
| `currentValue` | BigDecimal | Book value | **purchaseCost - accumulatedDepreciation** |
| `status` | AssetStatus | Asset state | ACTIVE/DISPOSED/LOST |
| `disposalDate` | LocalDate | When disposed | Optional |
| `disposalValue` | BigDecimal | Sale price | Optional |
| `disposalNotes` | String | Disposal notes | Optional |
| `createdAt` | LocalDateTime | Record creation | Audit |
| `updatedAt` | LocalDateTime | Last update | Audit |

**Potential Conflicts:**
1. ⚠️ **CALCULATED FIELD**: `currentValue` = `purchaseCost` - `accumulatedDepreciation` - must stay in sync

---

## 8. NOTIFICATION & AUDIT MODULE

### 8.1 Notification Entity (`Notification.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/notification/domain/entity/Notification.java`

| Variable Name | Type | Business Meaning | Notes |
|--------------|------|------------------|-------|
| `id` | UUID | Notification ID | Primary key |
| `user` | User | Recipient user | ManyToOne |
| `title` | String | Notification title | Short text |
| `message` | String | Notification body | Full message |
| `recipientEmail` | String | Email address | Delivery target |
| `recipientPhone` | String | Phone number | Delivery target |
| `subject` | String | Email subject | For email type |
| `type` | NotificationType | Notification category | EMAIL/SMS/IN_APP/PUSH/etc. |
| `status` | String | Delivery status | Free text |
| `isRead` | boolean | Read flag | For in-app |
| `createdAt` | LocalDateTime | Creation time | Audit |
| `sentAt` | LocalDateTime | Sent time | Delivery time |
| `failureReason` | String | Error message | If failed |
| `retryCount` | Integer | Retry attempts | Default: 0 |

**No conflicts identified** - Clean entity

### 8.2 AuditLog Entity (`AuditLog.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/audit/domain/entity/AuditLog.java`

| Variable Name | Type | Business Meaning | Notes |
|--------------|------|------------------|-------|
| `id` | UUID | Log ID | Primary key |
| `user` | User | User who performed action | ManyToOne |
| `userEmail` | String | User's email | **DUPLICATE with user.email** |
| `userName` | String | User's name | **DUPLICATE with user.firstName + lastName** |
| `action` | String | Action performed | CREATE/UPDATE/DELETE/etc. |
| `entityType` | String | Entity affected | e.g., "Member", "Loan" |
| `entityId` | String | Entity identifier | UUID as string |
| `description` | String | Action description | Free text |
| `ipAddress` | String | Client IP | Security field |
| `userAgent` | String | Browser info | Security field |
| `status` | Status | Action result | SUCCESS/FAILURE/PENDING |
| `errorMessage` | String | Error details | If failed |
| `createdAt` | LocalDateTime | Log time | Auto-generated |

**Potential Conflicts:**
1. ⚠️ **DENORMALIZATION**: `userEmail` and `userName` duplicate data from `user` entity
2. **REASON**: Intentional for audit trail integrity - preserves data if user deleted

---

## 9. DTO ANALYSIS

### 9.1 MemberDTO (`MemberDTO.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/member/api/dto/MemberDTO.java`

**Matches Member entity fields 1:1** - No conflicts

### 9.2 LoanDTO (`LoanDTO.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/loan/api/dto/LoanDTO.java`

**Additional fields not in Loan entity:**
- `memberName` - Derived from `member.firstName + lastName`
- `productName` - Derived from `product.name`
- `processingFee` - From product
- `memberSavings` - From member

**Missing fields from Loan entity:**
- `totalPrepaid`, `totalArrears`, `gracePeriodWeeks` - Not exposed in DTO

### 9.3 SavingsAccountDTO (`SavingsAccountDTO.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/savings/api/dto/SavingsAccountDTO.java`

**Additional fields not in SavingsAccount entity:**
- `memberName` - Derived
- `productName` - Derived
- `interestRate` - From product

### 9.4 DepositDTO (`DepositDTO.java`)
**File:** `src/main/java/com/sacco/sacco_system/modules/deposit/api/dto/DepositDTO.java`

**Additional field:**
- `memberName` - Derived

**Matches entity otherwise**

---

## 10. CRITICAL ISSUES & RECOMMENDATIONS

### 10.1 Major Duplications

#### Issue 1: Member vs ShareCapital - totalShares
**Locations:**
- `Member.totalShares` (BigDecimal)
- `ShareCapital.paidAmount` (BigDecimal)
- `ShareCapital.totalShares` (BigDecimal)
- `ShareCapital.paidShares` (BigDecimal)

**Business Meaning:** Member's total share capital investment

**Conflict:** Four fields representing the same value

**Recommendation:** 
- Keep `ShareCapital.paidAmount` as source of truth
- Keep `Member.totalShares` as denormalized cache for performance
- Remove `ShareCapital.totalShares` and `ShareCapital.paidShares` duplication
- Add synchronization service to keep them in sync

#### Issue 2: Member vs SavingsAccount - totalSavings
**Locations:**
- `Member.totalSavings` (BigDecimal)
- Sum of all `SavingsAccount.balance` for member

**Business Meaning:** Member's total savings across all accounts

**Conflict:** Denormalized data that can go out of sync

**Recommendation:**
- Keep `Member.totalSavings` as cached value
- Add database trigger or application service to sync on every savings transaction
- Add validation endpoint to detect discrepancies

#### Issue 3: User vs Member - Personal Information
**Locations:**
- `User.email`, `User.firstName`, `User.lastName`, `User.phoneNumber`, `User.memberNumber`
- `Member.email`, `Member.firstName`, `Member.lastName`, `Member.phoneNumber`, `Member.memberNumber`

**Business Meaning:** User's personal details

**Conflict:** Complete duplication of personal info

**Recommendation:**
- This is intentional (User for auth, Member for business)
- Add synchronization service when member updates profile
- Consider making User.email the single source of truth

### 10.2 Naming Inconsistencies

#### Issue 1: Loan.monthlyRepayment stores weekly amount
**Location:** `Loan.monthlyRepayment` (BigDecimal)

**Business Meaning:** Actually stores WEEKLY repayment amount (see LoanService line 297)

**Conflict:** Field name doesn't match content

**Recommendation:** Rename to `weeklyRepayment` or `installmentAmount`

#### Issue 2: Transaction IDs
**Locations:**
- `Transaction.id` (UUID) - Database primary key
- `Transaction.transactionId` (String) - Human-readable reference (TXN123456789)
- `Transaction.referenceCode` (String) - External reference (M-Pesa code)

**Conflict:** Three different "IDs" for same transaction

**Recommendation:**
- Rename `transactionId` to `transactionNumber` or `transactionRef`
- Keep `referenceCode` for external references
- Keep `id` as is (database convention)

#### Issue 3: Payment Method - String vs Enum
**Locations:**
- `Transaction.paymentMethod` (Enum: PaymentMethod)
- `Deposit.paymentMethod` (String)

**Conflict:** Same concept, different types

**Recommendation:** Standardize to Enum for type safety

### 10.3 Missing Validations

#### Issue 1: DepositAllocation - Mutually Exclusive Destinations
**Location:** `DepositAllocation` entity

**Problem:** Only ONE of (savingsAccount, loan, fine, depositProduct) should be non-null based on `destinationType`, but no constraint enforces this

**Recommendation:** Add @PrePersist validation or database CHECK constraint

#### Issue 2: Calculated Fields Not Validated
**Locations:**
- `LoanRepayment.totalPaid` should equal `principalPaid + interestPaid`
- `FinancialReport.netIncome` should equal `totalIncome - totalExpenses`
- `Asset.currentValue` should equal `purchaseCost - accumulatedDepreciation`

**Recommendation:** Add validation methods in @PrePersist/@PreUpdate hooks

#### Issue 3: ShareCapital - Circular Calculation
**Location:** `ShareCapital` entity

**Problem:** `shareValue * totalShares` should equal `paidAmount`, but both `totalShares` and `paidShares` exist with no clear distinction

**Recommendation:** Clarify business logic - are these the same? If so, remove duplication.

### 10.4 Status Enum Proliferation

**Observation:** Many entities have status enums with overlapping values:
- `MemberStatus`: ACTIVE, INACTIVE, SUSPENDED, DECEASED
- `AccountStatus`: ACTIVE, DORMANT, CLOSED, FROZEN, MATURED
- `LoanStatus`: 19 different statuses
- `WithdrawalStatus`: PENDING, APPROVED, REJECTED, PROCESSED
- `ChargeStatus`: PENDING, PAID, WAIVED
- `FineStatus`: PENDING, PAID, WAIVED

**Recommendation:** Consider common status enum pattern for consistency

---

## 11. FRONTEND VARIABLE ANALYSIS

### Key Frontend State Variables (React)

#### Common Patterns Found:
1. **Member Data:**
   - `member` - Current logged-in member
   - `members` - List of all members
   - `selectedMember` - Member being viewed/edited

2. **Loan Data:**
   - `loan` / `loans` - Loan objects
   - `loanApplication` - Draft loan form data
   - `loanStatus` - Current loan workflow state
   - `guarantors` - Guarantor list

3. **Savings Data:**
   - `savingsAccounts` - List of accounts
   - `balance` - Current balance
   - `transactions` - Transaction history

4. **Deposit Data:**
   - `depositAmount` - Total deposit
   - `allocations` - Split destinations
   - `depositProducts` - Available products

5. **System Data:**
   - `settings` - System settings object
   - `saccoName`, `saccoLogo` - Branding
   - `userRole` - Current user's role

**No major conflicts found** - Frontend naming generally aligns with backend DTOs

---

## 12. SUMMARY TABLE - ALL CRITICAL DUPLICATIONS

| Variable/Field | Locations | Business Meaning | Severity | Recommendation |
|----------------|-----------|------------------|----------|----------------|
| **totalShares** | Member.totalShares<br>ShareCapital.paidAmount<br>ShareCapital.totalShares<br>ShareCapital.paidShares | Member's share capital | 🔴 CRITICAL | Consolidate to ShareCapital.paidAmount as source of truth |
| **totalSavings** | Member.totalSavings<br>Sum(SavingsAccount.balance) | Member's total savings | 🔴 CRITICAL | Keep denormalized but add sync service |
| **Personal Info** | User.email/firstName/lastName<br>Member.email/firstName/lastName | User's identity | 🟡 MEDIUM | Intentional separation but needs sync |
| **monthlyRepayment** | Loan.monthlyRepayment | Weekly installment amount | 🟡 MEDIUM | Rename to weeklyRepayment |
| **transactionId** | Transaction.id<br>Transaction.transactionId<br>Transaction.referenceCode | Transaction references | 🟡 MEDIUM | Clarify naming |
| **paymentMethod** | Transaction.paymentMethod (Enum)<br>Deposit.paymentMethod (String) | Payment type | 🟡 MEDIUM | Standardize to Enum |
| **Fine vs Charge** | Fine entity<br>Charge entity | Penalties and fees | 🟢 LOW | Semantic overlap but distinct purposes |
| **totalPaid** | LoanRepayment.totalPaid<br>= principalPaid + interestPaid | Repayment amount | 🟢 LOW | Add validation |

**Severity Legend:**
- 🔴 CRITICAL: Data integrity risk, multiple sources of truth
- 🟡 MEDIUM: Naming inconsistency or type mismatch
- 🟢 LOW: Minor issue or intentional design

---

## 13. CONCLUSION

The SACCO system codebase is generally well-structured with clear separation of concerns. The main issues identified are:

1. **Denormalization Challenges:** Several aggregate values (`totalShares`, `totalSavings`) are cached in multiple places for performance but lack robust synchronization mechanisms.

2. **Naming Inconsistencies:** Some fields don't accurately reflect their content (e.g., `monthlyRepayment` storing weekly amounts).

3. **Type Inconsistencies:** Similar concepts use different types in different places (String vs Enum for payment methods).

4. **Missing Constraints:** Some business rules (like mutually exclusive foreign keys) aren't enforced at the database level.

5. **Calculated Field Validation:** Fields that represent calculations aren't always validated to ensure they match their components.

**Overall Assessment:** The duplication and conflicts are manageable and appear to be mostly intentional design choices for performance optimization. The main recommendation is to add synchronization services and validation to maintain data integrity.

---

## APPENDIX: Complete Entity Inventory

**Total Entities:** 34

### Domain Entities
1. Member
2. User
3. Transaction
4. ShareCapital
5. Fine
6. Charge
7. Dividend
8. Loan
9. LoanProduct
10. LoanRepayment
11. LoanDisbursement
12. Guarantor
13. SavingsAccount
14. SavingsProduct
15. Withdrawal
16. Deposit
17. DepositAllocation
18. DepositProduct
19. GLAccount
20. JournalEntry
21. JournalLine
22. GlMapping
23. FiscalPeriod
24. FinancialReport
25. SystemSetting
26. Asset
27. Notification
28. AuditLog
29. VerificationToken

### Enums (30+)
- MemberStatus, RegistrationStatus
- User.Role
- TransactionType, PaymentMethod
- LoanStatus, DurationUnit, InterestType, RepaymentStatus, GuarantorStatus
- DisbursementMethod, DisbursementStatus
- AccountStatus, ProductType, WithdrawalStatus
- DepositStatus, AllocationStatus, DepositDestinationType, DepositProductStatus
- AccountType
- FineType, FineStatus
- ChargeType, ChargeStatus
- DividendStatus
- NotificationType
- AuditLog.Status, AssetStatus

---

**End of Report**

