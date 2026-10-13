# RideLink – Service 3: Ride Management Service

**Port:** `8083`  
**Package:** `com.ridelink.ride`  
**Database:** Isolated H2 File-based Database (`./data/ride-db`)  
**Technology Stack:** Java 17, Spring Boot 3.3.4, Spring Data JPA, Spring Security, Spring Web, Springdoc OpenAPI 3 (Swagger UI), JJWT, JUnit 5, Mockito.

---

## 1. Overview & Service Responsibility

The **Ride Management Service** is the central coordinating microservice for the RideLink ride-hailing platform. It is responsible for:
- Passenger ride-request creation with geospatial pickup and destination coordinates.
- Calling **Fare & Payment Service (Port 8084)** to obtain binding fare estimates.
- Discovering and reserving eligible available drivers from **Driver & Vehicle Service (Port 8082)**.
- Enforcing strict status lifecycle state transitions (`REQUESTED` → `ASSIGNED` → `ACCEPTED` → `IN_PROGRESS` → `COMPLETED`).
- Managing ride cancellations with automatic driver release compensation.
- Coordinating trip completion, final fare computation with Fare Service, and releasing drivers back to `AVAILABLE`.
- Enforcing strict passenger and driver ownership rules.

---

## 2. Architecture & Interservice Communication

```
                   +------------------------+
                   |     Account Service    | (Port 8081)
                   |  (JWT Token Provider)  |
                   +-----------+------------+
                               | (Local HS256 JWT Verification)
                               v
+------------------+   REST (Port 8083)    +-------------------------------+
| Passenger/Driver | --------------------> |    Ride Management Service    |
+------------------+                       +---------------+---------------+
                                                           |
                      +------------------------------------+------------------------------------+
                      |                                                                         |
                      | REST (`X-Internal-Service-Key`)                                         | REST (`Bearer <JWT>` / Internal)
                      v                                                                         v
+---------------------------------------+                             +---------------------------------------+
|   Driver & Vehicle Service (Port 8082)|                             |    Fare & Payment Service (Port 8084) |
|   - Driver Discovery & Haversine Calc |                             |    - Fare Estimates                   |
|   - Reservation & Release             |                             |    - Final Fare Generation            |
+---------------------------------------+                             +---------------------------------------+
```

---

## 3. Ride Lifecycle State Machine

```
  +-------------+
  |  REQUESTED  | -------- (Cancelled by Passenger/Admin) --------+
  +------+------+                                                 |
         | (Driver Discovered & Reserved)                         |
         v                                                        |
  +-------------+                                                 |
  |  ASSIGNED   | -------- (Cancelled: Driver Released) ----------+
  +------+------+                                                 |
         | (Driver Accepts)                                       |
         v                                                        v
  +-------------+                                          +-------------+
  |  ACCEPTED   | -------- (Cancelled: Driver Released) -> |  CANCELLED  |
  +------+------+                                          +-------------+
         | (Driver Starts Trip: Driver -> ON_TRIP)
         v
  +-------------+
  | IN_PROGRESS | (Cannot be cancelled)
  +------+------+
         | (Driver Completes: Final Fare Computed, Driver Released)
         v
  +-------------+
  |  COMPLETED  |
  +-------------+
```

### Transition Validation Rules
- `REQUESTED` → `ASSIGNED`: Valid when an eligible driver is found and reserved.
- `ASSIGNED` → `ACCEPTED`: Only the assigned driver can accept.
- `ACCEPTED` → `IN_PROGRESS`: Only the assigned driver can start the trip.
- `IN_PROGRESS` → `COMPLETED`: Only the assigned driver can complete the trip with actual distance and duration.
- Cancellation: Permitted from `REQUESTED`, `ASSIGNED`, or `ACCEPTED`. If a driver is reserved, the driver is automatically released back to `AVAILABLE`.
- Any invalid or out-of-order transition immediately returns `409 Conflict`.

---

## 4. Driver Assignment & Discovery Algorithm

1. **Eligible Driver Query:** Ride Service queries Driver Service at `GET /api/drivers/eligible?serviceArea={area}&vehicleType={type}&pickupLatitude={lat}&pickupLongitude={lon}&limit=5`.
2. **Selection Rule:** Driver Service returns eligible available drivers sorted by Haversine distance from the pickup point. Ride Service prioritizes the closest available driver.
3. **Atomic Reservation:** Ride Service calls `POST /api/internal/drivers/{driverProfileId}/reserve` with the `rideId` and `X-Internal-Service-Key`.
4. **Conflict Handling:** If the candidate driver was reserved concurrently by another ride (`409 Conflict`), Ride Service automatically tries the next candidate in the eligibility list.
5. **No Available Driver:** If all candidates are unavailable or the list is empty, the ride remains truthfully in `REQUESTED` status, allowing the passenger to retry later via `POST /api/rides/{rideId}/assign`.

---

## 5. Interservice Contract Details

### Driver & Vehicle Service (Port 8082)
- **Header:** `X-Internal-Service-Key: ridelink-internal-secret-key-2026`
- **Eligibility Discovery:** `GET /api/drivers/eligible`
- **Driver Reservation:** `POST /api/internal/drivers/{driverProfileId}/reserve` body: `{"rideId": "..."}`
- **Start Trip:** `POST /api/internal/drivers/{driverProfileId}/start-trip` body: `{"rideId": "..."}`
- **Release Driver:** `POST /api/internal/drivers/{driverProfileId}/release` body: `{"rideId": "...", "reason": "..."}`

### Fare & Payment Service (Port 8084)
- **Fare Estimate:** `POST /api/fares/estimate` body: `{"distanceKm": 5.0, "durationMinutes": 15}`
- **Final Fare Calculation:** `POST /api/fares/final` body: `{"rideId": "...", "passengerId": "...", "distanceKm": 5.5, "durationMinutes": 18}`

---

## 6. API Endpoints

### Passenger Operations
| Method | Endpoint | Description | Auth / Role |
|---|---|---|---|
| `POST` | `/api/rides` | Create ride request, estimate fare & assign driver | `ROLE_PASSENGER`, `ROLE_ADMIN` |
| `POST` | `/api/rides/{id}/assign` | Retry driver assignment for `REQUESTED` ride | `ROLE_PASSENGER`, `ROLE_ADMIN` |
| `GET` | `/api/rides/{id}` | Get ride details (Passenger, Assigned Driver, Admin) | Authenticated |
| `GET` | `/api/rides/my` | Get passenger's ride history | `ROLE_PASSENGER`, `ROLE_ADMIN` |
| `POST` | `/api/rides/{id}/cancel` | Cancel ride (releases driver if assigned) | Authenticated |

### Driver Operations
| Method | Endpoint | Description | Auth / Role |
|---|---|---|---|
| `GET` | `/api/rides/driver/my` | Get driver's assigned rides | `ROLE_DRIVER` |
| `POST` | `/api/rides/{id}/accept` | Accept assigned ride (`ASSIGNED` → `ACCEPTED`) | `ROLE_DRIVER` |
| `POST` | `/api/rides/{id}/start` | Start trip (`ACCEPTED` → `IN_PROGRESS`) | `ROLE_DRIVER` |
| `POST` | `/api/rides/{id}/complete` | Complete trip & generate final fare (`IN_PROGRESS` → `COMPLETED`) | `ROLE_DRIVER` |

### Administrator Operations
| Method | Endpoint | Description | Auth / Role |
|---|---|---|---|
| `GET` | `/api/admin/rides` | View all platform rides (optional `?status=` filter) | `ROLE_ADMIN` |

---

## 7. Security & Role Ownership

- Authentication uses **JWT Bearer tokens** signed with HMAC-SHA256 (`HS256`).
- Passenger ID is extracted directly from the JWT principal (`accountId`). Client-supplied passenger IDs are never trusted.
- Assigned driver checks verify that `driverAccountId` matches the caller's JWT account ID.
- Unauthorized access attempts return `403 Forbidden`. Missing or invalid tokens return `401 Unauthorized`.

---

## 8. Configuration & Environment Variables

| Variable | Default Value | Description |
|---|---|---|
| `PORT` | `8083` | Service port |
| `JWT_SECRET` | `bXktc3VwZXItc2VjcmV0LWtleS10aGF0LWlzLWxvbmctZW5vdWdoLWZvci1IUzI1Ng==` | Shared JWT HMAC-SHA256 Secret Key |
| `DRIVER_SERVICE_URL` | `http://localhost:8082` | Driver & Vehicle Service base URL |
| `FARE_SERVICE_URL` | `http://localhost:8084` | Fare & Payment Service base URL |
| `INTERNAL_SERVICE_KEY` | `ridelink-internal-secret-key-2026` | Secret key for internal microservice calls |

---

## 9. Running & Testing

### Compile and Run Tests
```powershell
.\mvnw.cmd clean test
```

### Package into Executable JAR
```powershell
.\mvnw.cmd clean package -DskipTests
```

### Run Service
```powershell
.\mvnw.cmd spring-boot:run
```

### Interactive API Documentation (Swagger UI)
```
http://localhost:8083/swagger-ui/index.html
```

---

## 10. Known Limitations
- Geospatial calculations use simulated coordinates and Haversine mathematical formulas without third-party mapping SDK dependencies (Google Maps / Mapbox).
- Driver locations are simulated rather than connected to real GPS telemetry streams.
