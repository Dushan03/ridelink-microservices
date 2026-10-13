# RideLink – Driver & Vehicle Service (Service 2 · Port 8082)

## 1. Overview & Service Responsibility

The **Driver & Vehicle Service** is one of the four core microservices comprising the RideLink platform (assigned to **Member 2**). It manages the complete lifecycle of driver operational accounts, vehicle inventory, driver availability states, geospatial driver discovery with Haversine distance calculations, and atomic ride reservations for the Ride Management Service.

---

## 2. Technology Stack

* **Language**: Java 17
* **Framework**: Spring Boot 3.2.12
* **Persistence**: Spring Data JPA with isolated file-based H2 Database (`./data/driver-db`)
* **Security**: Spring Security (Stateless), JWT Bearer Token validation (HMAC-SHA256), `X-Internal-Service-Key` header authentication
* **API Documentation**: Springdoc OpenAPI 2.3.0 (Swagger UI)
* **Build Tool**: Maven Wrapper (`mvnw.cmd`)
* **Testing**: JUnit 5, Mockito, Spring Boot Test, Spring Security Test

---

## 3. Database Ownership

In strict compliance with microservice isolation principles:
* **Driver Service exclusively owns `./data/driver-db`**.
* No direct cross-service queries or entity imports are performed.
* Cross-service references (such as `accountId` from Account Service and `rideId` from Ride Service) are stored as plain UUID strings (`VARCHAR(36)`).

---

## 4. Configuration & Environment Variables

| Variable | Default (Dev) | Description |
| :--- | :--- | :--- |
| `SERVER_PORT` | `8082` | HTTP server port |
| `JWT_SECRET` | Base64 fallback (32+ chars) | HMAC-SHA256 secret key matching Account Service |
| `INTERNAL_SERVICE_KEY` | `ridelink-internal-secret-key-2026` | Shared secret header (`X-Internal-Service-Key`) for internal interservice endpoints |

---

## 5. Running the Service on Windows PowerShell

### Start the Service:
```powershell
cd d:\SLIIT\ridelink-microservices\driver-vehicle-service
.\mvnw.cmd spring-boot:run
```

### Run Tests:
```powershell
cd d:\SLIIT\ridelink-microservices\driver-vehicle-service
.\mvnw.cmd clean test
```

### Package JAR:
```powershell
cd d:\SLIIT\ridelink-microservices\driver-vehicle-service
.\mvnw.cmd clean package
```

### Swagger UI:
Open `http://localhost:8082/swagger-ui/index.html` in any browser.

---

## 6. Endpoints Table

### A. Driver Profile Endpoints
| Method | Path | Auth | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/drivers/profile` | `ROLE_DRIVER` | Create operational driver profile |
| `GET` | `/api/drivers/me` | `ROLE_DRIVER` | Get own driver profile |
| `PUT` | `/api/drivers/me` | `ROLE_DRIVER` | Update profile fields (service area, license expiry) |
| `PATCH`| `/api/drivers/me/availability`| `ROLE_DRIVER` | Update availability status (`OFFLINE` $\leftrightarrow$ `AVAILABLE`) |
| `PATCH`| `/api/drivers/me/location` | `ROLE_DRIVER` | Update service area and simulated coordinates |
| `GET` | `/api/drivers/{driverId}` | `ROLE_ADMIN` | Admin retrieve driver profile |

### B. Vehicle Management Endpoints
| Method | Path | Auth | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/drivers/me/vehicles` | `ROLE_DRIVER` | Register a new vehicle under own profile |
| `GET` | `/api/drivers/me/vehicles` | `ROLE_DRIVER` | List all vehicles owned by driver |
| `GET` | `/api/drivers/me/vehicles/{vehicleId}` | `ROLE_DRIVER` | Get specific owned vehicle |
| `PUT` | `/api/drivers/me/vehicles/{vehicleId}` | `ROLE_DRIVER` | Update owned vehicle details |
| `PATCH`| `/api/drivers/me/vehicles/{vehicleId}/active`| `ROLE_DRIVER` | Activate/deactivate owned vehicle |

### C. Driver Discovery & Eligibility
| Method | Path | Auth | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/drivers/eligible` | Public / Service | Find available drivers sorted by Haversine distance |

### D. Protected Interservice APIs (Ride Management Service)
| Method | Path | Auth | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/internal/drivers/{driverId}/reserve` | `X-Internal-Service-Key` | Atomically reserve available driver for a ride |
| `POST` | `/api/internal/drivers/{driverId}/start-trip`| `X-Internal-Service-Key` | Transition driver from `RESERVED` to `ON_TRIP` |
| `POST` | `/api/internal/drivers/{driverId}/release` | `X-Internal-Service-Key` | Release driver back to `AVAILABLE` on cancel or complete |

---

## 7. Availability State Machine & Transition Rules

```
     ┌───────────┐     (Driver Go Online)
     │  OFFLINE  │ ───────────────────────────► ┌───────────┐
     │           │ ◄─────────────────────────── │ AVAILABLE │
     └───────────┘    (Driver Go Offline)       └─────┬─────┘
                                                      │
                                                      │ (Ride Service Reserve)
                                                      ▼
     ┌───────────┐     (Ride Start Trip)        ┌───────────┐
     │  ON_TRIP  │ ◄─────────────────────────── │ RESERVED  │
     └─────┬─────┘                              └─────┬─────┘
           │                                          │
           │ (Ride Complete Release)                  │ (Ride Cancel Release)
           └──────────────────► AVAILABLE ◄───────────┘
```

### Preconditions to become `AVAILABLE`:
1. Valid driver profile with non-expired license (`licenseExpiryDate > today`).
2. At least one registered vehicle with `active = true`.
3. Valid designated `serviceArea`.
4. Valid simulated coordinates (`currentLatitude` between -90 and 90, `currentLongitude` between -180 and 180).

---

## 8. Interservice Integration Contract with Ride Service

When a passenger requests a ride:
1. **Ride Service** calls `GET /api/drivers/eligible?serviceArea=Colombo&vehicleType=CAR&pickupLatitude=6.9271&pickupLongitude=79.8612` to discover the closest available drivers.
2. **Ride Service** calls `POST /api/internal/drivers/{driverId}/reserve` passing `{"rideId": "<uuid>"}` with `X-Internal-Service-Key`.
3. When the trip commences, **Ride Service** calls `POST /api/internal/drivers/{driverId}/start-trip`.
4. When the ride finishes or is cancelled, **Ride Service** calls `POST /api/internal/drivers/{driverId}/release`.

---

## 9. Known Academic Limitations

1. Coordinates and vehicle movement are simulated; no commercial Google Maps API is used.
2. Token revocation/blacklist is omitted in favor of stateless JWT validation.
3. Internal service authentication uses a pre-shared secret key header.
