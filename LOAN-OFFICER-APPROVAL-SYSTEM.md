# Loan Officer Review & Approval System

**Module:** Loan Management  
**Version:** 1.0  
**Date:** January 10, 2026

---

## 📋 Overview

This document outlines the complete loan review and approval workflow for loan officers, including backend APIs, notifications, and frontend UI components.

---

## 🔄 Workflow States

### Loan Status Flow

```
PENDING_GUARANTORS → Add Guarantors
                   ↓
              SUBMITTED → Loan Officer Reviews
                   ↓
           UNDER_REVIEW → Loan Officer Makes Decision
                   ↓
         ┌──────────┴──────────┐
         ↓                     ↓
    APPROVED              REJECTED
         ↓
   DISBURSED (Finance Officer)
         ↓
      ACTIVE
```

---

## 🔐 Backend Implementation

### 1. New Service: `LoanOfficerService.java`

**Location:** `src/main/java/.../loan/domain/service/LoanOfficerService.java`

**Methods:**

#### `startReview(UUID loanId, String officerEmail)`
- Moves loan from `SUBMITTED` to `UNDER_REVIEW`
- Sends notification to applicant
- Creates audit log

#### `approveLoan(UUID loanId, String officerEmail, BigDecimal approvedAmount, String notes)`
- Validates loan is in `UNDER_REVIEW` or `SUBMITTED`
- Sets `APPROVED` status
- Records approval date and amount
- Sends approval email and SMS to applicant
- Creates audit trail

**Payload:**
```json
{
  "approvedAmount": 50000,
  "notes": "Approved based on good credit history"
}
```

#### `rejectLoan(UUID loanId, String officerEmail, String rejectionReason)`
- Validates loan is in `UNDER_REVIEW` or `SUBMITTED`
- Sets `REJECTED` status
- Sends rejection email and SMS with reason
- Creates audit trail

**Payload:**
```json
{
  "reason": "Insufficient guarantors or credit history issues"
}
```

#### `requestMoreInformation(UUID loanId, String officerEmail, String information)`
- Sends email/SMS to applicant requesting additional info
- Logs request in audit trail
- Does not change loan status

**Payload:**
```json
{
  "information": "Please provide updated payslips for the last 3 months"
}
```

#### `getLoanForReview(UUID loanId)`
- Fetches complete loan details with eager loading
- Includes member info, guarantors, product details

---

### 2. New Controller: `LoanOfficerController.java`

**Location:** `src/main/java/.../loan/api/controller/LoanOfficerController.java`

**Base URL:** `/api/loan-officer`

**Security:** Requires `LOAN_OFFICER` or `ADMIN` role

**Endpoints:**

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/pending-loans` | Get all submitted/under review loans |
| GET | `/loans/{loanId}` | Get detailed loan info for review |
| POST | `/loans/{loanId}/start-review` | Move to UNDER_REVIEW |
| POST | `/loans/{loanId}/approve` | Approve loan |
| POST | `/loans/{loanId}/reject` | Reject loan |
| POST | `/loans/{loanId}/request-info` | Request more info |
| GET | `/statistics` | Get officer dashboard stats |

---

### 3. Email Notifications

**Added to `EmailService.java`:**

#### `sendLoanApprovalEmail()`
- Beautiful HTML template with loan details
- Shows approved amount, loan number, product name
- Green/success theme

#### `sendLoanRejectionEmail()`
- Professional rejection notice
- Includes rejection reason
- Suggests next steps

**Example Approval Email:**

```
Subject: Loan Approved - LN-123456

Dear John Doe,

🎉 Congratulations! Your loan application has been approved.

┌──────────────────────┬─────────────────┐
│ Loan Number:         │ LN-123456       │
│ Product:             │ Emergency Loan  │
│ Approved Amount:     │ KES 50,000      │
└──────────────────────┴─────────────────┘

Your loan will be disbursed shortly...
```

---

### 4. Audit Trail

**Every action logged via `AuditService.logLoanAction()`:**

- Loan review started
- Loan approved/rejected
- Information requested
- Officer email, timestamp, IP address recorded

---

## 📊 API Examples

### Get Pending Loans

```http
GET /api/loan-officer/pending-loans
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "message": "Pending loans retrieved",
  "data": [
    {
      "id": "uuid",
      "loanNumber": "LN-123456",
      "memberName": "John Doe",
      "memberNumber": "MEM000123",
      "productName": "Emergency Loan",
      "principalAmount": 50000,
      "durationWeeks": 12,
      "interestRate": 12.5,
      "status": "SUBMITTED",
      "applicationDate": "2026-01-08",
      "guarantorsCount": 2,
      "guarantorsApproved": 2
    }
  ]
}
```

### Get Loan Details

```http
GET /api/loan-officer/loans/{loanId}
Authorization: Bearer {token}
```

**Response:** Complete Loan object with member, guarantors, product

### Start Review

```http
POST /api/loan-officer/loans/{loanId}/start-review
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "message": "Loan moved to under review",
  "data": { /* Updated loan object */ }
}
```

### Approve Loan

```http
POST /api/loan-officer/loans/{loanId}/approve
Authorization: Bearer {token}
Content-Type: application/json

{
  "approvedAmount": 45000,
  "notes": "Approved with reduced amount due to guarantor limits"
}
```

**Triggers:**
1. Loan status → APPROVED
2. Email sent to applicant
3. SMS sent to applicant
4. Audit log created

### Reject Loan

```http
POST /api/loan-officer/loans/{loanId}/reject
Authorization: Bearer {token}
Content-Type: application/json

{
  "reason": "Insufficient credit history. Member has pending arrears on previous savings account."
}
```

**Triggers:**
1. Loan status → REJECTED
2. Email sent with reason
3. SMS sent with reason
4. Audit log created

### Get Statistics

```http
GET /api/loan-officer/statistics
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "pendingReview": 5,
    "submitted": 3,
    "underReview": 2,
    "approved": 45,
    "rejected": 12,
    "activeLoans": 120,
    "totalDisbursed": 5000000,
    "totalOutstanding": 3200000
  }
}
```

---

## 🎨 Frontend Components (To Build)

### 1. Loan Officer Dashboard

**Route:** `/loan-officer/dashboard`

**Sections:**
- Statistics Cards (pending, approved, rejected, active)
- Pending Loans Table
- Quick Actions

### 2. Loan Review Page

**Route:** `/loan-officer/loans/:loanId`

**Sections:**
- Applicant Information
- Loan Details (amount, duration, interest rate)
- Guarantors List (with status)
- Member Credit History
- Action Buttons (Approve, Reject, Request Info)

### 3. Approval Modal

**Features:**
- Input for approved amount (pre-filled with requested amount)
- Option to reduce amount
- Notes/comments field
- Confirmation

### 4. Rejection Modal

**Features:**
- Required rejection reason field
- Predefined reasons dropdown + custom
- Confirmation

---

## 📧 Notification Flow

### When Loan Submitted:
✅ Already handled by `LoanApplicationService`

### When Review Started:
1. ✉️ Email: "Your loan is under review"
2. 📱 SMS: "Loan under review notification"

### When Approved:
1. ✉️ Email: Beautiful approval notice with details
2. 📱 SMS: "Congratulations! Loan approved for KES X"

### When Rejected:
1. ✉️ Email: Professional rejection with reason
2. 📱 SMS: "Loan application declined" with brief reason

### When Info Requested:
1. ✉️ Email: Details of information needed
2. 📱 SMS: "Additional info required - check email"

---

## 🔒 Security & Permissions

### Role Requirements:

```java
@PreAuthorize("hasAnyRole('LOAN_OFFICER', 'ADMIN')")
```

### Who Can:
- **LOAN_OFFICER**: Review, approve, reject loans
- **ADMIN**: Same as loan officer + view all stats
- **MEMBER**: View their own loan status only

### Validation:
- Loan must be in correct status
- Approved amount ≤ requested amount
- Rejection reason required
- All actions logged in audit trail

---

## 📝 Database Changes

### Existing Tables (No changes needed):
- ✅ `loans` - has all required fields
- ✅ `audit_logs` - tracks officer actions
- ✅ `notification_logs` - tracks sent emails/SMS

### New Columns Used:
- `loans.approval_date` - Set when approved
- `loans.approved_amount` - Can differ from principal
- `loans.updated_by` - Officer email

---

## 🧪 Testing Checklist

### Backend Tests:
- [ ] Submit loan → appears in pending list
- [ ] Start review → status changes, email sent
- [ ] Approve loan → status APPROVED, email/SMS sent
- [ ] Reject loan → status REJECTED, email/SMS sent
- [ ] Approve with reduced amount → saves correctly
- [ ] Request info → email sent, status unchanged
- [ ] Statistics API → correct counts

### Frontend Tests:
- [ ] Dashboard loads pending loans
- [ ] Click loan → review page opens
- [ ] Approve → modal confirms, API call succeeds
- [ ] Reject → reason required, API call succeeds
- [ ] Real-time status update after action
- [ ] Notifications appear in member dashboard

---

## 🚀 Deployment Steps

1. **Backend:**
   ```bash
   mvn clean package
   # Deploy new classes
   ```

2. **Database:**
   - No migrations needed (using existing schema)

3. **Frontend:**
   ```bash
   cd sacco-frontend
   npm install
   npm run build
   ```

4. **Verify:**
   - Create test loan as member
   - Login as loan officer
   - Review and approve/reject
   - Check member receives emails

---

## 📚 Code Locations Summary

| Component | File Path |
|-----------|-----------|
| Loan Officer Service | `modules/loan/domain/service/LoanOfficerService.java` |
| Loan Officer Controller | `modules/loan/api/controller/LoanOfficerController.java` |
| Loan Read Service (Updated) | `modules/loan/domain/service/LoanReadService.java` |
| Email Service (Updated) | `modules/notification/domain/service/EmailService.java` |
| Audit Service (Updated) | `modules/audit/domain/service/AuditService.java` |

---

## 🎯 Next Steps

1. ✅ Backend APIs created
2. ✅ Email notifications implemented
3. ✅ Audit logging added
4. ⏳ **Create frontend UI components**
5. ⏳ Add role-based access control UI
6. ⏳ Testing and deployment

---

**Status:** Backend Complete ✅ | Frontend Pending ⏳

**Last Updated:** January 10, 2026

