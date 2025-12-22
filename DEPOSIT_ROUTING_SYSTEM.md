# Complex Deposit Routing System

## Overview
The deposit routing system allows members to make a single deposit that can be automatically split and routed to multiple destinations based on their preferences.

## Features

### 1. **Flexible Deposit Allocation**
Members can deposit money and route it to any combination of:
- **Savings Accounts** - Direct deposit to savings
- **Loan Repayments** - Pay off active loans
- **Fine Payments** - Clear pending fines and penalties
- **Contribution Products** - Custom products created by admins (meat contribution, harambee, etc.)
- **Share Capital** - Increase member share capital

### 2. **Custom Contribution Products**
Admins (Chairperson, Treasurer, Secretary) can create custom contribution products such as:
- Meat contribution
- Harambee fundraising
- Group projects
- Special events
- Any other member contributions

## API Endpoints

### Member Endpoints

#### 1. Create Deposit with Allocations
**POST** `/api/deposits/create`

**Request Body:**
```json
{
  "totalAmount": 10000,
  "paymentMethod": "MPESA",
  "paymentReference": "QWE123RT45",
  "allocations": [
    {
      "destinationType": "SAVINGS_ACCOUNT",
      "amount": 5000,
      "savingsAccountId": "uuid-here",
      "notes": "Monthly savings"
    },
    {
      "destinationType": "CONTRIBUTION_PRODUCT",
      "amount": 3000,
      "depositProductId": "uuid-here",
      "notes": "Meat contribution"
    },
    {
      "destinationType": "FINE_PAYMENT",
      "amount": 2000,
      "fineId": "uuid-here",
      "notes": "Clear late payment fine"
    }
  ],
  "notes": "December deposit"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Deposit processed successfully",
  "deposit": {
    "id": "deposit-uuid",
    "transactionReference": "DEP-ABC12345",
    "totalAmount": 10000,
    "status": "COMPLETED",
    "allocations": [
      {
        "id": "allocation-1-uuid",
        "destinationType": "SAVINGS_ACCOUNT",
        "amount": 5000,
        "status": "COMPLETED",
        "destinationName": "SAV-001234"
      },
      {
        "id": "allocation-2-uuid",
        "destinationType": "CONTRIBUTION_PRODUCT",
        "amount": 3000,
        "status": "COMPLETED",
        "destinationName": "Meat Contribution"
      },
      {
        "id": "allocation-3-uuid",
        "destinationType": "FINE_PAYMENT",
        "amount": 2000,
        "status": "COMPLETED",
        "destinationName": "Late payment penalty"
      }
    ],
    "createdAt": "2025-12-21T10:30:00",
    "processedAt": "2025-12-21T10:30:02"
  }
}
```

#### 2. Get Deposit History
**GET** `/api/deposits/my-history`

**Response:**
```json
{
  "success": true,
  "deposits": [
    {
      "id": "uuid",
      "transactionReference": "DEP-ABC12345",
      "totalAmount": 10000,
      "status": "COMPLETED",
      "paymentMethod": "MPESA",
      "paymentReference": "QWE123RT45",
      "allocations": [...],
      "createdAt": "2025-12-21T10:30:00",
      "processedAt": "2025-12-21T10:30:02"
    }
  ]
}
```

#### 3. Get Available Contribution Products
**GET** `/api/deposits/products/available`

**Response:**
```json
{
  "success": true,
  "products": [
    {
      "id": "uuid",
      "name": "Meat Contribution",
      "description": "Monthly meat contribution for members",
      "targetAmount": 100000,
      "currentAmount": 45000,
      "status": "ACTIVE",
      "createdByName": "John Doe",
      "createdAt": "2025-12-01T08:00:00"
    },
    {
      "id": "uuid",
      "name": "December Harambee",
      "description": "Fundraising for community project",
      "targetAmount": 500000,
      "currentAmount": 120000,
      "status": "ACTIVE",
      "createdByName": "Jane Smith",
      "createdAt": "2025-12-05T10:00:00"
    }
  ]
}
```

### Admin Endpoints

#### 1. Create Contribution Product
**POST** `/api/admin/deposit-products`

**Authorization:** CHAIRPERSON, TREASURER, or SECRETARY

**Request Body:**
```json
{
  "name": "Meat Contribution",
  "description": "Monthly meat contribution for members",
  "targetAmount": 100000
}
```

**Response:**
```json
{
  "success": true,
  "message": "Deposit product created successfully",
  "product": {
    "id": "uuid",
    "name": "Meat Contribution",
    "description": "Monthly meat contribution for members",
    "targetAmount": 100000,
    "currentAmount": 0,
    "status": "ACTIVE",
    "createdByName": "John Doe",
    "createdAt": "2025-12-21T10:00:00"
  }
}
```

#### 2. Update Contribution Product
**PUT** `/api/admin/deposit-products/{id}`

#### 3. Get All Products (Including Closed)
**GET** `/api/admin/deposit-products`

#### 4. Close Product
**POST** `/api/admin/deposit-products/{id}/close`

#### 5. Delete Product
**DELETE** `/api/admin/deposit-products/{id}`
*(Only if no contributions have been made)*

## Destination Types

| Type | Description | Required Field | Example |
|------|-------------|----------------|---------|
| `SAVINGS_ACCOUNT` | Deposit to savings account | `savingsAccountId` | Regular savings |
| `LOAN_REPAYMENT` | Pay loan | `loanId` | Loan installment |
| `FINE_PAYMENT` | Clear fine | `fineId` | Late payment penalty |
| `CONTRIBUTION_PRODUCT` | Custom contribution | `depositProductId` | Meat contribution |
| `SHARE_CAPITAL` | Buy shares | None | Share purchase |

## Validation Rules

1. **Total Validation**: Sum of all allocations must equal `totalAmount`
2. **Positive Amounts**: All amounts must be greater than zero
3. **Ownership**: Members can only deposit to their own accounts/loans/fines
4. **Status Checks**:
   - Savings accounts must be ACTIVE
   - Loans must be ACTIVE
   - Fines must not be already PAID
   - Products must be ACTIVE
5. **Required Fields**: Each allocation must have the appropriate reference ID based on its type

## Accounting Integration

All deposit allocations create proper double-entry accounting records:

### Savings Account Deposit
```
DR: Bank Account (1020)
CR: Member Savings (2010)
```

### Loan Repayment
```
DR: Bank Account (1020)
CR: Loans Receivable (1200) [Principal]
CR: Interest Income (4002) [Interest]
```

### Fine Payment
```
DR: Bank Account (1020)
CR: Fines & Penalties Income (4004)
```

### Contribution Product
```
DR: Bank Account (1020)
CR: Member Deposits Non-Withdrawable (2001)
```

### Share Capital
```
DR: Bank Account (1020)
CR: Share Capital (3001)
```

## Error Handling

### Common Errors

1. **Validation Errors (400)**
```json
{
  "success": false,
  "message": "Allocations total (9500.00) does not match deposit amount (10000.00)"
}
```

2. **Security Errors (403)**
```json
{
  "success": false,
  "message": "Cannot deposit to another member's account"
}
```

3. **State Errors (400)**
```json
{
  "success": false,
  "message": "Savings account is not active"
}
```

### Partial Failures
If some allocations fail, the deposit status will be `FAILED`, and each allocation will have:
- `status`: `COMPLETED` or `FAILED`
- `errorMessage`: Description of the error (if failed)

## Use Cases

### Example 1: Monthly Deposit
Member wants to allocate KES 10,000:
- 6,000 → Savings account
- 2,000 → Loan repayment
- 2,000 → Meat contribution

### Example 2: Fine Clearance
Member deposits KES 5,000:
- 3,000 → Clear late payment fine
- 2,000 → Savings account

### Example 3: Multiple Contributions
Member deposits KES 8,000:
- 3,000 → Meat contribution
- 3,000 → Harambee
- 2,000 → Share capital

## Database Schema

### Tables Created:
- `deposit_products` - Custom contribution products
- `deposits` - Main deposit records
- `deposit_allocations` - Routing details for each deposit

### Relationships:
- Deposit → Member (Many-to-One)
- Deposit → DepositAllocation (One-to-Many)
- DepositAllocation → SavingsAccount/Loan/Fine/DepositProduct (Many-to-One)
- DepositProduct → Member (CreatedBy) (Many-to-One)

## Audit Trail

All deposit operations are logged:
- `CREATE_DEPOSIT` - When member creates deposit
- `CREATE_DEPOSIT_PRODUCT` - When admin creates product
- `UPDATE_DEPOSIT_PRODUCT` - When admin updates product
- `CLOSE_DEPOSIT_PRODUCT` - When admin closes product
- `DELETE_DEPOSIT_PRODUCT` - When admin deletes product

## Future Enhancements

1. **Scheduled Deposits**: Auto-allocate deposits based on saved preferences
2. **Percentage-based Allocation**: Instead of fixed amounts, use percentages
3. **Recurring Contributions**: Automatic monthly allocations
4. **Contribution Reports**: Analytics on contribution products
5. **Member Contribution History**: Track member's contributions to each product
