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

