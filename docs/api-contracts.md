# RideLink – API Contracts

> **Version:** 0.1 (Phase 0 – Design)
> **Last Updated:** Phase 0
> **Rule:** No contract may be changed silently after approval. Any change must be explained and this file updated.

---

## Table of Contents

1. [Standard Conventions](#1-standard-conventions)
2. [Account Service – Port 8081](#2-account-service--port-8081)
3. [Driver & Vehicle Service – Port 8082](#3-driver--vehicle-service--port-8082)
4. [Ride Management Service – Port 8083](#4-ride-management-service--port-8083)
5. [Fare & Payment Service – Port 8084](#5-fare--payment-service--port-8084)
6. [Interservice Contracts](#6-interservice-contracts)
7. [Status Code Reference](#7-status-code-reference)

---

## 1. Standard Conventions

### Authentication Header
```
Authorization: Bearer <JWT token>
```

### Standard Error Response (all services)
```json
{
  "timestamp": "2025-01-01T10:00:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "email: must be a valid email address",
  "path": "/api/auth/register/passenger"
}
```

**Error Codes:**

| code | Meaning |
|------|---------|
| VALIDATION_ERROR | Bean validation failed |
| DUPLICATE_EMAIL | Email already registered |
| INVALID_CREDENTIALS | Wrong email or password |
| ACCOUNT_SUSPENDED | Account is suspended |
| ACCOUNT_DISABLED | Account is disabled |
| UNAUTHORIZED | Missing or invalid JWT |
| FORBIDDEN | Valid JWT but insufficient role or wrong ownership |
| NOT_FOUND | Resource not found |
| CONFLICT | Business rule conflict (e.g., invalid status transition) |
| NO_DRIVER_AVAILABLE | No eligible driver found |
| SERVICE_UNAVAILABLE | Required downstream service is unreachable |
| DUPLICATE_FINAL_FARE | Final fare already exists for this ride |
| DUPLICATE_PAYMENT | Payment already successfully processed |
| PAYMENT_FAILED | Simulated payment failure |
| INTERNAL_ERROR | Unexpected server error |

### UUID Format
All ID fields are UUID strings, e.g. `"3fa85f64-5717-4562-b3fc-2c963f66afa6"`.

---

## 2. Account Service – Port 8081

Base URL: `http://localhost:8081`

### 2.1 POST /api/auth/register/passenger

**Purpose:** Register a new passenger account.
**Auth:** Public (no token required).

**Request Body:**
```json
{
  "name":     "Nimal Perera",
  "email":    "nimal@example.com",
  "password": "SecurePass123!",
  "phone":    "+94771234567"
}
```

**Field Validation:**

| Field | Rules |
|-------|-------|
| name | Not blank, max 100 chars |
| email | Valid format, unique across all accounts |
| password | Min 8 chars, at least 1 uppercase, 1 digit, 1 special char |
| phone | Optional; if provided, must match E.164 format |

**Response 201 Created:**
```json
{
  "accountId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "name":      "Nimal Perera",
  "email":     "nimal@example.com",
  "phone":     "+94771234567",
  "role":      "PASSENGER",
  "status":    "ACTIVE",
  "createdAt": "2025-01-01T10:00:00Z"
}
```

**Error Responses:**

| Status | code | Condition |
|--------|------|-----------|
| 400 | VALIDATION_ERROR | Any field fails validation |
| 409 | DUPLICATE_EMAIL | Email already registered |

---

### 2.2 POST /api/auth/register/driver

**Purpose:** Register a new driver account (account only, no operational profile).
**Auth:** Public.

**Request Body:** Same fields as passenger registration.

**Response 201 Created:**
```json
{
  "accountId": "uuid",
  "name":      "Kamal Silva",
  "email":     "kamal@example.com",
  "phone":     "+94771234568",
  "role":      "DRIVER",
  "status":    "ACTIVE",
  "createdAt": "2025-01-01T10:00:00Z"
}
```

**Error Responses:** Same as passenger registration.

---

### 2.3 POST /api/auth/login

**Purpose:** Authenticate and receive a JWT.
**Auth:** Public.

**Request Body:**
```json
{
  "email":    "nimal@example.com",
  "password": "SecurePass123!"
}
```

**Response 200 OK:**
```json
{
  "accessToken": "<JWT string>",
  "tokenType":   "Bearer",
  "expiresIn":   86400,
  "accountId":   "uuid",
  "role":        "PASSENGER"
}
```

**Error Responses:**

| Status | code | Condition |
|--------|------|-----------|
| 400 | VALIDATION_ERROR | Blank fields |
| 401 | INVALID_CREDENTIALS | Wrong email or password |
| 403 | ACCOUNT_SUSPENDED | Account is suspended |
| 403 | ACCOUNT_DISABLED | Account is disabled |

---

### 2.4 GET /api/accounts/me

**Purpose:** Return the authenticated account's own profile.
**Auth:** Required – PASSENGER, DRIVER, or ADMIN.

**Response 200 OK:**
```json
{
  "accountId": "uuid",
  "name":      "Nimal Perera",
  "email":     "nimal@example.com",
  "phone":     "+94771234567",
  "role":      "PASSENGER",
  "status":    "ACTIVE",
  "createdAt": "2025-01-01T10:00:00Z",
  "updatedAt": "2025-01-02T09:00:00Z"
}
```

**Error Responses:**

| Status | code | Condition |
|--------|------|-----------|
| 401 | UNAUTHORIZED | No or invalid JWT |

---

### 2.5 PUT /api/accounts/me

**Purpose:** Update own name, phone (not email or role).
**Auth:** Required – PASSENGER or DRIVER.

**Request Body:**
```json
{
  "name":  "Nimal K. Perera",
  "phone": "+94779999999"
}
```

**Response 200 OK:** Updated account profile (same shape as GET /me).

**Error Responses:**

| Status | code | Condition |
|--------|------|-----------|
| 400 | VALIDATION_ERROR | Validation failure |
| 401 | UNAUTHORIZED | No or invalid JWT |

---

### 2.6 PATCH /api/accounts/{accountId}/status

**Purpose:** Admin changes account status (ACTIVE / SUSPENDED / DISABLED).
**Auth:** Required – ADMIN only.

**Path Param:** `accountId` – UUID of target account.

**Request Body:**
```json
{
  "status": "SUSPENDED",
  "reason": "Terms of service violation"
}
```

**Response 200 OK:** Updated account profile.

**Error Responses:**

| Status | code | Condition |
|--------|------|-----------|
| 400 | VALIDATION_ERROR | Invalid status value |
| 401 | UNAUTHORIZED | No or invalid JWT |
| 403 | FORBIDDEN | Not ADMIN |
| 404 | NOT_FOUND | accountId does not exist |

---

## 3. Driver & Vehicle Service – Port 8082

Base URL: `http://localhost:8082`

### 3.1 POST /api/drivers

**Purpose:** Create a driver operational profile linked to an Account Service accountId.
**Auth:** Required – DRIVER role only.

**Request Body:**
```json
{
  "licenseNumber": "B1234567",
  "serviceArea":   "Colombo",
  "phone":         "+94771234568"
}
```

**Field Validation:**

| Field | Rules |
|-------|-------|
| licenseNumber | Not blank, max 50 chars |
| serviceArea | Not blank, max 100 chars |
| phone | Optional, E.164 format |

**Response 201 Created:**
```json
{
  "driverId":       "uuid",
  "accountId":      "uuid",
  "licenseNumber":  "B1234567",
  "serviceArea":    "Colombo",
  "availability":   "OFFLINE",
  "phone":          "+94771234568",
  "currentLat":     null,
  "currentLng":     null,
  "createdAt":      "2025-01-01T10:00:00Z"
}
```

**Error Responses:**

| Status | code | Condition |
|--------|------|-----------|
| 400 | VALIDATION_ERROR | Validation failure |
| 401 | UNAUTHORIZED | No or invalid JWT |
| 403 | FORBIDDEN | Not DRIVER role |
| 409 | CONFLICT | Driver profile already exists for this accountId |

---

### 3.2 GET /api/drivers/me

**Purpose:** Return the authenticated driver's operational profile.
**Auth:** Required – DRIVER role.

**Response 200 OK:** Same shape as POST /api/drivers response.

---

### 3.3 PUT /api/drivers/me

**Purpose:** Update driver profile (serviceArea, phone).
**Auth:** Required – DRIVER role.

**Request Body:**
```json
{
  "serviceArea": "Kandy",
  "phone":       "+94771234500"
}
```

**Response 200 OK:** Updated driver profile.

---

### 3.4 POST /api/drivers/me/vehicles

**Purpose:** Add a vehicle to the authenticated driver's profile.
**Auth:** Required – DRIVER role.

**Request Body:**
```json
{
  "make":         "Toyota",
  "model":        "Prius",
  "year":         2020,
  "licensePlate": "CAB-1234",
  "color":        "White",
  "type":         "SEDAN"
}
```

**Field Validation:**

| Field | Rules |
|-------|-------|
| make | Not blank, max 50 chars |
| model | Not blank, max 50 chars |
| year | 1990 to current year + 1 |
| licensePlate | Not blank, max 20 chars |
| color | Not blank, max 30 chars |
| type | SEDAN, SUV, VAN, TUKTUUK |

**Response 201 Created:**
```json
{
  "vehicleId":    "uuid",
  "driverId":     "uuid",
  "make":         "Toyota",
  "model":        "Prius",
  "year":         2020,
  "licensePlate": "CAB-1234",
  "color":        "White",
  "type":         "SEDAN",
  "createdAt":    "2025-01-01T10:00:00Z"
}
```

---

### 3.5 PUT /api/drivers/me/location

**Purpose:** Update the driver's simulated current GPS coordinates.
**Auth:** Required – DRIVER role.

**Request Body:**
```json
{
  "latitude":  6.9271,
  "longitude": 79.8612
}
```

**Field Validation:**

| Field | Rules |
|-------|-------|
| latitude | Between -90.0 and 90.0 |
| longitude | Between -180.0 and 180.0 |

**Response 200 OK:** Updated driver profile.

---

### 3.6 PATCH /api/drivers/me/availability

**Purpose:** Change the authenticated driver's availability status.
**Auth:** Required – DRIVER role.

**Request Body:**
```json
{
  "availability": "AVAILABLE"
}
```

Valid values: `AVAILABLE`, `RESERVED`, `ON_RIDE`, `OFFLINE`.

**Response 200 OK:** Updated driver profile.

---

### 3.7 GET /api/drivers/eligible?serviceArea={area}

**Purpose:** Return a list of AVAILABLE drivers in the given service area.
**Auth:** Required – PASSENGER, ADMIN, or internal interservice call (forwarded DRIVER or PASSENGER token).

**Query Params:**

| Param | Required | Description |
|-------|----------|-------------|
| serviceArea | Yes | The service area string to filter by |

**Response 200 OK:**
```json
[
  {
    "driverId":     "uuid",
    "accountId":    "uuid",
    "serviceArea":  "Colombo",
    "availability": "AVAILABLE",
    "currentLat":   6.9271,
    "currentLng":   79.8612
  }
]
```

Returns empty array `[]` if no eligible drivers exist. Never returns 404.

---

### 3.8 POST /api/drivers/{driverId}/reserve

**Purpose:** Mark a driver as RESERVED (called internally by Ride Service).
**Auth:** Required – forwarded JWT from Ride Service context.

**Path Param:** `driverId` – UUID of driver to reserve.

**Response 200 OK:**
```json
{
  "driverId":     "uuid",
  "availability": "RESERVED"
}
```

**Error Responses:**

| Status | code | Condition |
|--------|------|-----------|
| 404 | NOT_FOUND | Driver not found |
| 409 | CONFLICT | Driver is not AVAILABLE (already RESERVED or ON_RIDE) |

---

### 3.9 POST /api/drivers/{driverId}/release

**Purpose:** Release a driver back to AVAILABLE or OFFLINE (called by Ride Service on cancellation or no-show).
**Auth:** Required – forwarded JWT.

**Response 200 OK:**
```json
{
  "driverId":     "uuid",
  "availability": "AVAILABLE"
}
```

---

## 4. Ride Management Service – Port 8083

Base URL: `http://localhost:8083`

### Ride Status Machine

```
REQUESTED --> ASSIGNED   (system assigns a driver)
REQUESTED --> CANCELLED  (passenger cancels before assignment)
ASSIGNED  --> ACCEPTED   (driver accepts)
ASSIGNED  --> CANCELLED  (passenger or admin cancels)
ACCEPTED  --> IN_PROGRESS (driver starts the ride)
ACCEPTED  --> CANCELLED   (documented group decision required)
IN_PROGRESS --> COMPLETED (driver marks complete)
```

Any other transition returns HTTP 409 CONFLICT.

---

### 4.1 POST /api/rides

**Purpose:** Passenger requests a ride. System auto-queries Driver Service and assigns a driver.
**Auth:** Required – PASSENGER role.

**Request Body:**
```json
{
  "pickupLocation":      "Colombo Fort",
  "destination":         "Kandy",
  "serviceArea":         "Colombo",
  "distanceKm":          115.5,
  "durationMinutes":     180
}
```

**Field Validation:**

| Field | Rules |
|-------|-------|
| pickupLocation | Not blank, max 200 chars |
| destination | Not blank, max 200 chars |
| serviceArea | Not blank, max 100 chars |
| distanceKm | Positive decimal |
| durationMinutes | Positive integer |

**Response 201 Created:**
```json
{
  "rideId":          "uuid",
  "passengerId":     "uuid",
  "driverId":        "uuid",
  "pickupLocation":  "Colombo Fort",
  "destination":     "Kandy",
  "serviceArea":     "Colombo",
  "distanceKm":      115.5,
  "durationMinutes": 180,
  "status":          "ASSIGNED",
  "requestedAt":     "2025-01-01T10:00:00Z",
  "updatedAt":       "2025-01-01T10:00:01Z"
}
```

**Error Responses:**

| Status | code | Condition |
|--------|------|-----------|
| 400 | VALIDATION_ERROR | Validation failure |
| 401 | UNAUTHORIZED | No or invalid JWT |
| 403 | FORBIDDEN | Not PASSENGER role |
| 503 | NO_DRIVER_AVAILABLE | No eligible drivers in service area |
| 503 | SERVICE_UNAVAILABLE | Driver Service unreachable |

---

### 4.2 GET /api/rides/{rideId}

**Purpose:** Retrieve ride by ID. Passenger sees only own rides; driver sees assigned rides; ADMIN sees all.
**Auth:** Required – PASSENGER, DRIVER, or ADMIN.

**Response 200 OK:** Same shape as POST /api/rides response (plus all timestamps).

**Error Responses:**

| Status | code | Condition |
|--------|------|-----------|
| 401 | UNAUTHORIZED | No JWT |
| 403 | FORBIDDEN | Ownership check failed |
| 404 | NOT_FOUND | Ride not found |

---

### 4.3 GET /api/rides/my-rides

**Purpose:** List all rides for the authenticated user (passenger = their rides; driver = rides assigned to them).
**Auth:** Required – PASSENGER or DRIVER.

**Response 200 OK:** Array of ride objects.

---

### 4.4 PATCH /api/rides/{rideId}/accept

**Purpose:** Driver accepts an ASSIGNED ride (ASSIGNED → ACCEPTED).
**Auth:** Required – DRIVER role, must be the assigned driver.

**Response 200 OK:** Updated ride object with status ACCEPTED.

**Error Responses:**

| Status | code | Condition |
|--------|------|-----------|
| 403 | FORBIDDEN | Not the assigned driver |
| 404 | NOT_FOUND | Ride not found |
| 409 | CONFLICT | Ride is not in ASSIGNED status |

---

### 4.5 PATCH /api/rides/{rideId}/start

**Purpose:** Driver starts ride (ACCEPTED → IN_PROGRESS).
**Auth:** Required – DRIVER role, must be the assigned driver.

**Response 200 OK:** Updated ride object with status IN_PROGRESS.

---

### 4.6 PATCH /api/rides/{rideId}/complete

**Purpose:** Driver completes ride (IN_PROGRESS → COMPLETED).
After success, Ride Service calls Fare Service to create final fare.
**Auth:** Required – DRIVER role, must be the assigned driver.

**Response 200 OK:** Updated ride object with status COMPLETED.

**Error Responses:**

| Status | code | Condition |
|--------|------|-----------|
| 409 | CONFLICT | Ride not IN_PROGRESS |
| 503 | SERVICE_UNAVAILABLE | Fare Service unreachable (ride still marked COMPLETED) |

---

### 4.7 PATCH /api/rides/{rideId}/cancel

**Purpose:** Cancel a ride at an allowed status.
**Auth:** Required – PASSENGER (own ride), DRIVER (assigned ride), or ADMIN.

**Response 200 OK:** Updated ride with status CANCELLED. If ride was ASSIGNED or ACCEPTED, Ride Service also calls Driver Service to release the driver.

**Error Responses:**

| Status | code | Condition |
|--------|------|-----------|
| 409 | CONFLICT | Ride is IN_PROGRESS or already COMPLETED/CANCELLED |

---

## 5. Fare & Payment Service – Port 8084

Base URL: `http://localhost:8084`

### Fare Formula

```
fare = baseFare + (distanceKm * ratePerKm) + (durationMinutes * ratePerMinute)

defaults: baseFare=100.00, ratePerKm=80.00, ratePerMinute=5.00, currency=LKR
```

All monetary values are `BigDecimal` with 2 decimal places.

---

### 5.1 POST /api/fare-estimates

**Purpose:** Calculate a fare estimate without creating a persistent record.
**Auth:** Required – PASSENGER or ADMIN.

**Request Body:**
```json
{
  "distanceKm":      115.5,
  "durationMinutes": 180
}
```

**Field Validation:**

| Field | Rules |
|-------|-------|
| distanceKm | Positive decimal |
| durationMinutes | Positive integer |

**Response 200 OK:**
```json
{
  "distanceKm":      115.5,
  "durationMinutes": 180,
  "baseFare":        100.00,
  "ratePerKm":       80.00,
  "ratePerMinute":   5.00,
  "estimatedFare":   10140.00,
  "currency":        "LKR"
}
```

Calculation: 100 + (115.5 * 80) + (180 * 5) = 100 + 9240 + 900 = 10240.00
*(numbers above are illustrative; implementation must be correct)*

---

### 5.2 POST /api/fares/final

**Purpose:** Create the definitive fare for a completed ride. Called by Ride Service.
**Auth:** Required – forwarded JWT from Ride Service.
**Rule:** One final fare per rideId. Returns 409 if a final fare already exists.

**Request Body:**
```json
{
  "rideId":          "uuid",
  "passengerId":     "uuid",
  "distanceKm":      115.5,
  "durationMinutes": 180
}
```

**Response 201 Created:**
```json
{
  "fareId":          "uuid",
  "rideId":          "uuid",
  "passengerId":     "uuid",
  "distanceKm":      115.5,
  "durationMinutes": 180,
  "baseFare":        100.00,
  "ratePerKm":       80.00,
  "ratePerMinute":   5.00,
  "totalFare":       10240.00,
  "currency":        "LKR",
  "status":          "FINAL",
  "createdAt":       "2025-01-01T11:00:00Z"
}
```

**Error Responses:**

| Status | code | Condition |
|--------|------|-----------|
| 400 | VALIDATION_ERROR | Negative or zero distance/duration |
| 409 | DUPLICATE_FINAL_FARE | Final fare already exists for rideId |

---

### 5.3 GET /api/fares/{fareId}

**Purpose:** Retrieve a fare record by ID.
**Auth:** Required – PASSENGER (own fare), ADMIN.

**Response 200 OK:** Same shape as POST /api/fares/final response.

---

### 5.4 GET /api/fares/ride/{rideId}

**Purpose:** Get the fare associated with a specific ride.
**Auth:** Required – PASSENGER (own ride), DRIVER (assigned ride), ADMIN.

**Response 200 OK:** Same shape as fare response.

---

### 5.5 POST /api/payments

**Purpose:** Passenger initiates a simulated payment for a fare.
**Auth:** Required – PASSENGER (must own the fare).

**Request Body:**
```json
{
  "fareId":          "uuid",
  "idempotencyKey":  "unique-client-generated-key",
  "paymentMethod":   "CARD",
  "simulateSuccess": true
}
```

**Field Validation:**

| Field | Rules |
|-------|-------|
| fareId | Not blank, valid UUID |
| idempotencyKey | Not blank, max 100 chars |
| paymentMethod | CARD, CASH, WALLET |
| simulateSuccess | Boolean; controls test outcome |

**Business Rules:**
- Fare must exist and be in FINAL status.
- If `simulateSuccess=true`, payment is SUCCESSFUL and fare status becomes PAID.
- If `simulateSuccess=false`, payment is FAILED; fare remains FINAL; retry is permitted.
- Sending the same `idempotencyKey` again after a SUCCESSFUL payment returns the original result without re-processing.
- Sending the same `idempotencyKey` after a FAILED payment allows retry.

**Response 201 Created:**
```json
{
  "paymentId":      "uuid",
  "fareId":         "uuid",
  "passengerId":    "uuid",
  "amount":         10240.00,
  "currency":       "LKR",
  "paymentMethod":  "CARD",
  "status":         "SUCCESSFUL",
  "idempotencyKey": "unique-client-generated-key",
  "paidAt":         "2025-01-01T11:05:00Z"
}
```

**Error Responses:**

| Status | code | Condition |
|--------|------|-----------|
| 400 | VALIDATION_ERROR | Missing fields |
| 401 | UNAUTHORIZED | No JWT |
| 403 | FORBIDDEN | Fare does not belong to this passenger |
| 404 | NOT_FOUND | Fare not found |
| 409 | DUPLICATE_PAYMENT | Fare already PAID (idempotencyKey matches a SUCCESS) |

---

### 5.6 GET /api/payments/{paymentId}

**Purpose:** Retrieve payment details by ID.
**Auth:** Required – PASSENGER (own payment), ADMIN.

**Response 200 OK:** Same shape as POST /api/payments response.

---

### 5.7 GET /api/payments/{paymentId}/receipt

**Purpose:** Retrieve a payment receipt. Only available for SUCCESSFUL payments.
**Auth:** Required – PASSENGER (own), ADMIN.

**Response 200 OK:**
```json
{
  "receiptId":       "uuid",
  "paymentId":       "uuid",
  "fareId":          "uuid",
  "rideId":          "uuid",
  "passengerId":     "uuid",
  "amount":          10240.00,
  "currency":        "LKR",
  "paymentMethod":   "CARD",
  "distanceKm":      115.5,
  "durationMinutes": 180,
  "fareBreakdown": {
    "baseFare":      100.00,
    "distanceFare":  9240.00,
    "durationFare":  900.00,
    "totalFare":     10240.00
  },
  "issuedAt":        "2025-01-01T11:05:01Z"
}
```

**Error Responses:**

| Status | code | Condition |
|--------|------|-----------|
| 404 | NOT_FOUND | Payment not found or payment was not SUCCESSFUL |
| 403 | FORBIDDEN | Not the owner of this payment |

---

## 6. Interservice Contracts

These are the exact HTTP calls Ride Service makes to Driver Service and Fare Service.

### 6.1 Ride → Driver: Find Eligible Driver

```
GET http://localhost:8082/api/drivers/eligible?serviceArea=Colombo
Authorization: Bearer <forwarded token>
```

Response: array of eligible driver DTOs (see 3.7). Ride Service picks the first one.

---

### 6.2 Ride → Driver: Reserve Driver

```
POST http://localhost:8082/api/drivers/{driverId}/reserve
Authorization: Bearer <forwarded token>
```

Response: driver availability DTO. On 409 or error, Ride Service retries with the next eligible driver (or returns 503 if none remain).

---

### 6.3 Ride → Driver: Release Driver

```
POST http://localhost:8082/api/drivers/{driverId}/release
Authorization: Bearer <forwarded token>
```

Called on ride cancellation when driver was already RESERVED or ON_RIDE.

---

### 6.4 Ride → Fare: Create Final Fare

```
POST http://localhost:8084/api/fares/final
Authorization: Bearer <forwarded token>
Content-Type: application/json

{
  "rideId":          "uuid",
  "passengerId":     "uuid",
  "distanceKm":      115.5,
  "durationMinutes": 180
}
```

Called after ride is marked COMPLETED.

---

## 7. Status Code Reference

| HTTP Status | When Used |
|-------------|-----------|
| 200 OK | Successful GET, PUT, PATCH |
| 201 Created | Successful POST that creates a resource |
| 400 Bad Request | Validation errors |
| 401 Unauthorized | Missing or invalid JWT |
| 403 Forbidden | Valid JWT but wrong role or ownership |
| 404 Not Found | Resource does not exist |
| 409 Conflict | Duplicate resource or invalid state transition |
| 503 Service Unavailable | Required downstream service unreachable |
| 500 Internal Server Error | Unexpected server error |
