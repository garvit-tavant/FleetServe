# FleetServe – Copilot Instructions

## Project Overview

FleetServe is a **fleet maintenance and workshop management platform** built as a monolithic Spring Boot backend (designed to evolve into microservices). It manages:
- Vehicle asset registration & maintenance scheduling
- Workshop capacity (bays, technicians, calendars)
- Work order lifecycle
- Spare-parts inventory (append-only ledger)
- SLA tracking, MTTR, and cost reporting

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.1.1 |
| Security | Spring Security + JWT (stateless) |
| Persistence | Spring Data JPA + PostgreSQL |
| Migrations | Flyway |
| Validation | Jakarta Bean Validation |
| Build | Maven (mvnw wrapper) |

**Database:** PostgreSQL running on `localhost:5432/fleetserve_db`  
**Server port:** `8080`

## Package Structure

Base package: `com.example.backend`

```
com.example.backend
├── BackendApplication.java
├── common/
│   └── exception/           # shared exception types
├── SecurityService/
│   ├── config/              # SecurityConfig, CORS, filter chain
│   ├── controller/          # AuthController  (/api/auth)
│   ├── dto/                 # LoginRequest, RegisterRequest, AuthResponse, AppUserPrincipal
│   ├── entity/              # AppUser, Role
│   ├── exception/           # AuthenticationFailedException, DuplicateUsernameException
│   ├── repository/          # UserRepository, RoleRepository
│   ├── security/            # JwtService (issue / validate tokens)
│   └── service/             # AuthService → AuthServiceImpl, AppUserDetailsService
└── AssetManagamentService/  # NOTE: intentional typo in folder name
    ├── controller/          # AssetController, AssetClassController, MaintenancePlanController, …
    ├── dto/                 # asset/, assetclass/, maintenanceplan/, odometer/ sub-packages
    ├── entity/              # Asset, AssetClass, MaintenancePlan, AssetClassPlan, OdometerReading
    ├── exception/           # ResourceNotFoundException, DuplicateResourceException,
    │                        # BusinessValidationException, AssetRetirementBlockedException
    ├── mapper/              # AssetManagementMapper
    ├── port/                # AssetRetirementBlockerPort (interface), CurrentUserProvider (interface)
    │                        # CurrentUserProviderImpl
    ├── repository/          # AssetRepository, AssetClassRepository, AssetClassPlanRepository,
    │                        # OdometerReadingRepository, MaintenancePlanRepository,
    │                        # DueMaintenanceRepository (projection-based)
    ├── service/             # interfaces + impl/ sub-package
    ├── source/              # enums/constants for asset sources
    └── status/              # AssetStatus enum (ACTIVE, RETIRED, …)
```

## Domain Services (Planned / Partially Implemented)

1. **Asset Management** – register/retire/reinstate assets, update odometer, calculate maintenance due
2. **Capacity & Scheduling** – workshops, bays, technicians, working calendars, bookings
3. **Work Order** – state machine: `SCHEDULED → IN_PROGRESS → AWAITING_PARTS → COMPLETED / CANCELLED`
4. **Inventory** – append-only ledger (`inventory_movement`), stock as a DB view
5. **Execution** – labour recording, findings, part usage on work orders
6. **SLA & Breakdown** – raise breakdown, apply/pause/resume SLA, compliance & MTTR metrics

## Key Conventions & Patterns

- **Controller → Service (interface) → ServiceImpl** layering; always code to the interface.
- **Constructor injection** only – no `@Autowired` on fields.
- `@Transactional(readOnly = true)` on service class; `@Transactional` overrides on write methods.
- **Mapper class** (`AssetManagementMapper`) handles all entity ↔ DTO conversions.
- **Port interfaces** (`AssetRetirementBlockerPort`, `CurrentUserProvider`) for cross-service dependencies.
- **Flyway migrations** in `src/main/resources/db/migration/` – always add new scripts as `V{n}__description.sql`.
- DTOs are plain Java classes (no Lombok) with manual getters/setters.
- Entities are plain JPA (`@Entity`) classes – no Lombok.
- JWT is stateless; the `JwtService` issues and validates Bearer tokens.
- Default role assigned at registration: `TECHNICIAN`.

## REST API Summary

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login, returns JWT |
| GET | `/api/auth/ping` | Auth check (Bearer required) |
| POST | `/api/assets` | Register asset |
| GET | `/api/assets/{id}` | Get asset detail |
| GET | `/api/assets` | List all assets |
| PATCH | `/api/assets/{id}/retire` | Retire asset |
| PATCH | `/api/assets/{id}/reinstate` | Reinstate asset |
| POST | `/api/asset-classes` | Create asset class |
| GET | `/api/asset-classes/{id}` | Get asset class |
| GET | `/api/asset-classes` | List all asset classes |

## Validation Layers (apply to every new feature)

1. **Request DTO** – `@NotBlank`, `@NotNull`, `@Size`, `@Positive`, `@PastOrPresent`, etc.
2. **Service layer** – uniqueness, referenced entity existence, business rules (odometer monotonicity, state transitions, ownership).
3. **Database** – primary keys, foreign keys, unique constraints, CHECK constraints enforced by Flyway migrations.

## Database Migrations (Flyway)

| File | Content |
|---|---|
| V1 | Identity & access (app_user, role, user_role) |
| V2 | Workshop resources |
| V4 | Maintenance policy & assets |
| V5 | SLA & breakdown |
| V6 | Booking |
| V7 | Work orders |
| V8 | Inventory |
| V9 | Append-only protection triggers |
| V10 | Required indexes |
| V11 | Reference & fixture data |
| V12 | Replace initial roles |

## Important Notes

- The folder is named `AssetManagamentService` (single 'e' in Management) — match this spelling in all imports.
- `CurrentUserProviderImpl.getCurrentUserId()` is currently a stub — needs Spring Security context wiring.
- `DueMaintenanceRepository` uses a custom `@Query` projection (`DueMaintenanceProjection`) instead of `JpaRepository`.
- The JWT secret in `application.properties` must be Base64-encoded.

## Import Guidelines

Always include all required imports when generating or completing Java code. Follow these rules:

- **Jakarta EE**: Use `jakarta.*` (not `javax.*`) — e.g., `jakarta.servlet.FilterChain`, `jakarta.servlet.ServletException`, `jakarta.servlet.http.HttpServletRequest`, `jakarta.servlet.http.HttpServletResponse`
- **Spring Security**: Common imports needed in filters/controllers:
  - `org.springframework.security.core.context.SecurityContextHolder`
  - `org.springframework.security.authentication.UsernamePasswordAuthenticationToken`
  - `org.springframework.security.core.GrantedAuthority`
  - `org.springframework.security.core.authority.SimpleGrantedAuthority`
  - `org.springframework.web.filter.OncePerRequestFilter`
- **JWT (jjwt library)**: Use `io.jsonwebtoken.Claims`, `io.jsonwebtoken.Jwts` — do NOT import `io.jsonwebtoken.Jwt` unless explicitly needed
- **Java standard**: Always import `java.io.IOException` in any class that overrides servlet filter methods
- **Never leave unused imports** — remove any import that is not referenced in the code
- When suggesting a method override (e.g., `doFilterInternal`), always include the full correct signature with the `throws ServletException, IOException` clause

## Inline Comment Q&A

When the user writes a question inside a code comment (e.g., `// agent: why is this needed?` or `// q: what does this do?`), respond by replacing or appending to that comment with a concise answer — inline, in the same file, as a code comment. Keep the answer brief and context-aware. Mark it clearly, e.g., `// ans`. Do not open a chat reply; answer directly in the file.
