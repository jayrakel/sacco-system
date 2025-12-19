# SACCO System - Modular Monolith Implementation Guide

## Progress Status

### ✅ Completed Components

1. **Core Module Infrastructure**
   - ✅ Exception handling (`ApiException`, `ResourceNotFoundException`, `ValidationException`, `GlobalExceptionHandler`)
   - ✅ Response DTOs (`ApiResponse`, `PagedResponse`)
   - ✅ Event infrastructure (`DomainEvent`, `EventPublisher`)
   - ✅ Utility classes (`ValidationUtils`, `NumberGenerator`, `DateUtils`)

2. **Member Module (Complete Example)**
   - ✅ Domain Layer
     - Entity: `Member`, `MemberStatus`
     - Repository: `MemberRepository`
     - Service: `MemberService`, `MemberValidator`
   - ✅ API Layer
     - Controller: `MemberController`
     - DTOs: `CreateMemberRequest`, `UpdateMemberRequest`, `MemberResponse`
   - ✅ Internal Layer
     - Events: `MemberCreatedEvent`, `MemberStatusChangedEvent`

3. **Directory Structure**
   - ✅ All module directories created
   - ✅ Proper package organization

### 🔄 Next Steps - Migration by Module

## Module Implementation Checklist

### Module 1: Savings Module ⏳
**Estimated effort:** 2-3 hours

#### Domain Layer
- [ ] Create `SavingsAccount.java` entity
- [ ] Create `SavingsProduct.java` entity
- [ ] Create `SavingsTransaction.java` entity
- [ ] Create `SavingsAccountRepository.java`
- [ ] Create `SavingsProductRepository.java`
- [ ] Create `SavingsAccountService.java`
- [ ] Create `SavingsProductService.java`
- [ ] Create `SavingsValidator.java`

#### API Layer
- [ ] Create `SavingsAccountController.java`
- [ ] Create `SavingsProductController.java`
- [ ] Create DTOs: `CreateSavingsAccountRequest`, `DepositRequest`, `WithdrawRequest`, `SavingsAccountResponse`

#### Internal Layer
- [ ] Create events: `DepositedEvent`, `WithdrawnEvent`
- [ ] Create event listener for notifications

### Module 2: Loan Module ⏳
**Estimated effort:** 3-4 hours

#### Domain Layer
- [ ] Create `Loan.java` entity
- [ ] Create `LoanProduct.java` entity
- [ ] Create `LoanRepayment.java` entity
- [ ] Create `LoanStatus.java` enum
- [ ] Create `Guarantor.java` entity
- [ ] Create repositories for all entities
- [ ] Create `LoanService.java`
- [ ] Create `LoanProductService.java`
- [ ] Create `LoanRepaymentService.java`
- [ ] Create `LoanLimitService.java`
- [ ] Create `LoanValidator.java`

#### API Layer
- [ ] Create controllers
- [ ] Create DTOs

#### Internal Layer
- [ ] Create events: `LoanAppliedEvent`, `LoanApprovedEvent`, `LoanDisbursedEvent`, `RepaymentProcessedEvent`

### Module 3: Finance Module ⏳
**Estimated effort:** 2-3 hours

#### Domain Layer
- [ ] Create `FinancialReport.java` entity
- [ ] Create `Transaction.java` entity
- [ ] Create `Charge.java` entity
- [ ] Create `ShareCapital.java` entity
- [ ] Create repositories
- [ ] Create `FinancialReportService.java`
- [ ] Create `TransactionService.java`
- [ ] Create `ChargeService.java`
- [ ] Create `ShareCapitalService.java`

#### API Layer
- [ ] Create controllers
- [ ] Create DTOs

#### Internal Layer
- [ ] Create calculator utilities
- [ ] Create event listeners

### Module 4: Payment Module ⏳
**Estimated effort:** 2 hours

#### Domain Layer
- [ ] Create `Payment.java` entity
- [ ] Create repository
- [ ] Create `PaymentService.java`
- [ ] Create `PaymentProcessor.java`
- [ ] Create `PaymentValidator.java`

#### API Layer
- [ ] Create `PaymentController.java`
- [ ] Create DTOs

#### Internal Layer
- [ ] Create events: `PaymentProcessedEvent`, `PaymentFailedEvent`

### Module 5: Admin Module ⏳
**Estimated effort:** 2 hours

#### Domain Layer
- [ ] Create `SystemSetting.java` entity
- [ ] Create `AuditLog.java` entity
- [ ] Create `Asset.java` entity
- [ ] Create repositories
- [ ] Create services

#### API Layer
- [ ] Create controllers
- [ ] Create DTOs

### Module 6: Notification Module ⏳
**Estimated effort:** 1.5 hours

#### Domain Layer
- [ ] Create `Notification.java` entity
- [ ] Create `EmailService.java`
- [ ] Create `SmsService.java`

#### API Layer
- [ ] Create `NotificationController.java`
- [ ] Create DTOs

#### Internal Layer
- [ ] Create event listeners for various domain events
- [ ] Implement email/SMS templates

### Module 7: Reporting Module ⏳
**Estimated effort:** 2 hours

#### Domain Layer
- [ ] Create `ReportService.java`
- [ ] Create `AnalyticsService.java`

#### API Layer
- [ ] Create `ReportController.java`
- [ ] Create DTOs

#### Internal Layer
- [ ] Create report generators (PDF, Excel, CSV)
- [ ] Create scheduler for automated reports

## Implementation Instructions

### For Each Module Follow This Pattern:

#### 1. Domain Layer (Core business logic)
```
1. Create entities with @Entity and @Table annotations
2. Create enums for status types
3. Create repository interfaces extending JpaRepository
4. Create service classes with business logic
5. Create validator classes for input validation
```

#### 2. API Layer (External communication)
```
1. Create controller with @RestController
2. Create request DTOs with @Valid annotations
3. Create response DTOs for data transfer
4. Map entities to DTOs in controller
```

#### 3. Internal Layer (Inter-module communication)
```
1. Create domain events extending DomainEvent
2. Create event listeners with @EventListener
3. Keep internal logic isolated
```

### Code Pattern - Service Methods

```java
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ExampleService {
    
    private final ExampleRepository repository;
    private final ExampleValidator validator;
    private final EventPublisher eventPublisher;
    
    public Example create(CreateExampleRequest request) {
        // 1. Log action
        log.info("Creating example: {}", request);
        
        // 2. Validate input
        validator.validateCreateRequest(request);
        
        // 3. Check business rules
        // (e.g., check for duplicates, constraints)
        
        // 4. Create and persist entity
        Example entity = new Example();
        // ... set properties
        Example saved = repository.save(entity);
        
        // 5. Publish domain event
        eventPublisher.publish(new ExampleCreatedEvent(this, saved.getId().toString(), saved));
        
        // 6. Return result
        return saved;
    }
}
```

### Code Pattern - Controller Methods

```java
@RestController
@RequestMapping("/api/v1/examples")
@RequiredArgsConstructor
public class ExampleController {
    
    private final ExampleService service;
    
    @PostMapping
    public ResponseEntity<ApiResponse<ExampleResponse>> create(
            @Valid @RequestBody CreateExampleRequest request) {
        Example entity = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(convertToResponse(entity), "Created successfully"));
    }
    
    private ExampleResponse convertToResponse(Example entity) {
        // Map entity to response DTO
        return new ExampleResponse();
    }
}
```

## Inter-Module Communication Examples

### Example 1: Savings Module Listens to Member Events

```java
// In savings module internal listener
@Slf4j
@Component
public class MemberEventListener {
    
    @EventListener
    public void handleMemberCreated(MemberCreatedEvent event) {
        log.info("Member created: {}, creating default savings account", event.getMember().getId());
        // Create default savings account for new member
    }
    
    @EventListener
    public void handleMemberStatusChanged(MemberStatusChangedEvent event) {
        if (event.getNewStatus() == MemberStatus.SUSPENDED) {
            // Suspend member's savings accounts
        }
    }
}
```

### Example 2: Finance Module Listens to Savings & Loan Events

```java
@Slf4j
@Component
public class FinanceEventListener {
    
    @EventListener
    public void handleDeposit(DepositedEvent event) {
        log.info("Deposit recorded: {}", event.getAggregateId());
        // Record transaction in finance module
    }
    
    @EventListener
    public void handleRepayment(RepaymentProcessedEvent event) {
        log.info("Repayment recorded: {}", event.getAggregateId());
        // Record transaction in finance module
    }
}
```

### Example 3: Notification Module Listens to All Events

```java
@Slf4j
@Component
public class NotificationEventListener {
    
    private final EmailService emailService;
    
    @EventListener
    public void handleMemberCreated(MemberCreatedEvent event) {
        // Send welcome email
        emailService.sendWelcomeEmail(event.getMember().getEmail());
    }
    
    @EventListener
    public void handleLoanApproved(LoanApprovedEvent event) {
        // Send approval notification
        emailService.sendLoanApprovalEmail(event.getLoan().getMemberId());
    }
}
```

## Database Schema Considerations

### Module-Specific Tables

```sql
-- Member Module
CREATE TABLE members (
    id BIGINT PRIMARY KEY,
    member_number VARCHAR(50) UNIQUE,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    id_number VARCHAR(50) UNIQUE,
    address VARCHAR(255),
    date_of_birth DATE,
    status VARCHAR(20),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deactivated_at TIMESTAMP
);

-- Savings Module
CREATE TABLE savings_accounts (
    id BIGINT PRIMARY KEY,
    account_number VARCHAR(50) UNIQUE,
    member_id BIGINT,
    product_id BIGINT,
    balance DECIMAL(15,2),
    created_at TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES members(id)
);

CREATE TABLE savings_products (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100),
    interest_rate DECIMAL(5,2),
    created_at TIMESTAMP
);

-- Loan Module
CREATE TABLE loans (
    id BIGINT PRIMARY KEY,
    loan_number VARCHAR(50) UNIQUE,
    member_id BIGINT,
    product_id BIGINT,
    amount DECIMAL(15,2),
    interest_rate DECIMAL(5,2),
    status VARCHAR(20),
    created_at TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES members(id)
);

-- And so on for other modules...
```

## Testing Strategy

### Unit Tests Per Module

```java
@SpringBootTest
@AutoConfigureMockMvc
public class MemberServiceTests {
    
    @MockBean
    private MemberRepository repository;
    
    @InjectMocks
    private MemberService service;
    
    @Test
    public void testCreateMember_Success() {
        // Given
        CreateMemberRequest request = new CreateMemberRequest();
        
        // When
        Member result = service.createMember(request);
        
        // Then
        assertNotNull(result);
    }
}
```

### Integration Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
public class MemberControllerIntegrationTests {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    public void testCreateMember_Integration() throws Exception {
        CreateMemberRequest request = new CreateMemberRequest();
        
        mockMvc.perform(post("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
```

## Deployment & Versioning

### API Versioning Strategy
All endpoints use `/api/v1/` prefix to allow for future versioning without breaking existing clients.

### Configuration Management
- Create `application.properties` for core settings
- Create module-specific properties files:
  - `application-member.properties`
  - `application-loan.properties`
  - `application-savings.properties`
  - etc.

### Module Features in application.properties
```properties
# Enable/Disable modules
module.member.enabled=true
module.savings.enabled=true
module.loan.enabled=true
module.finance.enabled=true
module.payment.enabled=true
module.admin.enabled=true
module.notification.enabled=true
module.reporting.enabled=true

# Module-specific settings
member.email-verification.enabled=true
loan.auto-approval.enabled=false
savings.minimum-balance=1000
```

## Monitoring & Logging

### Structured Logging Approach

```java
// Using logback.xml configuration
<logger name="com.sacco.sacco_system.modules.member" level="INFO"/>
<logger name="com.sacco.sacco_system.modules.loan" level="INFO"/>
<logger name="com.sacco.sacco_system.modules.savings" level="INFO"/>
```

### Metrics Collection

```java
// Add Micrometer metrics
@Service
public class ExampleService {
    private final MeterRegistry meterRegistry;
    
    public void processExample() {
        // Record metric
        meterRegistry.counter("example.processed").increment();
    }
}
```

## Security Considerations

### Module-Level Security

```java
// In security config
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests((authz) -> authz
                .requestMatchers("/api/v1/members/**").hasRole("MEMBER_ADMIN")
                .requestMatchers("/api/v1/loans/**").hasRole("LOAN_ADMIN")
                .requestMatchers("/api/v1/savings/**").hasRole("SAVINGS_ADMIN")
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
```

## Frontend Integration

### API Endpoint Mapping

```javascript
// Frontend: sacco-frontend/src/api.js
const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api/v1';

export const memberAPI = {
    create: (data) => axios.post(`${API_BASE_URL}/members`, data),
    getById: (id) => axios.get(`${API_BASE_URL}/members/${id}`),
    getAll: (page, size) => axios.get(`${API_BASE_URL}/members`, {params: {page, size}}),
};

export const savingsAPI = {
    createAccount: (data) => axios.post(`${API_BASE_URL}/savings/accounts`, data),
    deposit: (data) => axios.post(`${API_BASE_URL}/savings/deposit`, data),
    withdraw: (data) => axios.post(`${API_BASE_URL}/savings/withdraw`, data),
};

// ... similar for other modules
```

## Migration Timeline

### Phase 1: Core Infrastructure ✅
- Duration: 2-3 days
- Create core module utilities
- Set up exception handling
- Initialize event system

### Phase 2: Migrate Key Modules 🔄
- Duration: 1-2 weeks
- Member Module
- Savings Module
- Loan Module
- Finance Module

### Phase 3: Support Modules
- Duration: 1 week
- Payment Module
- Admin Module
- Notification Module
- Reporting Module

### Phase 4: Testing & Optimization
- Duration: 1-2 weeks
- Unit tests for each module
- Integration tests
- Performance optimization
- Documentation

### Phase 5: Deployment
- Duration: 2-3 days
- Database migration
- Backward compatibility testing
- Gradual rollout

## Success Metrics

- [ ] All modules follow modular structure
- [ ] No circular dependencies between modules
- [ ] Core module has <1% of business logic
- [ ] Each module has >80% test coverage
- [ ] API response time <500ms for 95% of requests
- [ ] Zero critical security issues
- [ ] All modules independently deployable (in theory)
- [ ] Event communication working across all modules
- [ ] Comprehensive API documentation
- [ ] Easy onboarding for new developers

## Troubleshooting Guide

### Common Issues & Solutions

**Issue 1: Circular Dependencies**
```
Problem: Module A depends on Module B, Module B depends on Module A
Solution: 
  - Use events for communication instead of direct calls
  - Move shared code to Core module
  - Reconsider module boundaries
```

**Issue 2: Event Listener Not Triggered**
```
Problem: Event published but listener not called
Solution:
  - Ensure @EventListener annotation is present
  - Verify @Component or @Service is on listener class
  - Check if event class matches the parameter type
  - Ensure ApplicationEventPublisher is used
```

**Issue 3: Database Transaction Issues**
```
Problem: Data not persisted after service method
Solution:
  - Ensure @Transactional on service methods
  - Check for rollback due to exceptions
  - Verify cascade settings on relationships
```

**Issue 4: API Endpoint Not Found**
```
Problem: 404 error on API call
Solution:
  - Verify @RestController annotation
  - Check @RequestMapping path matches
  - Ensure controller is in component scan path
  - Check spelling and HTTP method
```

## Future Enhancements

1. **Microservices Migration**: Each module can be extracted as independent microservice
2. **API Gateway**: Add Kong or Spring Cloud Gateway for centralized routing
3. **Event Store**: Implement event sourcing for audit trail
4. **CQRS Pattern**: Separate read and write models for better performance
5. **Message Queue**: Add RabbitMQ/Kafka for async processing
6. **Service Mesh**: Implement Istio for advanced traffic management
7. **GraphQL API**: Add alongside REST for flexible querying
8. **Multi-tenancy**: Support multiple SACCO organizations

---

**Last Updated:** December 2025
**Status:** In Progress - Phase 1 Complete ✅
