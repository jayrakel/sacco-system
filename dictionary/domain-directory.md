# Domain Dictionary — Phase A (LOCKED)

**Status:** ✅LOCKED  
**Scope:** Core Identity, Audit, Membership, Security, Savings Foundation  
**Change Policy:**
- ❌No renames without a Dictionary Change Request (DCR)
- ❌No new fields during refactoring
- ✅The dictionary overrides code in all conflicts

---

## 1️. Global Definition: Audit & Identity

**All persistent entities MUST inherit these fields.**

- id : UUID (Database Primary Key)
- active : boolean (System-level soft delete flag)
- createdAt : LocalDateTime
- updatedAt : LocalDateTime
- createdBy : String
- updatedBy : String

---

## 2. Global Uniqueness Constraints (Phase A)

**The following fields MUST be globally unique:**

1. username
2. email
3. memberNumber
4. nationalId
5. phoneNumber
6. kraPin
7. productCode
8. accountNumber
9. transactionReference

---

## 3. Member

Represents a registered SACCO member.

### Identifiers
- Primary Identifier: id
- Business Identifiers:
    - memberId (UUID/String, system-generated)
    - memberNumber (Human-readable, unique)

### Personal Details
- firstName
- lastName
- nationalId (unique)
- kraPin (unique)
- dateOfBirth

### Contact Details
- phoneNumber (unique)
- email (unique)
- address

### Membership Details
- membershipDate
- profileImageUrl

### Status
- memberStatus (Enum- Business Lifecycle)
- registrationStatus (Enum- Financial Entry State)

### System Linkage
- userId (UUID, optional, links to User)

### Relationships
- beneficiaries : List<Beneficiary>
- employmentDetails : EmploymentDetails (One-to-One)

### Inherited
- Global Audit & Identity fields

---

## 4. Member Status & Lifecycle

### Enums (LOCKED)

**MemberStatus**
- ACTIVE
- SUSPENDED
- EXITED
- DECEASED

**RegistrationStatus**
- PENDING
- PAID

**MemberAction**
- REGISTER
- UPDATE
- SUSPEND
- REACTIVATE
- EXIT
- MARK_DECEASED

### Lifecycle Rules

| Action        | Allowed When       | Result    |
|---------------|--------------------|-----------|
| REGISTER      | New                | ACTIVE    |
| UPDATE        | ACTIVE             | No change |
| SUSPEND       | ACTIVE             | SUSPENDED |
| REACTIVATE    | SUSPENDED          | ACTIVE    |
| EXIT          | ACTIVE / SUSPENDED | EXITED    |
| MARK_DECEASED | ACTIVE / SUSPENDED | DECEASED  |

EXITED and DECEASED members are permanently read-only forever.

---

## 5. Beneficiary

Represents a designated next-of-kin.

**Identifiers**

Primary Identifier: id

**Fields**
- firstName
- lastName
- relationship
- identityNumber
- allocationPercentage
- memberId

Inherits Global Audit & Identity fields.

---

## 6. EmploymentDetails

Represents a member’s economic profile.

**Identifiers**

Primary Identifier: id

**Fields**

- employerName
- employmentTerms
- netMonthlyIncome
- bankAccountDetails
- memberId

Inherits Global Audit & Identity fields.

---

## 7. User (Authentication & Authorization Entity)

### Purpose
Represents a system authentication account.

A `User` may authenticate using:
- a **personal email** (Member access), OR
- an **official system-issued email** (Staff / Admin access)

The same `User` may hold both identities.

---

### Identity

| Field    | Type | Description                                       |
|----------|------|---------------------------------------------------|
| `id`     | UUID | Primary key (database-generated)                  |
| `userId` | UUID | Business identifier (system-generated, immutable) |

---

### Authentication Identifiers

| Field           | Type   | Constraints      | Description                                            |
|-----------------|--------|------------------|--------------------------------------------------------|
| `username`      | String | Unique, Not Null | Primary login identifier used by the auth system       |
| `email`         | String | Unique           | Personal email (used for Member login & communication) |
| `officialEmail` | String | Unique           | System-issued email for SACCO officials                |

#### 🔐 Authentication Rule (LOCKED)

- Authentication MAY occur using:
    - `username`
    - `email`
    - `officialEmail`
- All three resolve to the same `User`
- Internally, authentication MUST map to `userId`

> The system MUST NOT assume that `email == officialEmail`.

---

### Security State(locked)
| Field                | Type    | Description                                                                                                                          |
|----------------------|---------|--------------------------------------------------------------------------------------------------------------------------------------|
| `emailVerified`      | boolean | Defaults to `false`. Must be `true` before full access is granted.                                                                   |
| `mustChangePassword` | boolean | defaults to `true`. Enforced to first login and admin password reset. Automatically cleared only after a successfull password change |

### Credentials

| Field          | Type   | Description            |
|----------------|--------|------------------------|
| `passwordHash` | String | BCrypt-hashed password |

---

### Personal Details

| Field       | Type   | Description |
|-------------|--------|-------------|
| `firstName` | String | Given name  |
| `lastName`  | String | Family name |

> **Rule:**  
> Name fields are standardized system-wide (`firstName`, `lastName`).

---

### Status & Lifecycle

| Field        | Type    | Description              |
|--------------|---------|--------------------------|
| `userStatus` | Enum    | Authentication lifecycle |
| `active`     | Boolean | System-level soft delete |

#### `UserStatus` ENUM
ACTIVE
LOCKED
DISABLED


## 8. Role

- id
- roleName
- description
- active

Inherits Global Audit & Identity fields.

---

## 9. Permission

- id
- permissionName
- description
- active

Inherits Global Audit & Identity fields.

---

## 10. Security Relationships

### User ↔ Role
- Many-to-Many
- User variable: Set<Role> roles
- Join table: user_roles

### Role ↔ Permission
- Many-to-Many
- Role variable: Set<Permission> permissions
- Join table: role_permissions

---

## 11. Authentication & Authorization

### Authentication
- Username + Password
- Stateless
- JWT-based

### JWT Claims (Mandatory)
- sub
- userId
- roles
- permissions
- iat
- exp

### Password Policy
- BCrypt
- Minimum 10 characters
- Uppercase, lowercase, number required

---

## 12. AuditLog

Represents an immutable audit event.

- id
- action
- entityName
- entityId
- performedBy
- performedAt

Inherits Global Audit & Identity fields.

---

## 13. Member Service Contract

Service: MemberService

### Operations

- createMember()
- updateMember()
- suspendMember()
- reactivateMember()
- exitMember()
- markMemberDeceased()
- getMemberById()
- getMemberByMemberNumber()
- searchMembers()

---

## 14. Member API Endpoints

| Operation     | Method | Endpoint                              |
|---------------|--------|---------------------------------------|
| Create        | POST   | /api/members                          |
| Update        | PUT    | /api/members/{id}                     |
| Suspend       | POST   | /api/members/{id}/suspend             |
| Reactivate    | POST   | /api/members/{id}/reactivate          |
| Exit          | POST   | /api/members/{id}/exit                |
| Deceased      | POST   | /api/members/{id}/deceased            |
| Get by ID     | GET    | /api/members/{id}                     |
| Get by Number | GET    | /api/members/by-number/{memberNumber} |
| Search        | GET    | /api/members                          |

---

## 15. Savings Domain (Foundation)

### SavingsProduct
- id
- productCode (unique)
- productName
- description
- currencyCode
- Inherits Global Audit & Identity fields.

### SavingsAccount
- id
- accountNumber (unique)
- balanceAmount (cached BigDecimal)
- currencyCode
- accountStatus
- Inherits Global Audit & Identity fields.

### SavingsTransaction
- id
- transactionReference (unique)
- transactionType
- amount
- currencyCode
- transactionDate
- narration
- inherits Global Audit & Identity fields.
---

### 🔷 Phase B — Loans & Credit Domain (ADVANCED)

## 16. LoanProduct
**Defines a standardized loan offering.**

### Identifiers
id
productCode (Unique)

### Fields
- productName
- description
- interestRate
- interestType (FLAT, REDUCING_BALANCE)
- repaymentPeriodWeeks
- gracePeriodWeeks
- penaltyRate
- currencyCode
- active
- Inherits Global Audit & Identity fields.
---

### 17. Loan
Represents a loan issued to a member.

**Identifiers**
id
loanNumber (Unique)

**References**
- memberId
- productCode
  **Financials**
- principalAmount
- interestRate
- approvedAmount
- disbursedAmount
- outstandingPrincipal
- outstandingInterest
- totalOutstandingAmount

**Dates**
- applicationDate
- approvalDate
- disbursementDate
- maturityDate

**Status**
- loanStatus
- Inherits Global Audit & Identity fields.
---

### 18. LoanApplicationStatus (ENUM)
- DRAFT
- SUBMITTED
- UNDER_REVIEW
- APPROVED
- REJECTED
- CANCELLED
### LoanStatus (Financial/Accounting) (Enum)
- DISBURSED
- ACTIVE
- IN_ARREARS
- DEFAULTED
- CLOSED
- WRITTEN_OFF

### 🔐 Rule (LOCKED)

- A loan MUST NOT have a LoanStatus until it is DISBURSED.
- A loan MUST NOT have penalties, arrears, or repayments unless LoanStatus != APPLIED.

**This separation keeps:**
- Audit clean
- Accounting deterministic
- Reporting accurate
- Regulators happy
---

### 2️⃣ Penalties, Arrears, and Pre-payments — how does a member “see” them?
This is an accounting + state derivation problem, not a UI hack.
**Key Principle**
- Arrears and pre-payments are DERIVED STATES, not manually set flags.
---

### ✅ Required Financial Indicators (Derived, Not Stored)
**Arrears**
A member is IN ARREARS when:

`(sum of due installments up to today)>(sum of payments received up to today)`

**Arrears Fields (Derived)**
- arrearsAmount
- arrearsSinceDate
- overdueInstallmentsCount
  If `arrearsAmount > 0
→ LoanStatus = IN_ARREARS`
---

**Pre-payment (Advance Payment)**
A member is IN PREPAYMENT when:

`(total payments received)>(total installments due up to today)`

**Prepayment Fields (Derived)**
- prepaidAmount
- prepaidInstallmentsCount

**📌 Pre-payment does NOT change LoanStatus**
The loan remains ACTIVE unless fully settled.
---
### Penalties

**Penalties are:**
- Calculated per overdue installment
- Accrued daily or monthly (policy-driven)
- Recorded as separate accounting entries
---
**Penalties MUST:**
- Increase outstanding amount
- Be settled before interest and principal
- Be auditable
---

### 🔐 Settlement Order (LOCKED)

**When a payment is made:**
- Penalties
- Interest
- Principal
  This is non-negotiable in lending systems.
---

### 19. LoanGuarantor

Represents a member guaranteeing a loan.

- id
- loanId
- guarantorMemberId
- guaranteedAmount
- Inherits Global Audit & Identity fields.
---

### 20. LoanRepaymentSchedule

Represents the expected repayment plan.

- id
- loanId
- installmentNumber
- dueDate
- principalAmount
- interestAmount
- totalAmount
- paidAmount
- installmentStatus
---

### 21. LoanRepayment

Represents an actual repayment transaction.

- id
- loanId
- transactionReference
- paymentDate
- amountPaid
- principalComponent
- interestComponent
- penaltyComponent
- Inherits Global Audit & Identity fields.
---

### 22. Loan Accounting Rules (LOCKED)

Loans use amortized schedules

- Interest accrues daily
- Penalties apply on overdue installments
- Repayments settle in order:
    - Penalties
    - Interest
    - Principal
- Loan is CLOSED only when outstanding = 0
---

### 23. Loan Service Contract

**Service: LoanService**
- applyForLoan()
- approveLoan()
- rejectLoan()
- disburseLoan()
- postRepayment()
- calculateOutstanding()
- getLoanById()
- getLoansByMemberId()
---

### 24. Loan API Endpoints
| Operation | Method | Endpoint                        |
|-----------|--------|---------------------------------|
| Apply     | Post   | /api/loans                      |
| Approve   | POST   | /api/loans/{id}/approve         |
| Reject    | Post   | /api/loans/{id}/reject          |
| Disburse  | Post   | /api/loans/{id}/disburse        |
| Repay     | Post   | /api/loans/{id}/repay           |
| Get       | GET    | /api/loans/{id}                 |
| By Member | GET    | /api/loans/by-member/{memberId} |
---

## 🚀 PHASE C — Accounting & General Ledger (ADVANCED, PRODUCTION-GRADE)

This is not minimal. This is core-banking level accounting.

### 25. ChartOfAccount
Defines a ledger account.

**Identifiers**
- id
- accountCode (Unique)

**Fields**
- accountName
- accountType
### AccountType (ENUM)
- ASSET
- LIABILITY
- EQUITY
- INCOME
- EXPENSE
- active
- Inherits Global Audit & Identity fields.
---

## 26. GeneralLedgerEntry
Represents a single accounting movement.

- id
- entryDate
- accountCode
- debitAmount
- creditAmount
- referenceType
- referenceId
- narration

### 🔐 Rule:
Exactly one of debitAmount or creditAmount MUST be > 0.
---

## 27. JournalEntry
Represents a balanced accounting transaction.

- id
- journalDate
- referenceType
- referenceId
- narration
  **Relationship**
- journalEntries : List<GeneralLedgerEntry>

## 🔐 Rule:

`SUM(debits) == SUM(credits)`
---

## 28. Loan Accounting Integration (LOCKED)
**Loan Disbursement**

| Account         | Debit | Credit |
|-----------------|-------|--------|
| Loan Receivable | ✅     |        |
| Cash/Bank       |       | ✅      |

**Loan Repayment**

| Component       | Debit | Credit   |
|-----------------|-------|----------|
| Cash/Bank       | ✅     |          |
| Penalty Income  |       | (if any) |
| Interest Income |       |          |
| Loan Receivable |       |          |

**Penalty Accrual**

| Account         | Debit | Credit |
|-----------------|-------|--------|
| Loan Receivable |       |        |
| Penalty Income  |       |        |
---

### 29. Member Financial Position (Derived)

A member’s financial view MUST include:
- totalSavingsBalance
- totalLoanOutstanding
- arrearsAmount
- prepaidAmount
- netPosition

**This is computed, never stored.**
---

### 30. Accounting Invariants (NON-NEGOTIABLE)

- No journal entry without balance
- No financial update without journal entry
- No deletion of financial records
- Corrections use reversing entries only
- Ledger is the ultimate source of truth


# 🚀 PHASE D — REPORTING, STATEMENTS, & REGULATORY COMPLIANCE
**Status:** ✅Locked  
**Scope:** Financial reporting, member statements, regulatory outputs, system observability  
**Principle:** Everything here is DERIVED from Ledger + Domain Events

---

## 31. Reporting Principles (LOCKED)

1. Reports NEVER mutate data
2. Reports NEVER store aggregates
3. All figures must be reproducible from:
- GeneralLedgerEntry
- JournalEntry
- SavingsTransaction
- Loan / LoanRepaymentSchedule
4. Any mismatch = SYSTEM BUG, not “rounding”

---

## 32. Member Statement

Represents a generated, point-in-time financial snapshot.

### MemberStatement (Derived)

**Inputs**
- memberId
- startDate
- endDate

**Outputs**
- openingBalance
- totalSavingsDeposits
- totalSavingsWithdrawals
- totalLoanDisbursements
- totalLoanRepayments
- totalPenalties
- closingBalance

### Rules
- Statements are GENERATED, not persisted
- PDFs may be cached, data never is
- Ledger remains the source of truth

---

## 33. Savings Statement

### SavingsStatement (Derived)

**Inputs**
- accountNumber
- startDate
- endDate

**Outputs**
- openingBalance
- transactions : List<SavingsTransaction>
- closingBalance

### Rule
`openingBalance + sum(transactions) = closingBalance`

---

## 34. Loan Statement

### LoanStatement (Derived)

**Inputs**
- loanId
- startDate
- endDate

**Outputs**
- principalBroughtForward
- interestAccrued
- penaltiesAccrued
- paymentsReceived
- principalOutstanding
- arrearsAmount
- prepaidAmount

---

## 35. Trial Balance

### TrialBalance (Derived)

**Fields**
- accountCode
- accountName
- debitTotal
- creditTotal

### Rule
`SUM(debitTotal) == SUM(creditTotal)`

Failure here means:
❌ System integrity breach

---

## 36. Financial Statements

### ProfitAndLossStatement

**Derived From**
- INCOME accounts
- EXPENSE accounts

**Fields**
- totalIncome
- totalExpenses
- netProfit

---

### BalanceSheet

**Derived From**
- ASSET
- LIABILITY
- EQUITY

**Rule**
`ASSETS = LIABILITIES + EQUITY`

---

## 37. Regulatory Reports (SACCO / Cooperative)

### Examples
- Member Register
- Dormant Accounts Report
- Loan Portfolio Aging
- Non-Performing Loans (NPL)
- Capital Adequacy
- Liquidity Ratios

### Rule
All regulatory reports MUST be reproducible on demand.

No Excel-only logic.
No manual adjustments.
No “management overrides”.

---

## 38. Audit & Compliance Controls

### System Controls (Mandatory)

- Every financial mutation → JournalEntry
- Every JournalEntry → AuditLog
- Every API mutation → performedBy

---

## 39. Data Retention Policy

- Financial records: NEVER deleted
- Members marked EXITED / DECEASED → archived only
- AuditLog retained forever

---

## 40. Observability & Integrity Checks

### Scheduled System Checks

- Ledger balance check
- Savings balance reconciliation
- Loan outstanding vs schedule check
- Arrears calculation validation

### Failure Policy
Any failure:
- Raises system alert
- Blocks financial operations
- Requires administrative resolution

---

## 41. Reporting Service Contract

### ReportingService

- generateMemberStatement()
- generateSavingsStatement()
- generateLoanStatement()
- generateTrialBalance()
- generateProfitAndLoss()
- generateBalanceSheet()
- generateRegulatoryReport()

---

## 42. Reporting API Endpoints

| Operation         | Method | Endpoint                             |
|-------------------|--------|--------------------------------------|
| Member Statement  | GET    | /api/reports/members/{memberId}      |
| Savings Statement | GET    | /api/reports/savings/{accountNumber} |
| Loan Statement    | GET    | /api/reports/loans/{loanId}          |
| Trial Balance     | GET    | /api/reports/trial-balance           |
| P&L               | GET    | /api/reports/profit-loss             |
| Balance Sheet     | GET    | /api/reports/balance-sheet           |

---

# 🚀 PHASE E — EVENTS, NOTIFICATIONS & SYSTEM COMMUNICATION

**Status:** 🔐 LOCKED  
**Scope Locked:** Domain Events, Notifications, Asynchronous Processing, Integrations  
**Design Level:** Production-grade / Core-banking compatible

---

## 43. Core Principles (LOCKED)

1. Events represent **facts that already happened**
2. Events NEVER mutate state
3. Notifications are **side-effects only**
4. Notifications MUST NOT block transactions
5. Events are replayable and auditable
6. Accounting, Ledger, and Business Logic MUST NOT depend on notifications

---

## 44. DomainEvent (Base Contract)

All domain events MUST conform to this structure.

### DomainEvent
- eventId : UUID
- eventType : String
- entityName : String
- entityId : UUID
- occurredAt : LocalDateTime
- triggeredBy : String (userId or SYSTEM)
- payload : JSON (immutable snapshot)

**Rules**
- Events are immutable
- Events are published **after transaction commit**
- Payload is read-only and never re-processed as input

---

## 45. Core Domain Events

### Member Events
- MemberRegistered
- MemberUpdated
- MemberSuspended
- MemberReactivated
- MemberExited
- MemberMarkedDeceased

### User & Security Events
- UserCreated
- UserActivated
- UserDeactivated
- LoginSuccess
- LoginFailure
- RoleAssigned
- RoleRevoked
- PasswordResetRequested
- PasswordResetCompleted

### Savings Events
- SavingsAccountOpened
- SavingsDeposited
- SavingsWithdrawn
- SavingsAccountFrozen
- SavingsAccountClosed

### Loan Events
- LoanApplied
- LoanApproved
- LoanRejected
- LoanDisbursed
- LoanRepaymentPosted
- LoanInArrears
- LoanCleared
- LoanWrittenOff

### Accounting Events
- JournalPosted
- LedgerBalanced
- LedgerImbalanceDetected

---

## 46. Notification Channels

Supported outbound communication channels:

- EMAIL
- SMS
- IN_APP
- WEBHOOK (future)

---

## 47. NotificationTemplate

Defines reusable message templates.

### NotificationTemplate
- id
- templateCode (unique)
- channel (ENUM)
- subjectTemplate
- bodyTemplate
- active

**Rules**
- Templates contain placeholders only
- No conditional logic
- No calculations
- No data mutation

---

## 48. NotificationLog

Tracks all notification attempts.

### NotificationLog
- id
- templateCode
- channel
- recipient
- referenceType
- referenceId
- status (SENT, FAILED, RETRIED)
- sentAt
- failureReason

Inherits Global Audit & Identity fields.

---

## 49. Event → Notification Mapping

Event-to-notification linkage is **configuration-driven**, not hardcoded.

| Domain Event           | Notification Template |
|------------------------|-----------------------|
| MemberRegistered       | MEMBER_WELCOME        |
| LoanApproved           | LOAN_APPROVAL         |
| LoanInArrears          | LOAN_ARREARS_ALERT    |
| LoanCleared            | LOAN_CLEARED_NOTICE   |
| PasswordResetRequested | PASSWORD_RESET        |

---

## 50. Notification Rules (LOCKED)

- Notifications NEVER mutate data
- Failures are logged, not silently ignored
- Retry policies are bounded
- Sensitive data MUST be masked
- Financial figures MUST be redacted where required

---

## 51. Asynchronous Processing Model

- Events are published **after DB commit**
- Event handlers run asynchronously
- Delivery model: at-least-once
- Handlers MUST be idempotent
- Failures are isolated per handler

---

## 52. EventStore (Optional but Recommended)

Persists all emitted domain events.

### EventStore
- id
- eventType
- entityName
- entityId
- payload
- occurredAt

**Uses**
- Event replay
- Forensic audit
- Analytics
- Debugging

---

## 53. External Integrations

Supported integrations:

- SMS Gateway
- Email Provider
- Payment Provider
- Identity Provider

**Rule**
External system failure MUST NOT corrupt internal state.

---

## 54. Services

### EventService
- publishEvent()
- replayEvents()
- getEventsByEntity()

### NotificationService
- sendNotification()
- resendFailedNotifications()
- enableTemplate()
- disableTemplate()

---

## 55. Notification API Endpoints

| Operation              | Method | Endpoint                                    |
|------------------------|--------|---------------------------------------------|
| Test Notification      | POST   | /api/notifications/test                     |
| View Notification Logs | GET    | /api/notifications/logs                     |
| Enable Template        | POST   | /api/notifications/templates/{code}/enable  |
| Disable Template       | POST   | /api/notifications/templates/{code}/disable |

---

## 🔐 PHASE E EXIT CRITERIA

Before locking Phase E:

- [ ] Events emitted only post-commit
- [ ] Notifications are async and non-blocking
- [ ] No business logic in templates
- [ ] Event replay tested
- [ ] Accounting independent of notifications

---

## 🧱 PHASE E SUMMARY

- Events = Facts
- Notifications = Side-effects
- Ledger & Accounting = Untouched
- System = Observable, Auditable, Resilient

---
# 🧪 PHASE F — TESTING, INVARIANTS, DATA INTEGRITY & GOVERNANCE

**Status:** 🟡 ACTIVE (Not Locked)  
**Scope:** System correctness, regression safety, refactoring protection, audit confidence  
**Purpose:** Ensure the SACCO system is provably correct, refactor-safe, and regulator-ready

Phase F defines **what must always be true**, not how code is written.

---

## 43. Core Testing Philosophy (LOCKED)

1. Tests protect the **Domain Dictionary**, not implementation details
2. Business invariants are more important than code coverage
3. Any refactor that passes Phase F tests is SAFE
4. Any test failure indicates:
    - ❌ Business rule violation
    - ❌ Accounting integrity breach
    - ❌ Dictionary drift

---

## 44. Test Classification Model

### 44.1 Unit Tests
**Scope**
- Entity validation
- Enum transitions
- Pure calculations

**Rules**
- No database
- No Spring context
- No mocks for domain logic

---

### 44.2 Integration Tests
**Scope**
- Repository mappings
- Service lifecycle flows
- Transaction boundaries

**Rules**
- Real database (H2 / Testcontainers)
- Rollback after each test
- No external systems

---

### 44.3 Financial Integrity Tests (MANDATORY)
**Scope**
- Ledger correctness
- Balance reconciliation
- Arrears & penalties derivation

**Rules**
- No mocking allowed
- Must reproduce balances from raw data

---

### 44.4 Contract Tests
**Scope**
- API request/response schemas
- Enum values
- Field names (dictionary enforcement)

**Rules**
- Any rename breaks tests
- Protects frontend & integrations

---

## 45. Global System Invariants (NON-NEGOTIABLE)

These MUST hold true at all times.

### 45.1 Identity & Audit
- Every entity has `id`, `createdAt`, `createdBy`
- `active = false` never deletes data
- AuditLog is append-only

---

### 45.2 Member Invariants
- `nationalId`, `memberNumber`, `phoneNumber`, `email` are unique
- EXITED or DECEASED members:
    - Cannot transact
    - Cannot be modified
- `memberStatus` follows lifecycle rules only

---

### 45.3 Savings Invariants
- `SavingsTransaction` is immutable
- `SavingsAccount.balanceAmount` equals:
  SUM(transactions.amount)

- Withdrawals never exceed balance
- CLOSED accounts never transact

---

### 45.4 Loan Invariants
- LoanStatus exists only after disbursement
- Outstanding = principal + interest + penalties
- Repayment settlement order:
1. Penalties
2. Interest
3. Principal
- Loan closes only when outstanding = 0

---

### 45.5 Accounting Invariants
- Every financial operation creates:
- JournalEntry
- Balanced GeneralLedgerEntries
- SUM(debits) == SUM(credits)
- No deletion or update of ledger entries
- Corrections use reversal entries only

---

## 46. Mandatory Test Scenarios (Minimum Set)

### 46.1 Member Lifecycle
- Create → Suspend → Reactivate → Exit
- Attempt invalid transitions → FAIL
- EXITED member mutation → FAIL

---

### 46.2 Savings Scenarios
- Deposit increases balance
- Withdraw reduces balance
- Overdraw attempt → FAIL
- Reconciliation test:
```

ledger balance == account balance

```

---

### 46.3 Loan Scenarios
- Apply → Approve → Disburse
- Repayment with penalties
- Arrears derivation accuracy
- Prepayment does NOT close loan

---

### 46.4 Accounting Scenarios
- Loan disbursement journal
- Repayment journal split correctly
- Trial balance equals zero

---

## 47. Refactoring Safety Net (CRITICAL)

### Refactor Approval Rules
A refactor is APPROVED only if:
- Phase A–E dictionary unchanged
- All Phase F tests pass
- No new warnings introduced

### Automatic Refactor Rejection
- Any test bypass
- Any invariant disabled
- Any hardcoded balances or aggregates

---

## 48. Dictionary Enforcement Tests

### Purpose
Prevent drift between:
- Code
- API
- Dictionary

### Examples
- DTO field names must match dictionary
- Enum values must match dictionary
- API paths must match dictionary

---

## 49. Observability & Runtime Guards

### Runtime Assertions (Production-Safe)
- Ledger imbalance → BLOCK operations
- Negative balances → BLOCK
- Status violations → BLOCK

### Monitoring Alerts
- Failed reconciliation
- Unexpected arrears spikes
- Journal imbalance

---

## 50. Phase F Exit Criteria

Phase F may be LOCKED only when:

- [ ] All invariants are test-covered
- [ ] Financial reconciliation passes
- [ ] Refactor tests fail on rename drift
- [ ] No direct balance manipulation exists
- [ ] Auditors can reproduce balances independently

---

# 🧱 SYSTEM STATUS AFTER PHASE F

At completion:
- Refactoring is SAFE
- Accounting is PROVABLE
- Reporting is TRUSTED
- Audits are DEFENSIBLE

This is the difference between:
❌ “It works”
✅ “It cannot be wrong”
