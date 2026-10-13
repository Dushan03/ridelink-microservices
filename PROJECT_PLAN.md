# RideLink – University Group Assignment Project Plan

## Purpose

RideLink is a backend-only ride-sharing platform built as a university group assignment.
It demonstrates microservices architecture, REST API design, JWT security, and interservice
communication using Java 17 and Spring Boot.

---

## Team Ownership

| Member | Service | Port |
|--------|---------|------|
| Member 1 | Account Service | 8081 |
| Member 2 | Driver & Vehicle Service | 8082 |
| Member 3 | Ride Management Service | 8083 |
| Member 4 | Fare & Payment Service | 8084 |

---

## Monorepo Structure

```
ridelink-microservices/
├── account-service/           # Member 1
├── driver-vehicle-service/    # Member 2
├── ride-service/              # Member 3
├── fare-payment-service/      # Member 4
├── frontend/                  # Optional – Phase 9 only
├── postman/
│   ├── RideLink_Collection.json
│   └── RideLink_Environment.json
├── docs/
│   ├── api-contracts.md
│   ├── architecture.md
│   ├── security.md
│   ├── testing.md
│   └── individual-contributions-template.md
├── .github/
│   └── workflows/
│       └── ci.yml
├── .env.example
├── .gitignore
└── README.md
```

---

## Technology Stack

| Concern | Choice | Reason |
|---------|--------|--------|
| Language | Java 17 | Assignment requirement |
| Framework | Spring Boot 3.x | Industry standard, rich ecosystem |
| Security | Spring Security + JWT (jjwt) | Stateless, fits microservices |
| Persistence | Spring Data JPA + H2 (file-based) | Zero-install, per-service databases |
| HTTP Client | Spring RestClient | Modern, built into Spring 6 |
| API Docs | Springdoc OpenAPI 2.x | Compatible with Spring Boot 3 |
| Testing | JUnit 5 + Mockito + Spring Boot Test | Assignment requirement |
| Build | Maven Wrapper (mvnw) | Reproducible builds |
| CI | GitHub Actions | Assignment requirement |

---

## Implementation Phases

| Phase | Work | Gate |
|-------|------|------|
| 0 | Workspace inspection, PROJECT_PLAN.md, docs/api-contracts.md | CONTINUE |
| 1 | Monorepo structure, four minimal Spring Boot projects, H2, health, Swagger | CONTINUE |
| 2 | Full Fare & Payment Service | CONTINUE |
| 3 | Full Account Service (register, BCrypt, JWT) | CONTINUE |
| 4 | Full Driver & Vehicle Service | CONTINUE |
| 5 | Full Ride Service + RestClient to Driver & Fare services | CONTINUE |
| 6 | JWT validation + role/ownership checks across all services | CONTINUE |
| 7 | Postman integrated workflow + negative cases | CONTINUE |
| 8 | GitHub Actions CI + final README + technical docs | CONTINUE |
| 9 | Optional frontend (HTML/CSS/Vanilla JS) – only on request | CONTINUE |

---

## Database Configuration

Each service uses a **separate** file-based H2 database. These files must never be shared.

| Service | H2 File Path |
|---------|-------------|
| Account | `./data/account-db` |
| Driver | `./data/driver-db` |
| Ride | `./data/ride-db` |
| Fare | `./data/fare-db` |

H2 console is enabled in development mode only.
No service may reference another service's database file.

---

## Shared Identifier Conventions

All cross-service identifiers are UUID strings stored as plain VARCHAR columns.

| Identifier | Type | Used In |
|-----------|------|---------|
| accountId | UUID String | Account, Driver, Ride, Fare |
| driverId | UUID String | Driver, Ride |
| vehicleId | UUID String | Driver |
| rideId | UUID String | Ride, Fare |
| fareId | UUID String | Fare |
| paymentId | UUID String | Fare |
| receiptId | UUID String | Fare |

Rule: A service stores a foreign UUID as a plain String field. It never imports a JPA
entity from another service.

---

## JWT Design

```
Header:  { "alg": "HS256", "typ": "JWT" }
Payload: {
  "sub":   "<accountId>",
  "email": "<email>",
  "role":  "PASSENGER | DRIVER | ADMIN",
  "iat":   <issued-at epoch seconds>,
  "exp":   <expiry epoch seconds>
}
```

- Signed with HMAC-SHA256 using JWT_SECRET environment variable.
- Default expiry: 24 hours.
- All four services validate the token using the same JWT_SECRET.
- Interservice calls forward the original Authorization: Bearer <token> header.

---

## Role Matrix

| Action | PASSENGER | DRIVER | ADMIN |
|--------|-----------|--------|-------|
| Register | Yes | Yes | - |
| Login | Yes | Yes | Yes |
| View own profile | Yes | Yes | Yes |
| Update own profile | Yes | Yes | - |
| Manage account status | - | - | Yes |
| Create driver profile | - | Yes | - |
| Manage driver availability | - | Yes | - |
| Request ride | Yes | - | - |
| Accept/start/complete ride | - | Yes | - |
| Cancel ride | Yes | Yes | Yes |
| Get fare estimate | Yes | - | Yes |
| Pay fare | Yes | - | - |
| View receipt | Yes | - | Yes |

---

## Fare Calculation Formula

```
fare = baseFare + (distanceKm x ratePerKm) + (durationMinutes x ratePerMinute)
```

Starting rates (configurable via application properties):

| Parameter | Value | Currency |
|-----------|-------|----------|
| baseFare | 100.00 | LKR |
| ratePerKm | 80.00 | LKR |
| ratePerMinute | 5.00 | LKR |

---

## Standard Error Response

Every service returns this JSON on errors:

```json
{
  "timestamp": "2025-01-01T10:00:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Human-readable message",
  "path": "/api/example"
}
```

---

## Interservice Communication Map

```
Ride Service --GET--> Driver Service  : GET /api/drivers/eligible?serviceArea=X
Ride Service --POST-> Driver Service  : POST /api/drivers/{driverId}/reserve
Ride Service --POST-> Driver Service  : POST /api/drivers/{driverId}/release
Ride Service --POST-> Fare Service    : POST /api/fares/final
```

Failure handling:
- If Driver Service is unreachable: HTTP 503, ride is NOT created in ASSIGNED state.
- If Fare Service is unreachable: HTTP 503, ride remains COMPLETED, fare creation is retried manually.

---

## Security Warnings (Academic Disclaimer)

This project uses simplified JWT validation. Limitations to document:
1. No token revocation / blacklist.
2. Interservice calls forward the user JWT - a compromised token is valid across all services.
3. H2 databases have no encryption at rest.
4. Environment variable secrets are not rotated.

---

## CI Summary

GitHub Actions workflow (.github/workflows/ci.yml):
- Trigger: pull_request and push to main.
- Java: 17 (Temurin distribution).
- Steps: build + test all four services with ./mvnw verify.
- Fails fast if any service fails.

---

## Git Workflow

- main - integrated working code only.
- Feature branches: feature/<short-description> e.g. feature/account-service-auth.
- Commit style: <type>(<scope>): <short description> e.g. feat(account): add JWT issuance.
- Merge via pull request with peer review.
- No permanent per-service branches.

---

## Environment Variables Reference

| Variable | Used By | Description |
|----------|---------|-------------|
| JWT_SECRET | All services | HMAC-SHA256 signing key (min 32 chars) |
| DRIVER_SERVICE_URL | Ride Service | Base URL of Driver Service |
| FARE_SERVICE_URL | Ride Service | Base URL of Fare Service |
| ACCOUNT_SERVICE_URL | Optional | If services need to verify accounts |

See .env.example for safe placeholder values and PowerShell setup commands.
