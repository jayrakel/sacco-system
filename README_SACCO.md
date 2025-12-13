# SACCO Management System

A comprehensive Spring Boot application for managing Savings and Credit Cooperative Organizations (SACCO).

## Features

### 1. **Member Management**
- Register new members with full profile information
- Manage member details (name, email, phone, ID number, address, DOB)
- Track member status (ACTIVE, INACTIVE, SUSPENDED)
- View member shares and savings

**Endpoints:**
```
POST   /api/members                           - Create new member
GET    /api/members/{id}                      - Get member by ID
GET    /api/members/number/{memberNumber}    - Get member by member number
GET    /api/members                           - Get all members
GET    /api/members/active                    - Get active members
PUT    /api/members/{id}                      - Update member
DELETE /api/members/{id}                      - Deactivate member
GET    /api/members/count/active              - Get count of active members
```

### 2. **Savings Account Management**
- Create savings accounts for members
- Deposit funds
- Withdraw funds
- Track account balance and transaction history
- View total savings balance

**Endpoints:**
```
POST   /api/savings/account                   - Create savings account
GET    /api/savings/account/{id}              - Get savings account by ID
GET    /api/savings/account/number/{accountNumber}  - Get by account number
GET    /api/savings/member/{memberId}        - Get accounts by member
GET    /api/savings                           - Get all savings accounts
POST   /api/savings/deposit                   - Deposit funds
POST   /api/savings/withdraw                  - Withdraw funds
GET    /api/savings/total-balance             - Get total balance
```

### 3. **Loan Management**
- Apply for loans
- Calculate interest and monthly repayments automatically
- Approve/reject loans
- Disburse approved loans
- Track loan status and balance
- Generate repayment schedule

**Endpoints:**
```
POST   /api/loans/apply                       - Apply for loan
GET    /api/loans/{id}                        - Get loan by ID
GET    /api/loans/number/{loanNumber}        - Get loan by number
GET    /api/loans/member/{memberId}          - Get member's loans
GET    /api/loans                             - Get all loans
GET    /api/loans/status/{status}             - Get loans by status
PUT    /api/loans/{id}/approve                - Approve loan
PUT    /api/loans/{id}/disburse               - Disburse loan
PUT    /api/loans/{id}/reject                 - Reject loan
GET    /api/loans/totals/disbursed            - Get total disbursed
GET    /api/loans/totals/outstanding          - Get outstanding balance
GET    /api/loans/totals/interest             - Get interest collected
```

### 4. **Loan Repayment Tracking**
- Automatic repayment schedule generation
- Track individual repayments
- Record repayment transactions
- Monitor overdue payments

### 5. **Share Capital Management**
- Track member share purchases
- Manage share values
- Track paid vs. unpaid shares
- Calculate total share capital

### 6. **Financial Reporting**
- Generate daily financial reports
- Track total savings, loans, and repayments
- Calculate net income
- View historical reports

**Endpoints:**
```
POST   /api/reports/generate                  - Generate daily report
GET    /api/reports/today                     - Get today's report
GET    /api/reports/date/{date}               - Get report by date (yyyy-MM-dd)
```

### 7. **Authentication** (Placeholder)
- Login endpoint
- Registration endpoint
- Health check

**Endpoints:**
```
POST   /api/auth/login                        - Login
POST   /api/auth/register                     - Register
GET    /api/auth/health                       - Health check
```

## Database Schema

### Entities:

1. **User** - Authentication users
2. **Member** - SACCO members with personal information
3. **SavingsAccount** - Member savings accounts
4. **Transaction** - Deposit/withdrawal transactions
5. **Loan** - Member loans
6. **LoanRepayment** - Monthly loan repayment schedules
7. **ShareCapital** - Member share investments
8. **Withdrawal** - Withdrawal requests and processing
9. **FinancialReport** - Daily financial snapshots

## Project Structure

```
src/main/java/com/sacco/sacco_system/
├── entity/                  # JPA Entity classes
│   ├── User.java
│   ├── Member.java
│   ├── SavingsAccount.java
│   ├── Transaction.java
│   ├── Loan.java
│   ├── LoanRepayment.java
│   ├── ShareCapital.java
│   ├── Withdrawal.java
│   └── FinancialReport.java
├── repository/              # Spring Data JPA Repositories
│   ├── UserRepository.java
│   ├── MemberRepository.java
│   ├── SavingsAccountRepository.java
│   ├── TransactionRepository.java
│   ├── LoanRepository.java
│   ├── LoanRepaymentRepository.java
│   ├── ShareCapitalRepository.java
│   ├── WithdrawalRepository.java
│   └── FinancialReportRepository.java
├── service/                 # Business Logic Services
│   ├── MemberService.java
│   ├── SavingsService.java
│   ├── LoanService.java
│   └── FinancialReportService.java
├── controller/              # REST API Controllers
│   ├── AuthController.java
│   ├── MemberController.java
│   ├── SavingsController.java
│   ├── LoanController.java
│   └── FinancialReportController.java
└── dto/                     # Data Transfer Objects
    ├── AuthRequest.java
    ├── AuthResponse.java
    ├── MemberDTO.java
    ├── SavingsAccountDTO.java
    └── LoanDTO.java
```

## Technology Stack

- **Framework**: Spring Boot 4.0.0
- **Language**: Java 21
- **ORM**: Spring Data JPA with Hibernate
- **Database**: MySQL 8.0+
- **Security**: Spring Security
- **Authentication**: JWT (JSON Web Tokens)
- **Build Tool**: Maven
- **Utilities**: Lombok, Validation

## Setup Instructions

### Prerequisites
- Java 21 or higher
- MySQL 8.0 or higher
- Maven 3.8.0 or higher

### 1. Clone and Navigate
```bash
cd sacco-system
```

### 2. Configure Database
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/sacco_system?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=your_password
```

### 3. Create Database
```sql
CREATE DATABASE sacco_system;
```

### 4. Build the Application
```bash
mvn clean install
```

### 5. Run the Application
```bash
mvn spring-boot:run
```

Or using Java directly:
```bash
java -jar target/sacco-system-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

## API Response Format

All endpoints return JSON responses in the following format:

**Success Response:**
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { /* response data */ },
  "count": 10
}
```

**Error Response:**
```json
{
  "success": false,
  "message": "Error description"
}
```

## Key Features Explained

### Member Registration
1. Unique member number auto-generated (MEM000001)
2. Email and phone validation
3. Automatic join date tracking

### Savings Accounts
1. Auto-generated account numbers (SAV000001)
2. Real-time balance tracking
3. Deposit/withdrawal with automatic ledger entries
4. Transaction history for audit trail

### Loan Management
1. Automatic interest calculation
2. Monthly repayment scheduling
3. Loan status workflow (PENDING → APPROVED → DISBURSED → COMPLETED)
4. Real-time balance tracking
5. Interest accrual calculation

### Financial Reporting
1. Daily automated reports
2. Real-time data aggregation
3. Net income calculation
4. Historical tracking

## Future Enhancements

- [ ] JWT authentication implementation
- [ ] Email notifications
- [ ] SMS alerts for transactions
- [ ] Mobile application
- [ ] Advanced reporting and analytics
- [ ] Member portal/dashboard
- [ ] Dividend management
- [ ] Penalty and fine tracking
- [ ] Group lending features
- [ ] Integration with payment gateways

## Database Constraints

- Members must have unique: email, phone number, ID number
- Accounts must have unique account numbers
- Loans must have unique loan numbers
- Users must have unique usernames and emails
- Foreign key constraints enforce referential integrity

## Error Handling

All endpoints include comprehensive error handling:
- 400 Bad Request - Invalid input data
- 404 Not Found - Resource not found
- 500 Internal Server Error - Server errors

## Transactions

Critical operations are wrapped in `@Transactional` annotations:
- Loan disbursements
- Loan repayments
- Deposit/withdrawal operations
- Financial report generation

## Performance Considerations

- Indexed queries on frequently searched fields
- Lazy loading for related entities (where applicable)
- BigDecimal for precise financial calculations
- Transaction management for data consistency

## Security Considerations

- Passwords should be hashed using BCrypt (to be implemented)
- JWT tokens with expiration (to be implemented)
- Input validation on all endpoints
- SQL injection prevention via JPA
- CORS configuration (to be configured)

## Testing

To run tests:
```bash
mvn test
```

## Contributing

Please ensure:
- All tests pass
- Code follows Spring Boot conventions
- Transactions are properly managed
- Error messages are meaningful

## License

This project is licensed under the MIT License.

## Support

For issues or questions, please create an issue in the repository.

---

**Version**: 0.0.1-SNAPSHOT  
**Last Updated**: December 13, 2025
