## Audit logging

- Annotation: `@Loggable(action, category)`
- action: UPPER_SNAKE_CASE (e.g., APPROVE_LOAN)
- category: controlled values (to be enforced via enum)
- Implemented by: `AuditAspect`
- Persists to: `AuditLog` via `AuditService`