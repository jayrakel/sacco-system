# SACCO System - Modular Monolith Architecture

## Overview
This document outlines the modular monolith structure for the SACCO Management System. The system is organized into loosely-coupled, feature-focused modules that can be developed, tested, and deployed independently while running as a single application.

## Module Structure

### 1. **Core Module** (`core/`)
**Purpose:** Shared infrastructure, utilities, and cross-cutting concerns

```
core/
├── config/
│   ├── DatabaseConfig.java
│   ├── SecurityConfig.java
│   └── WebConfig.java
├── exception/
│   ├── ApiException.java
│   ├── GlobalExceptionHandler.java
│   └── ErrorResponse.java
├── util/
│   ├── DateUtils.java
│   ├── ValidationUtils.java
│   └── NumberGenerator.java
├── dto/
│   ├── ApiResponse.java
│   └── PagedResponse.java
├── annotation/
│   ├── Auditable.java
│   └── RateLimit.java
└── aspect/
    ├── LoggingAspect.java
    ├── PerformanceAspect.java
    └── AuditingAspect.java
```

### 2. **Member Module** (`member/`)
**Purpose:** Member management, profiles, and lifecycle

```
member/
├── domain/
│   ├── entity/
│   │   ├── Member.java
│   │   └── MemberStatus.java
│   ├── repository/
│   │   └── MemberRepository.java
│   └── service/
│       ├── MemberService.java
│       └── MemberValidator.java
├── api/
│   ├── controller/
│   │   └── MemberController.java
│   └── dto/
│       ├── CreateMemberRequest.java
│       ├── UpdateMemberRequest.java
│       └── MemberResponse.java
└── internal/
    ├── event/
    │   ├── MemberCreatedEvent.java
    │   └── MemberStatusChangedEvent.java
    └── listener/
        └── MemberEventListener.java
```

### 3. **Savings Module** (`savings/`)
**Purpose:** Savings accounts, deposits, and withdrawals

```
savings/
├── domain/
│   ├── entity/
│   │   ├── SavingsAccount.java
│   │   ├── SavingsProduct.java
│   │   └── SavingsTransaction.java
│   ├── repository/
│   │   ├── SavingsAccountRepository.java
│   │   └── SavingsProductRepository.java
│   └── service/
│       ├── SavingsAccountService.java
│       ├── SavingsProductService.java
│       └── SavingsValidator.java
├── api/
│   ├── controller/
│   │   ├── SavingsAccountController.java
│   │   └── SavingsProductController.java
│   └── dto/
│       ├── CreateSavingsAccountRequest.java
│       ├── DepositRequest.java
│       ├── WithdrawRequest.java
│       └── SavingsAccountResponse.java
└── internal/
    ├── event/
    │   ├── DepositedEvent.java
    │   └── WithdrawnEvent.java
    └── listener/
        └── SavingsEventListener.java
```

### 4. **Loan Module** (`loan/`)
**Purpose:** Loan products, applications, approvals, and management

```
loan/
├── domain/
│   ├── entity/
│   │   ├── Loan.java
│   │   ├── LoanProduct.java
│   │   ├── LoanRepayment.java
│   │   ├── LoanStatus.java
│   │   └── Guarantor.java
│   ├── repository/
│   │   ├── LoanRepository.java
│   │   ├── LoanProductRepository.java
│   │   └── LoanRepaymentRepository.java
│   └── service/
│       ├── LoanService.java
│       ├── LoanProductService.java
│       ├── LoanRepaymentService.java
│       ├── LoanLimitService.java
│       └── LoanValidator.java
├── api/
│   ├── controller/
│   │   ├── LoanController.java
│   │   └── LoanProductController.java
│   └── dto/
│       ├── ApplyLoanRequest.java
│       ├── LoanResponse.java
│       ├── RepaymentScheduleResponse.java
│       └── CreateLoanProductRequest.java
└── internal/
    ├── event/
    │   ├── LoanAppliedEvent.java
    │   ├── LoanApprovedEvent.java
    │   ├── LoanDisbursedEvent.java
    │   └── RepaymentProcessedEvent.java
    └── listener/
        └── LoanEventListener.java
```

### 5. **Finance Module** (`finance/`)
**Purpose:** Financial reporting, accounting, and compliance

```
finance/
├── domain/
│   ├── entity/
│   │   ├── FinancialReport.java
│   │   ├── Transaction.java
│   │   ├── Charge.java
│   │   └── ShareCapital.java
│   ├── repository/
│   │   ├── FinancialReportRepository.java
│   │   ├── TransactionRepository.java
│   │   └── ShareCapitalRepository.java
│   └── service/
│       ├── FinancialReportService.java
│       ├── TransactionService.java
│       ├── ChargeService.java
│       └── ShareCapitalService.java
├── api/
│   ├── controller/
│   │   ├── FinancialReportController.java
│   │   ├── TransactionController.java
│   │   └── ShareCapitalController.java
│   └── dto/
│       ├── FinancialReportResponse.java
│       └── TransactionResponse.java
└── internal/
    ├── calculator/
    │   ├── FinancialCalculator.java
    │   └── InterestCalculator.java
    └── event/
        └── FinancialReportGeneratedEvent.java
```

### 6. **Payment Module** (`payment/`)
**Purpose:** Payment processing, reconciliation, and management

```
payment/
├── domain/
│   ├── entity/
│   │   └── Payment.java
│   ├── repository/
│   │   └── PaymentRepository.java
│   └── service/
│       ├── PaymentService.java
│       ├── PaymentProcessor.java
│       └── PaymentValidator.java
├── api/
│   ├── controller/
│   │   └── PaymentController.java
│   └── dto/
│       ├── ProcessPaymentRequest.java
│       └── PaymentResponse.java
└── internal/
    ├── event/
    │   ├── PaymentProcessedEvent.java
    │   └── PaymentFailedEvent.java
    └── listener/
        └── PaymentEventListener.java
```

### 7. **Admin Module** (`admin/`)
**Purpose:** System administration, settings, and audit

```
admin/
├── domain/
│   ├── entity/
│   │   ├── SystemSetting.java
│   │   ├── AuditLog.java
│   │   ├── Asset.java
│   │   └── UserRole.java
│   ├── repository/
│   │   ├── SystemSettingRepository.java
│   │   ├── AuditLogRepository.java
│   │   └── AssetRepository.java
│   └── service/
│       ├── SystemSettingService.java
│       ├── AuditService.java
│       ├── AssetService.java
│       └── AdminValidator.java
├── api/
│   ├── controller/
│   │   ├── SystemSettingController.java
│   │   ├── AuditController.java
│   │   └── AssetController.java
│   └── dto/
│       ├── SystemSettingRequest.java
│       ├── AuditLogResponse.java
│       └── AssetResponse.java
└── internal/
    ├── event/
    │   └── SettingChangedEvent.java
    └── listener/
        └── AdminEventListener.java
```

### 8. **Notification Module** (`notification/`)
**Purpose:** Email, SMS, and other notifications

```
notification/
├── domain/
│   ├── service/
│   │   ├── EmailService.java
│   │   ├── SmsService.java
│   │   └── NotificationStrategy.java
│   └── entity/
│       └── Notification.java
├── api/
│   ├── controller/
│   │   └── NotificationController.java
│   └── dto/
│       └── NotificationResponse.java
└── internal/
    ├── template/
    │   ├── EmailTemplateEngine.java
    │   └── templates/
    │       ├── member_welcome.html
    │       ├── loan_approved.html
    │       └── payment_received.html
    └── listener/
        └── NotificationEventListener.java
```

### 9. **Reporting Module** (`reporting/`)
**Purpose:** Advanced reporting and analytics

```
reporting/
├── domain/
│   └── service/
│       ├── ReportService.java
│       ├── ReportGenerator.java
│       └── AnalyticsService.java
├── api/
│   ├── controller/
│   │   └── ReportController.java
│   └── dto/
│       └── ReportResponse.java
└── internal/
    ├── generator/
    │   ├── PDFReportGenerator.java
    │   ├── ExcelReportGenerator.java
    │   └── CSVReportGenerator.java
    └── scheduler/
        └── ReportScheduler.java
```

### 10. **Authentication Module** (`auth/`)
**Purpose:** Authentication, authorization, and security

```
auth/
├── domain/
│   ├── entity/
│   │   └── User.java
│   ├── repository/
│   │   └── UserRepository.java
│   └── service/
│       ├── AuthService.java
│       ├── TokenProvider.java
│       └── RolePermissionService.java
├── api/
│   ├── controller/
│   │   └── AuthController.java
│   └── dto/
│       ├── LoginRequest.java
│       ├── RegisterRequest.java
│       └── TokenResponse.java
└── internal/
    ├── filter/
    │   └── JwtAuthenticationFilter.java
    ├── provider/
    │   └── JwtTokenProvider.java
    └── event/
        └── UserAuthenticatedEvent.java
```

## Key Principles

### 1. **Module Independence**
- Each module is self-contained with its own domain logic
- Modules communicate through well-defined APIs
- Internal implementation details are hidden

### 2. **Layered Within Modules**
Each module follows this structure:
- **Domain Layer:** Core business logic, entities, repositories, and services
- **API Layer:** Controllers and DTOs for external communication
- **Internal Layer:** Events, listeners, and internal utilities

### 3. **Event-Driven Communication**
- Modules publish domain events for significant state changes
- Other modules listen to events they care about
- Decouples modules and prevents direct dependencies

### 4. **Dependency Direction**
```
Core Module
    ↑
    │ (depends on)
    │
Feature Modules (Member, Savings, Loan, Finance, etc.)
```

### 5. **Database Strategy**
- Shared database with module-specific schemas/tables
- Each module owns its entities
- Use foreign keys carefully to maintain loose coupling

### 6. **API Boundaries**
- Public: Everything in `api/` package is public
- Internal: Everything in `internal/` package is private
- Domain: Services in `domain/service/` should not be exposed directly

## Communication Patterns

### 1. **Inter-Module Communication**
```
Module A (Publisher)
    ↓
DomainEvent
    ↓
EventPublisher
    ↓
Module B EventListener (Subscriber)
    ↓
Module B Service (handles event)
```

### 2. **REST API Communication**
- Direct HTTP calls between modules in same process
- Useful for synchronous operations
- Use circuit breakers for resilience

### 3. **Async Processing**
- Use ApplicationEvent for async processing
- Spring's @Async annotation for background tasks
- Consider adding message queue (RabbitMQ/Kafka) if needed

## File Structure Migration

```
OLD STRUCTURE:
├── controller/
│   ├── MemberController.java
│   ├── LoanController.java
│   └── ...
├── entity/
│   ├── Member.java
│   ├── Loan.java
│   └── ...
├── service/
│   ├── MemberService.java
│   ├── LoanService.java
│   └── ...
└── repository/
    ├── MemberRepository.java
    ├── LoanRepository.java
    └── ...

NEW STRUCTURE:
├── core/
│   ├── config/
│   ├── exception/
│   └── util/
├── member/
│   ├── domain/
│   ├── api/
│   └── internal/
├── loan/
│   ├── domain/
│   ├── api/
│   └── internal/
├── savings/
│   ├── domain/
│   ├── api/
│   └── internal/
└── ...
```

## Benefits

1. **Scalability:** Easy to add new modules without affecting existing ones
2. **Maintainability:** Clear separation of concerns makes code easier to understand
3. **Testability:** Each module can be tested independently
4. **Reusability:** Module services can be reused by other modules
5. **Team Scaling:** Different teams can work on different modules independently
6. **Future Microservices:** Can be easily extracted as separate microservices

## Implementation Steps

1. Create new directory structure under `com.sacco.sacco_system`
2. Move entities to respective modules
3. Move repositories to respective modules
4. Move services to respective modules
5. Move controllers to respective modules
6. Create event classes in each module
7. Create event listeners for cross-module communication
8. Update dependencies and imports
9. Test each module
10. Create module documentation

## Next Steps

1. Review and approve architecture
2. Begin implementation with Core module
3. Migrate each feature module sequentially
4. Add event-driven communication
5. Add comprehensive tests
6. Document module APIs
