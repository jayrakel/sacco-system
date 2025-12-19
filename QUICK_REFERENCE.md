# SACCO System - Quick Reference Guide

## Project Structure Overview

### Backend Structure (Java/Spring Boot)
```
sacco-system/
└── src/main/java/com/sacco/sacco_system/
    ├── modules/
    │   ├── core/                    # 🔧 Shared infrastructure
    │   ├── member/                  # 👥 Member management
    │   ├── savings/                 # 💰 Savings accounts
    │   ├── loan/                    # 🏦 Loan management
    │   ├── finance/                 # 📊 Financial reporting
    │   ├── payment/                 # 💳 Payment processing
    │   ├── admin/                   # ⚙️  System administration
    │   ├── notification/            # 📧 Email & SMS
    │   └── reporting/               # 📈 Advanced reports
    ├── SaccoSystemApplication.java  # Main entry point
    └── config/                      # Global configuration
```

### Frontend Structure (React/Vite)
```
sacco-frontend/
├── src/
│   ├── modules/
│   │   ├── core/                    # Shared components
│   │   ├── member/                  # Member pages & components
│   │   ├── savings/                 # Savings pages & components
│   │   ├── loan/                    # Loan pages & components
│   │   ├── finance/                 # Finance & reports
│   │   ├── payment/                 # Payment components
│   │   ├── admin/                   # Admin pages
│   │   ├── reporting/               # Reporting pages
│   │   └── auth/                    # Authentication
│   ├── api/                         # API client & endpoints
│   ├── router/                      # Route configuration
│   ├── store/                       # Redux store
│   └── App.jsx                      # Root component
└── public/                          # Static assets
```

## Module Templates

### Backend Module Template

```
module/
├── domain/                          # Business logic layer
│   ├── entity/                     # JPA entities
│   ├── repository/                 # Data access
│   └── service/                    # Business logic
├── api/                            # REST layer
│   ├── controller/                 # REST endpoints
│   └── dto/                        # Request/Response DTOs
└── internal/                       # Internal communication
    ├── event/                      # Domain events
    └── listener/                   # Event handlers
```

### Frontend Module Template

```
module/
├── pages/                          # Page components
├── components/                     # Reusable components
├── hooks/                          # Custom hooks
├── services/                       # API service layer
├── store/                          # Redux slices
└── styles/                         # Module styles
```

## API Endpoints Summary

### Member Module
```
POST   /api/v1/members              - Create member
GET    /api/v1/members/{id}         - Get member by ID
GET    /api/v1/members              - List all members (paginated)
GET    /api/v1/members/active/all   - Get active members
PUT    /api/v1/members/{id}         - Update member
DELETE /api/v1/members/{id}         - Deactivate member
GET    /api/v1/members/count/active - Count active members
```

### Savings Module (to be migrated)
```
POST   /api/v1/savings/accounts     - Create account
GET    /api/v1/savings/accounts/{id} - Get account details
POST   /api/v1/savings/deposit      - Deposit funds
POST   /api/v1/savings/withdraw     - Withdraw funds
GET    /api/v1/savings              - List accounts (paginated)
```

### Loan Module (to be migrated)
```
POST   /api/v1/loans/apply          - Apply for loan
GET    /api/v1/loans/{id}           - Get loan details
GET    /api/v1/loans/member/{id}    - Get member loans
PUT    /api/v1/loans/{id}/approve   - Approve loan
PUT    /api/v1/loans/{id}/disburse  - Disburse loan
```

## Key Classes & Services

### Core Module
- `ApiResponse<T>` - Standardized response wrapper
- `PagedResponse<T>` - Pagination wrapper
- `DomainEvent` - Base class for events
- `EventPublisher` - Event publishing service
- `GlobalExceptionHandler` - Centralized error handling
- `ValidationUtils` - Common validations
- `NumberGenerator` - ID generation
- `DateUtils` - Date operations

### Member Module
- `Member` - Entity
- `MemberStatus` - Enum (ACTIVE, INACTIVE, SUSPENDED)
- `MemberRepository` - Data access
- `MemberService` - Business logic
- `MemberValidator` - Input validation
- `MemberController` - REST endpoints
- `MemberCreatedEvent` - Domain event
- `MemberStatusChangedEvent` - Domain event

## Common Development Tasks

### Create New Endpoint

1. **Create DTO** (in `module/api/dto/`)
   ```java
   @Data
   public class CreateXxxRequest {
       @NotBlank
       private String field1;
   }
   ```

2. **Create Service Method** (in `module/domain/service/`)
   ```java
   public Xxx create(CreateXxxRequest request) {
       // Validate
       // Create
       // Publish event
       return saved;
   }
   ```

3. **Create Controller Method** (in `module/api/controller/`)
   ```java
   @PostMapping
   public ResponseEntity<ApiResponse<XxxResponse>> create(
       @Valid @RequestBody CreateXxxRequest request) {
       Xxx entity = service.create(request);
       return ResponseEntity.status(HttpStatus.CREATED)
           .body(new ApiResponse<>(convertToResponse(entity), "Created"));
   }
   ```

### Create Event Listener

1. **Create Event** (in `module/internal/event/`)
   ```java
   public class XxxEvent extends DomainEvent {
       private final Xxx entity;
       
       public XxxEvent(Object source, String id, Xxx entity) {
           super(source, id);
           this.entity = entity;
       }
   }
   ```

2. **Create Listener** (in `module/internal/listener/`)
   ```java
   @Component
   public class XxxEventListener {
       @EventListener
       public void handle(XxxEvent event) {
           // Handle event
       }
   }
   ```

### Add Validation

```java
// Use ValidationUtils
ValidationUtils.validateNotEmpty(field, "fieldName");
ValidationUtils.validatePositive(amount, "amount");
ValidationUtils.validateEmail(email);
ValidationUtils.validateTrue(condition, "error message");
```

## Environment Setup

### Prerequisites
- Java 17+
- Maven 3.6+
- PostgreSQL 12+
- Node.js 18+
- npm or yarn

### Backend Setup
```bash
# Navigate to project root
cd sacco-system

# Build project
./mvnw clean install

# Run application
./mvnw spring-boot:run

# Run tests
./mvnw test
```

### Frontend Setup
```bash
# Navigate to frontend
cd sacco-frontend

# Install dependencies
npm install

# Run development server
npm run dev

# Build for production
npm run build
```

## Database Schema Considerations

### Entity Mapping Pattern
```java
@Entity
@Table(name = "table_name")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityName {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String fieldName;
    
    @Enumerated(EnumType.STRING)
    private StatusEnum status;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt;
}
```

## Error Handling

### API Response Format
```json
{
  "success": false,
  "message": "Error description",
  "data": null,
  "timestamp": "2025-12-19T10:30:00",
  "path": "/api/v1/endpoint",
  "statusCode": 400
}
```

### Exception Types
- `ApiException` - General API errors
- `ResourceNotFoundException` - Resource not found (404)
- `ValidationException` - Validation errors (400)

## Logging Best Practices

```java
@Slf4j  // Lombok annotation for logger
@Service
public class ExampleService {
    
    public void method() {
        log.info("Action started");           // Info level
        log.warn("Warning message");          // Warning level
        log.error("Error message", ex);       // Error level
        log.debug("Debug information");       // Debug level
    }
}
```

## Testing Examples

### Unit Test
```java
@SpringBootTest
@AutoConfigureMockMvc
public class ExampleServiceTests {
    
    @MockBean
    private ExampleRepository repository;
    
    @InjectMocks
    private ExampleService service;
    
    @Test
    public void testMethod() {
        // Given
        // When
        // Then
    }
}
```

### Integration Test
```java
@SpringBootTest
@AutoConfigureMockMvc
public class ExampleControllerTests {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    public void testEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/endpoint")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isCreated());
    }
}
```

## Git Workflow

### Creating Feature Branch
```bash
# Create feature branch
git checkout -b feature/module-name

# Make changes and commit
git add .
git commit -m "feat(module-name): description"

# Push branch
git push origin feature/module-name

# Create Pull Request on GitHub
```

### Commit Message Convention
```
feat(module): Add new feature
fix(module): Fix bug
refactor(module): Refactor code
test(module): Add tests
docs: Update documentation
```

## Performance Tips

1. **Use Pagination**: Always paginate list endpoints
2. **Lazy Loading**: Use @Lazy for heavy relationships
3. **Caching**: Use @Cacheable for frequently accessed data
4. **Transactions**: Use @Transactional(readOnly = true) for queries
5. **Indexes**: Add database indexes on frequently queried columns
6. **Async Processing**: Use @Async for heavy operations

## Security Best Practices

1. **Always validate input** using ValidationUtils or @Valid
2. **Check authorization** before returning sensitive data
3. **Hash passwords** using appropriate algorithms
4. **Use HTTPS** in production
5. **Rate limit** API endpoints
6. **Sanitize** user input
7. **Use CSRF tokens** for state-changing operations

## Troubleshooting Common Issues

### Issue: Bean not found
**Solution**: Check @Component/@Service/@Repository annotations and component scan

### Issue: Transaction not working
**Solution**: Ensure @Transactional is on public method, not private

### Issue: Event listener not triggered
**Solution**: Verify @EventListener annotation and event class matches parameter type

### Issue: API returns 404
**Solution**: Check @RestController/@RequestMapping paths and HTTP method

### Issue: Validation not working
**Solution**: Ensure @Valid annotation on controller method parameter

## Useful Links

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [React Documentation](https://react.dev)
- [Redux Documentation](https://redux.js.org)
- [Vite Documentation](https://vitejs.dev)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

## Contact & Support

For questions or issues with the modular architecture:
1. Check the [MODULAR_ARCHITECTURE.md](MODULAR_ARCHITECTURE.md)
2. Review [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md)
3. Check existing module implementations
4. Consult team members

---

**Last Updated:** December 2025
**Version:** 1.0
