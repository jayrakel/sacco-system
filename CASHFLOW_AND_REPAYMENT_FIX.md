# Cash Flow & Repayment Schedule Fixes ✅

## Issues Fixed

### Issue 1: No Cash Flow Visibility ❌ → ✅
**Problem:** Cannot see how money flows in and out of the SACCO

### Issue 2: Incorrect Repayment Calculation ❌ → ✅
**Problem:** Repayment schedule doesn't include interest and doesn't convert months to weeks properly

---

## Fix 1: Cash Flow Tracking System

### New Entity: CashFlow

**Purpose:** Track ALL money movements in the SACCO

**What It Tracks:**
```java
public enum TransactionType {
    // INFLOWS (Money coming in)
    SHARE_CONTRIBUTION,      // Member buys shares
    SAVINGS_DEPOSIT,         // Member deposits savings
    LOAN_REPAYMENT,          // Member repays loan
    LOAN_APPLICATION_FEE,    // Application fee payment
    MEMBERSHIP_FEE,          // Registration fee
    FINE_PAYMENT,            // Fine payments
    INTEREST_INCOME,         // Interest from loans
    EXTERNAL_GRANT,          // Grants/donations
    
    // OUTFLOWS (Money going out)
    LOAN_DISBURSEMENT,       // Loan given to member
    WITHDRAWAL,              // Member withdraws savings
    REFUND,                  // Refund to member
    OPERATIONAL_EXPENSE,     // Rent, salaries, etc.
    BANK_CHARGES            // Bank fees
}
```

### Cash Flow Structure:

```java
CashFlow {
    id: UUID
    transactionReference: "CF-2024-001"
    type: LOAN_DISBURSEMENT
    direction: OUTFLOW
    amount: 10,000
    member: John Doe
    description: "Loan disbursement LN12345"
    relatedEntityId: loan-uuid
    paymentMethod: CHEQUE
    transactionDate: 2024-12-20
    recordedBy: treasurer-uuid
}
```

### Money Flow Examples:

**Example 1: Member Applies for Loan (Application Fee)**
```
CashFlow Record:
- Type: LOAN_APPLICATION_FEE
- Direction: INFLOW
- Amount: 500
- Member: John Doe
- Payment Method: MPESA
- M-Pesa Code: QAB123XYZ
- Related Entity: Loan LN12345

Effect on SACCO Balance:
  Previous Balance: 100,000
  + Application Fee: 500
  New Balance: 100,500 ✅
```

**Example 2: Loan Disbursed to Member**
```
CashFlow Record:
- Type: LOAN_DISBURSEMENT
- Direction: OUTFLOW
- Amount: 10,000
- Member: John Doe
- Payment Method: CHEQUE
- Cheque Number: CHQ001234
- Related Entity: Loan LN12345

Effect on SACCO Balance:
  Previous Balance: 100,500
  - Loan Disbursed: 10,000
  New Balance: 90,500 ✅
```

**Example 3: Member Repays Loan**
```
CashFlow Record:
- Type: LOAN_REPAYMENT
- Direction: INFLOW
- Amount: 1,375 (weekly installment)
- Member: John Doe
- Payment Method: MPESA
- M-Pesa Code: QCD456XYZ
- Related Entity: Loan LN12345

Effect on SACCO Balance:
  Previous Balance: 90,500
  + Loan Repayment: 1,375
  New Balance: 91,875 ✅
```

**Example 4: Member Withdraws Savings**
```
CashFlow Record:
- Type: WITHDRAWAL
- Direction: OUTFLOW
- Amount: 5,000
- Member: Alice Smith
- Payment Method: BANK_TRANSFER
- Bank Reference: TRF789ABC

Effect on SACCO Balance:
  Previous Balance: 91,875
  - Withdrawal: 5,000
  New Balance: 86,875 ✅
```

### Cash Flow Dashboard Queries:

**Total Inflows:**
```sql
SELECT SUM(amount) FROM cash_flow WHERE direction = 'INFLOW'
```

**Total Outflows:**
```sql
SELECT SUM(amount) FROM cash_flow WHERE direction = 'OUTFLOW'
```

**Net Cash Flow (Current Balance):**
```sql
SELECT SUM(CASE 
    WHEN direction = 'INFLOW' THEN amount 
    ELSE -amount 
END) FROM cash_flow
```

**Cash Flow for This Month:**
```sql
SELECT SUM(CASE 
    WHEN direction = 'INFLOW' THEN amount 
    ELSE -amount 
END) FROM cash_flow 
WHERE transaction_date >= '2024-12-01'
```

---

## Fix 2: Correct Repayment Calculation

### Problem Example:

**Before (WRONG):**
```
Loan Amount: 10,000
Interest Rate: 10%
Duration: 2 months

Calculation (WRONG):
- Weekly Repayment = 10,000 / 8 weeks = 1,250
- Member repays: 1,250 × 8 = 10,000

Issue: NO INTEREST CHARGED! ❌
```

**After (CORRECT):**
```
Loan Amount: 10,000
Interest Rate: 10%
Duration: 2 months

Calculation (CORRECT):
Step 1: Calculate Interest
  Interest = 10,000 × 0.10 = 1,000

Step 2: Calculate Total Repayment
  Total = 10,000 + 1,000 = 11,000

Step 3: Convert Months to Weeks
  2 months × 4 weeks/month = 8 weeks

Step 4: Calculate Weekly Installment
  Weekly = 11,000 / 8 = 1,375

Member repays: 1,375 × 8 = 11,000 ✅
```

### Repayment Calculation Formula:

```java
/**
 * Calculate Weekly Repayment
 * 
 * Formula:
 * 1. Interest Amount = Principal × Interest Rate
 * 2. Total Repayment = Principal + Interest Amount
 * 3. Weeks = Duration × 4 (if months) OR Duration (if weeks)
 * 4. Weekly Installment = Total Repayment / Weeks
 */
public BigDecimal calculateWeeklyRepayment(
    BigDecimal principal,      // e.g., 10,000
    BigDecimal interestRate,   // e.g., 0.10 (10%)
    Integer duration,          // e.g., 2
    DurationUnit unit          // e.g., MONTHS
) {
    // Step 1: Interest = 10,000 × 0.10 = 1,000
    BigDecimal interest = principal.multiply(interestRate);
    
    // Step 2: Total = 10,000 + 1,000 = 11,000
    BigDecimal totalRepayment = principal.add(interest);
    
    // Step 3: Weeks = 2 × 4 = 8
    int weeks = (unit == MONTHS) ? duration * 4 : duration;
    
    // Step 4: Weekly = 11,000 / 8 = 1,375
    BigDecimal weekly = totalRepayment.divide(weeks, 2, HALF_UP);
    
    return weekly; // 1,375.00
}
```

### Duration Conversion:

**Months to Weeks:**
- 1 month = 4 weeks
- 2 months = 8 weeks
- 3 months = 12 weeks
- 6 months = 24 weeks
- 12 months = 48 weeks

**Weeks Stay as Weeks:**
- 4 weeks = 4 weeks
- 8 weeks = 8 weeks
- 12 weeks = 12 weeks

### Example Calculations:

**Example 1: 2 Months Duration**
```
Input:
- Principal: 10,000
- Interest Rate: 10%
- Duration: 2 MONTHS

Calculation:
- Interest: 10,000 × 0.10 = 1,000
- Total: 10,000 + 1,000 = 11,000
- Weeks: 2 × 4 = 8 weeks
- Weekly: 11,000 / 8 = 1,375

Result:
✅ Weekly Installment: KES 1,375
✅ Total to Repay: KES 11,000
✅ Interest Paid: KES 1,000
```

**Example 2: 8 Weeks Duration**
```
Input:
- Principal: 10,000
- Interest Rate: 10%
- Duration: 8 WEEKS

Calculation:
- Interest: 10,000 × 0.10 = 1,000
- Total: 10,000 + 1,000 = 11,000
- Weeks: 8 weeks (no conversion)
- Weekly: 11,000 / 8 = 1,375

Result:
✅ Weekly Installment: KES 1,375
✅ Total to Repay: KES 11,000
✅ Interest Paid: KES 1,000
```

**Example 3: 6 Months Duration**
```
Input:
- Principal: 50,000
- Interest Rate: 15%
- Duration: 6 MONTHS

Calculation:
- Interest: 50,000 × 0.15 = 7,500
- Total: 50,000 + 7,500 = 57,500
- Weeks: 6 × 4 = 24 weeks
- Weekly: 57,500 / 24 = 2,395.83

Result:
✅ Weekly Installment: KES 2,395.83
✅ Total to Repay: KES 57,500
✅ Interest Paid: KES 7,500
```

### Repayment Schedule Display:

**Week-by-Week Breakdown:**
```
Loan: LN12345
Principal: KES 10,000
Interest Rate: 10%
Duration: 2 months (8 weeks)
Weekly Installment: KES 1,375

Week | Installment | Principal | Interest | Remaining
-----|-------------|-----------|----------|----------
  1  |    1,375    |   1,250   |   125    |  9,625
  2  |    1,375    |   1,250   |   125    |  8,250
  3  |    1,375    |   1,250   |   125    |  6,875
  4  |    1,375    |   1,250   |   125    |  5,500
  5  |    1,375    |   1,250   |   125    |  4,125
  6  |    1,375    |   1,250   |   125    |  2,750
  7  |    1,375    |   1,250   |   125    |  1,375
  8  |    1,375    |   1,250   |   125    |      0
     |-------------|-----------|----------|
Total|   11,000    |  10,000   | 1,000    |
```

---

## Integration Points

### When Loan is Created:
```java
// In LoanService.initiateApplication()
BigDecimal weeklyRepayment = repaymentScheduleService.calculateWeeklyRepayment(
    amount, 
    product.getInterestRate(), 
    duration, 
    unit
);

loan.setMonthlyRepayment(weeklyRepayment); // Store weekly amount
```

### When Loan is Updated:
```java
// In LoanService.updateApplication()
BigDecimal weeklyRepayment = repaymentScheduleService.calculateWeeklyRepayment(
    newAmount,
    loan.getProduct().getInterestRate(),
    newDuration,
    newUnit
);

loan.setMonthlyRepayment(weeklyRepayment); // Update weekly amount
```

### When Loan is Disbursed:
```java
// Create cash flow record
CashFlow disbursement = CashFlow.builder()
    .type(TransactionType.LOAN_DISBURSEMENT)
    .direction(FlowDirection.OUTFLOW)
    .amount(loan.getPrincipalAmount())
    .member(loan.getMember())
    .relatedEntityId(loan.getId())
    .paymentMethod(PaymentMethod.CHEQUE)
    .chequeNumber(chequeNumber)
    .build();

cashFlowRepository.save(disbursement);
```

### When Member Makes Repayment:
```java
// Create cash flow record
CashFlow repayment = CashFlow.builder()
    .type(TransactionType.LOAN_REPAYMENT)
    .direction(FlowDirection.INFLOW)
    .amount(installmentAmount)
    .member(member)
    .relatedEntityId(loan.getId())
    .paymentMethod(PaymentMethod.MPESA)
    .mpesaCode(mpesaCode)
    .build();

cashFlowRepository.save(repayment);
```

---

## Frontend Display

### Loan Application Form:
```javascript
// When member selects amount and duration
const calculateRepayment = async () => {
    const response = await api.post('/loans/calculate-repayment', {
        principal: 10000,
        interestRate: 0.10,
        duration: 2,
        unit: 'MONTHS'
    });
    
    // Response:
    {
        principal: 10000,
        interestRate: "10%",
        interestAmount: 1000,
        totalRepayment: 11000,
        durationInWeeks: 8,
        weeklyInstallment: 1375,
        monthlyInstallment: 5500
    }
};

// Display to user:
"You will repay KES 1,375 per week for 8 weeks"
"Total to repay: KES 11,000 (includes KES 1,000 interest)"
```

### Cash Flow Dashboard:
```javascript
const CashFlowDashboard = () => {
    return (
        <div className="dashboard">
            <h2>SACCO Cash Flow</h2>
            
            <div className="summary">
                <div className="inflow">
                    <h3>Total Inflows</h3>
                    <p>KES {totalInflows}</p>
                </div>
                
                <div className="outflow">
                    <h3>Total Outflows</h3>
                    <p>KES {totalOutflows}</p>
                </div>
                
                <div className="balance">
                    <h3>Current Balance</h3>
                    <p>KES {netCashFlow}</p>
                </div>
            </div>
            
            <div className="transactions">
                <h3>Recent Transactions</h3>
                {cashFlowRecords.map(record => (
                    <div key={record.id} className="transaction">
                        <span className={record.direction}>
                            {record.direction === 'INFLOW' ? '↑' : '↓'}
                        </span>
                        <span>{record.type}</span>
                        <span>KES {record.amount}</span>
                        <span>{record.member?.name}</span>
                        <span>{record.transactionDate}</span>
                    </div>
                ))}
            </div>
        </div>
    );
};
```

---

## Summary

### ✅ Cash Flow Tracking:
- **Created:** CashFlow entity
- **Created:** CashFlowRepository
- **Tracks:** All money movements (in and out)
- **Shows:** Real-time SACCO balance
- **Links:** To loans, withdrawals, deposits, etc.

### ✅ Repayment Calculation:
- **Created:** RepaymentScheduleService
- **Formula:** (Principal + Interest) / Weeks
- **Converts:** Months to weeks (1 month = 4 weeks)
- **Stores:** Weekly installment amount in loan
- **Updates:** Automatically when loan is created/edited

### ✅ What Members See:
- Exact weekly installment amount
- Total amount to repay
- Interest amount
- Week-by-week breakdown
- Clear repayment schedule

### ✅ What Admins See:
- All cash inflows
- All cash outflows
- Current SACCO balance
- Cash flow by type
- Cash flow by member
- Date range reports

**The system now has complete money tracking and accurate loan calculations!** 🎉

