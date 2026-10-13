# RideLink – Fare & Payment Service

## 1. Overview

The **Fare & Payment Service** is an independent, Spring Boot-based microservice within the RideLink ride-sharing architecture running on port **8084**. It is responsible for:

1. **Fare Estimation**: Computing indicative fare quotes for passengers prior to booking.
2. **Final Fare Calculation**: Computing deterministic, immutable final fares upon ride completion using high-precision `BigDecimal` arithmetic.
3. **Simulated Payment Processing**: Handling cash, card, and digital wallet payment transactions.
4. **Payment Idempotency**: Ensuring duplicate or retransmitted payment requests with the same `Idempotency-Key` return identical transaction results safely.
5. **Digital Receipt Issuance**: Generating cryptographically unique digital receipts immediately upon successful payment.
6. **Resource Ownership & Security**: Enforcing JWT-based role and passenger resource ownership checks across all protected endpoints.

---

## 2. Technologies Used

* **Language**: Java 17
* **Framework**: Spring Boot 3.2.12
* **Persistence**: Spring Data JPA + Hibernate
* **Database**: H2 File-Based Database (`./data/fare-db`)
* **Security**: Spring Security + Stateless JWT (`jjwt` 0.12.3)
* **Validation**: Jakarta Bean Validation (`@Valid`, `@DecimalMin`, `@NotBlank`, `@NotNull`)
* **Documentation**: Springdoc OpenAPI 2.3.0 (Swagger UI)
* **Testing**: JUnit 5, Mockito, Spring Boot Test, Spring Security Test
* **Build Tool**: Apache Maven Wrapper (`mvnw.cmd`)

---

## 3. Pricing Formula & Configurable Rates

### Fare Calculation Formula:
$$\text{Fare} = \text{baseFare} + (\text{distanceKm} \times \text{ratePerKm}) + (\text{durationMinutes} \times \text{ratePerMinute})$$

### Default Configuration (`application.properties`):
```properties
fare.rates.base-fare=100.00
fare.rates.rate-per-km=50.00
fare.rates.rate-per-minute=2.00
fare.rates.currency=LKR
```

* **Calculation Rule**: All money amounts and rates use `java.math.BigDecimal` with `RoundingMode.HALF_UP` and a scale of 2 decimal places. Floating-point types (`double`/`float`) are strictly avoided to eliminate rounding inaccuracies.
* **Negative Constraints**: Any request with negative distance or negative duration is rejected with HTTP 400 (`VALIDATION_ERROR`).

---

## 4. Environment Variables & Setup

| Environment Variable | Description | Required in Production | Default / Dev Fallback |
| :--- | :--- | :---: | :--- |
| `JWT_SECRET` | Base64-encoded 256-bit HMAC key | Yes | `bXktc3VwZXItc2VjcmV0LWtleS10aGF0LWlzLWxvbmctZW5vdWdoLWZvci1IUzI1Ng==` |
| `SERVER_PORT` | HTTP server port | No | `8084` |

### Setting Environment Variables in Windows PowerShell:
```powershell
$env:JWT_SECRET="bXktc3VwZXItc2VjcmV0LWtleS10aGF0LWlzLWxvbmctZW5vdWdoLWZvci1IUzI1Ng=="
```

---

## 5. Running the Service & Tests

### Run Automated Tests:
```powershell
cd d:\SLIIT\ridelink-microservices\fare-payment-service
.\mvnw.cmd clean test
```

### Build & Package JAR:
```powershell
.\mvnw.cmd clean package
```

### Run Service:
```powershell
.\mvnw.cmd spring-boot:run
# Or run the packaged JAR:
java -jar target/fare-payment-service-0.0.1-SNAPSHOT.jar
```

---

## 6. Swagger / OpenAPI Documentation

* **Swagger UI**: [http://localhost:8084/swagger-ui/index.html](http://localhost:8084/swagger-ui/index.html)
* **OpenAPI Specification**: [http://localhost:8084/v3/api-docs](http://localhost:8084/v3/api-docs)

To test secured endpoints in Swagger UI:
1. Click **Authorize** (top-right padlock).
2. Paste the Bearer JWT obtained from Account Service (`POST http://localhost:8081/api/auth/login`).
3. Click **Authorize** and execute endpoints.

---

## 7. Database Configuration

* **H2 Database URL**: `jdbc:h2:file:./data/fare-db;AUTO_SERVER=TRUE`
* **H2 Web Console**: [http://localhost:8084/h2-console](http://localhost:8084/h2-console)
* **Console JDBC URL**: `jdbc:h2:file:./data/fare-db`
* **Username**: `sa` / **Password**: *(empty)*

> **Note**: Database state persists across server restarts in the `./data/fare-db.mv.db` file. The database is isolated and not shared with any other microservice.

---

## 8. REST Endpoints Reference

### Fare Endpoints

| Method | Endpoint | Auth | Description |
| :--- | :--- | :---: | :--- |
| `POST` | `/api/fares/estimate` | Public / Token | Calculate an indicative fare estimate |
| `POST` | `/api/fares/final` | Bearer JWT | Compute & store final fare for completed ride (409 on duplicate) |
| `GET` | `/api/fares/{fareId}` | Bearer JWT | Get fare details (Owner or ADMIN only) |
| `GET` | `/api/fares/ride/{rideId}` | Bearer JWT | Get final fare for ride ID (Owner or ADMIN only) |

### Payment Endpoints

| Method | Endpoint | Auth | Description |
| :--- | :--- | :---: | :--- |
| `POST` | `/api/payments` | Bearer JWT | Process payment with `Idempotency-Key` header |
| `GET` | `/api/payments/{paymentId}` | Bearer JWT | Get payment transaction details (Owner or ADMIN only) |
| `GET` | `/api/payments/my` | Bearer JWT | Get payment history for authenticated passenger |

### Receipt Endpoints

| Method | Endpoint | Auth | Description |
| :--- | :--- | :---: | :--- |
| `GET` | `/api/receipts/{receiptId}` | Bearer JWT | Get digital receipt by ID (Owner or ADMIN only) |
| `GET` | `/api/receipts/payment/{paymentId}` | Bearer JWT | Get digital receipt by payment ID (Owner or ADMIN only) |

---

## 9. Workflow & Simulation Guide

### Step 1: Obtain a JWT Token from Account Service
```http
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
  "email": "passenger@example.com",
  "password": "Password123!"
}
```
*Response returns `accessToken`.*

---

### Step 2: Request Fare Estimate (Optional)
```http
POST http://localhost:8084/api/fares/estimate
Content-Type: application/json

{
  "distanceKm": 12.5,
  "durationMinutes": 25.0
}
```

---

### Step 3: Compute Final Fare (Called on Ride Completion)
```http
POST http://localhost:8084/api/fares/final
Authorization: Bearer <TOKEN>
Content-Type: application/json

{
  "rideId": "550e8400-e29b-41d4-a716-446655440000",
  "passengerId": "<ACCOUNT_ID>",
  "distanceKm": 12.5,
  "durationMinutes": 25.0
}
```
*Formula Result: $100.00 + (12.5 \times 50.00) + (25.0 \times 2.00) = 775.00\text{ LKR}$.*

---

### Step 4A: Successful Payment Flow (Generates Receipt)
```http
POST http://localhost:8084/api/payments
Authorization: Bearer <TOKEN>
Idempotency-Key: 8b0660bb-b9b5-412e-a5e2-6d2c49a37e51
Content-Type: application/json

{
  "fareId": "<FARE_ID_FROM_STEP_3>",
  "paymentMethod": "CARD",
  "simulateFailure": false
}
```
*Response: Status `SUCCESSFUL`, returns digital receipt with unique `receiptNumber` (`RCP-...`).*

---

### Step 4B: Idempotent Retransmission
Sending the **exact same request** with the **same `Idempotency-Key`** (`8b0660bb-b9b5-412e-a5e2-6d2c49a37e51`) returns the already-processed payment and receipt without re-charging or creating duplicate records.

---

### Step 4C: Simulated Payment Failure Flow
```http
POST http://localhost:8084/api/payments
Authorization: Bearer <TOKEN>
Idempotency-Key: f34cb57e-e175-47e2-8924-4db2c366ff12
Content-Type: application/json

{
  "fareId": "<FARE_ID>",
  "paymentMethod": "CARD",
  "simulateFailure": true
}
```
*Response: Status `FAILED`, reason `Simulated payment rejection`, and `receipt: null`. The passenger can retry payment with a new idempotency key.*

---

## 10. Security & Ownership Rules

1. **Authentication**: Handled via `JwtAuthFilter` reading the HMAC-SHA256 token signed by Account Service.
2. **Passenger Isolation**: A passenger can only view their own fares, payments, and receipts (`fare.passengerId == auth.name`). Unauthorized access returns HTTP 403 (`FORBIDDEN`).
3. **Admin Privileges**: Users with role `ADMIN` can inspect any fare, payment, or receipt.
4. **Error Masking**: Stack traces are intercepted by `GlobalExceptionHandler` and replaced with structured JSON error messages.
