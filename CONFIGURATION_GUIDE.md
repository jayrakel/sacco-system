# SACCO System - Configuration & Setup Guide

## Environment Configuration

### application.properties (Core Configuration)

```properties
# ============================================================
# Spring Boot Application Configuration
# ============================================================

# Application
spring.application.name=sacco-system
server.port=8080
server.servlet.context-path=/

# ============================================================
# Database Configuration
# ============================================================

spring.datasource.url=jdbc:postgresql://localhost:5432/sacco_db
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate Configuration
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true

# ============================================================
# Module Configuration
# ============================================================

# Enable/Disable modules as needed
module.member.enabled=true
module.savings.enabled=true
module.loan.enabled=true
module.finance.enabled=true
module.payment.enabled=true
module.admin.enabled=true
module.notification.enabled=true
module.reporting.enabled=true

# ============================================================
# Module-Specific Settings
# ============================================================

# Member Module
member.email-verification.enabled=false
member.auto-activation=true

# Loan Module
loan.auto-approval.enabled=false
loan.max-interest-rate=36.0
loan.min-loan-amount=1000
loan.max-loan-amount=1000000

# Savings Module
savings.minimum-balance=1000
savings.interest-calculation-frequency=MONTHLY

# Finance Module
finance.report-generation-time=23:59
finance.fiscal-year-start=01-01

# Notification Module
notification.email.enabled=true
notification.sms.enabled=false
notification.queue-size=100

# Admin Module
admin.audit-logging.enabled=true
admin.log-retention-days=365

# ============================================================
# Security Configuration
# ============================================================

# JWT Configuration
security.jwt.secret=your-secret-key-here-min-32-characters
security.jwt.expiration=86400000
security.jwt.refresh-expiration=604800000

# Authentication
security.auth.attempts-limit=5
security.auth.lockout-duration=900000

# ============================================================
# Logging Configuration
# ============================================================

logging.level.root=INFO
logging.level.com.sacco.sacco_system=INFO
logging.level.org.springframework.web=INFO
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %logger{36} - %msg%n
logging.file.name=logs/application.log
logging.file.max-size=10MB
logging.file.max-history=10

# ============================================================
# Jackson Configuration
# ============================================================

spring.jackson.default-property-inclusion=non_null
spring.jackson.serialization.write-dates-as-timestamps=false
spring.jackson.serialization.indent-output=false

# ============================================================
# Mail Configuration
# ============================================================

spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000

# ============================================================
# Actuator Configuration
# ============================================================

management.endpoints.web.exposure.include=health,metrics,env,prometheus
management.endpoint.health.show-details=when-authorized
management.metrics.export.prometheus.enabled=true

# ============================================================
# Jackson DateFormat
# ============================================================

spring.jackson.serialization.write-dates-as-timestamps=false
spring.jackson.time-zone=UTC
```

### application-dev.properties (Development)

```properties
# Development Profile Configuration

spring.profiles.active=dev

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/sacco_db_dev
spring.jpa.hibernate.ddl-auto=update

# Logging
logging.level.root=DEBUG
logging.level.com.sacco.sacco_system=DEBUG
logging.level.org.springframework=DEBUG

# Module Settings
module.notification.enabled=true
notification.email.enabled=false

# Actuator
management.endpoints.web.exposure.include=*
```

### application-prod.properties (Production)

```properties
# Production Profile Configuration

spring.profiles.active=prod

# Database
spring.datasource.url=jdbc:postgresql://prod-db-host:5432/sacco_db
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Logging
logging.level.root=WARN
logging.level.com.sacco.sacco_system=INFO

# Security
security.jwt.secret=${JWT_SECRET}

# Actuator
management.endpoints.web.exposure.include=health,metrics
management.endpoint.health.show-details=when-authorized
```

## Database Schema Setup

### PostgreSQL Initial Setup

```sql
-- Create Database
CREATE DATABASE sacco_db
    WITH
    ENCODING = 'UTF8'
    LOCALE = 'en_US.UTF-8'
    TEMPLATE = template0;

-- Connect to database
\c sacco_db

-- Create Schema
CREATE SCHEMA sacco_system;

-- Set Search Path
ALTER ROLE postgres SET search_path = public,sacco_system;

-- Create Audit Table
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(255) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    old_values TEXT,
    new_values TEXT,
    user_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Index
CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at);
```

### Flyway Migration Setup

Create `src/main/resources/db/migration/` directory with SQL files:

```sql
-- V1__initial_schema.sql
CREATE TABLE members (
    id BIGSERIAL PRIMARY KEY,
    member_number VARCHAR(50) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20) NOT NULL,
    id_number VARCHAR(50) UNIQUE NOT NULL,
    address VARCHAR(255),
    date_of_birth DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deactivated_at TIMESTAMP
);

CREATE INDEX idx_member_email ON members(email);
CREATE INDEX idx_member_status ON members(status);
CREATE INDEX idx_member_created_at ON members(created_at);
```

## Maven Configuration (pom.xml)

### Add to pom.xml

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.0.0</version>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>

<dependency>
    <groupId>com.auth0</groupId>
    <artifactId>java-jwt</artifactId>
    <version>4.4.0</version>
</dependency>

<!-- Testing -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

## Frontend Configuration (.env)

### .env (Development)

```bash
VITE_API_URL=http://localhost:8080
VITE_ENV=development
VITE_LOG_LEVEL=debug
VITE_APP_NAME=SACCO System
VITE_APP_VERSION=1.0.0
```

### .env.production

```bash
VITE_API_URL=https://api.sacco.prod
VITE_ENV=production
VITE_LOG_LEVEL=error
VITE_APP_NAME=SACCO System
VITE_APP_VERSION=1.0.0
```

## Docker Configuration

### Dockerfile (Backend)

```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/sacco-system-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod

CMD ["java", "-jar", "app.jar"]
```

### docker-compose.yml

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: sacco_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - sacco-network

  app:
    build: .
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/sacco_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
    ports:
      - "8080:8080"
    depends_on:
      - postgres
    networks:
      - sacco-network

volumes:
  postgres_data:

networks:
  sacco-network:
    driver: bridge
```

## IDE Configuration

### VS Code settings.json

```json
{
  "java.configuration.updateBuildConfiguration": "automatic",
  "[java]": {
    "editor.defaultFormatter": "redhat.java",
    "editor.formatOnSave": true
  },
  "java.format.settings.url": "./.vscode/java-formatter.xml",
  "editor.codeActionsOnSave": {
    "source.organizeImports": true
  },
  "java.saveActions.organizeImports": true,
  "files.exclude": {
    "**/node_modules": true,
    "**/target": true
  }
}
```

### IntelliJ IDEA Configuration

1. **Code Style Settings**
   - Go to Settings → Editor → Code Style → Java
   - Set indentation to 4 spaces
   - Enable "Arrange by modifier"

2. **Inspection Settings**
   - Enable all inspections for better code quality
   - Fix warnings as they appear

3. **Run Configurations**
   ```
   Main class: com.sacco.sacco_system.SaccoSystemApplication
   VM options: -Dspring.profiles.active=dev
   ```

## Testing Configuration

### application-test.properties

```properties
spring.test.mockmvc.print=true
spring.jpa.hibernate.ddl-auto=create-drop
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
```

### Test Base Class

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BaseTest {
    
    @Autowired
    protected MockMvc mockMvc;
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    protected String asJsonString(Object obj) throws JsonProcessingException {
        return objectMapper.writeValueAsString(obj);
    }
}
```

## CI/CD Configuration

### GitHub Actions (.github/workflows/build.yml)

```yaml
name: Build and Test

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15-alpine
        env:
          POSTGRES_DB: sacco_db
          POSTGRES_PASSWORD: postgres
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432

    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Build with Maven
      run: ./mvnw clean install -DskipTests
    
    - name: Run tests
      run: ./mvnw test
    
    - name: Upload coverage
      uses: codecov/codecov-action@v3
```

## Security Configuration

### SecurityConfig.java

```java
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests((authz) -> authz
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/v1/members/**").hasRole("MEMBER_ADMIN")
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(new JwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

## Monitoring & Observability

### Micrometer Configuration

```properties
# Metrics
management.endpoints.web.exposure.include=health,metrics,prometheus
management.metrics.export.prometheus.enabled=true
management.endpoint.health.probes.enabled=true
management.health.livenessState.enabled=true
management.health.readinessState.enabled=true
```

### Logging with Logback

Create `src/main/resources/logback-spring.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProperty name="LOG_FILE" source="logging.file.name" defaultValue="logs/app.log"/>
    
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_FILE}</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>logs/app-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>10MB</maxFileSize>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>
    
    <logger name="com.sacco.sacco_system" level="INFO"/>
    <logger name="org.springframework" level="INFO"/>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

## Performance Tuning

### Connection Pool Configuration

```properties
# HikariCP Configuration
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

### Caching Configuration

```properties
spring.cache.type=redis
spring.redis.host=localhost
spring.redis.port=6379
spring.cache.redis.time-to-live=600000
```

---

**Last Updated:** December 2025
**Configuration Version:** 1.0
