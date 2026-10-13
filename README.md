# RideLink – Backend Microservices for a Ride-Sharing Platform

> **IT3130 – Application Development | Group Assignment**  
> Faculty of Computing, Sri Lanka Institute of Information Technology (SLIIT)

---

## 📋 Overview

**RideLink** is an enterprise-grade backend microservices solution for a ride-sharing platform. The system is decomposed into four independently deployable core services following the **Database-per-Service** design pattern, securing interactions via stateless **JWT authentication**, and enabling synchronous interservice communication via **Spring RestClient**.

---

## 🏛️ Architecture & Microservice Ownership

| Service Name | Port | Primary Database | Key Responsibilities |
|:---|:---:|:---:|:---|
| **Account Service** | `8081` | H2 (File-based: `./data/account-db`) | Passenger & driver registration, authentication (BCrypt), JWT token issuance, role-based access control. |
| **Driver & Vehicle Service** | `8082` | H2 (File-based: `./data/driver-db`) | Driver operational profile, fleet & vehicle management, real-time availability, simulated location, driver eligibility matching. |
| **Ride Management Service** | `8083` | H2 (File-based: `./data/ride-db`) | Trip lifecycle orchestration (`REQUESTED` → `ASSIGNED` → `ACCEPTED` → `IN_PROGRESS` → `COMPLETED` / `CANCELLED`), driver reservation, dispatch. |
| **Fare & Payment Service** | `8084` | **MongoDB Atlas (Cloud)** | Indicative fare estimation, final fare calculation, simulated idempotent payment processing, digital receipt generation. |

---

## 🛠️ Technology Stack

- **Language:** Java 17 (LTS)
- **Framework:** Spring Boot 3.2.12
- **Persistence:** Spring Data JPA / Hibernate (Services 1–3), Spring Data MongoDB (Service 4)
- **Databases:** 
  - H2 Database (Isolated file-based relational databases for Account, Driver, and Ride services)
  - MongoDB Atlas Cloud (Document database for Fare & Payment service)
- **Security:** Spring Security 6, JJWT (HMAC-SHA256 tokens), BCrypt password hashing
- **Interservice Communication:** Spring RestClient with `X-Internal-Service-Key` header verification
- **API Documentation:** Springdoc OpenAPI 2.3.x (Swagger UI)
- **Testing:** JUnit 5, Mockito, Spring Boot Test, Postman Collection
- **Build System:** Apache Maven Wrapper (`mvnw` / `mvnw.cmd`)

---

## 🚀 Quick Start Guide

### Prerequisites
- **Java Development Kit (JDK):** Version 17 or higher
- **Internet Connection:** Active connection for MongoDB Atlas access

### Running the Services Locally

Open 4 separate terminal windows and run each service:

```powershell
# 1. Start Account Service (Port 8081)
cd account-service
.\mvnw.cmd spring-boot:run

# 2. Start Driver & Vehicle Service (Port 8082)
cd driver-vehicle-service
.\mvnw.cmd spring-boot:run

# 3. Start Ride Management Service (Port 8083)
cd ride-service
.\mvnw.cmd spring-boot:run

# 4. Start Fare & Payment Service (Port 8084)
cd fare-payment-service
.\mvnw.cmd spring-boot:run
```

*(On Linux / macOS, use `./mvnw spring-boot:run`)*

---

## 📖 Swagger UI & API Documentation

Interactive Swagger documentation is exposed on each microservice:

- **Account Service:** [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html)
- **Driver Service:** [http://localhost:8082/swagger-ui/index.html](http://localhost:8082/swagger-ui/index.html)
- **Ride Service:** [http://localhost:8083/swagger-ui/index.html](http://localhost:8083/swagger-ui/index.html)
- **Fare Service:** [http://localhost:8084/swagger-ui/index.html](http://localhost:8084/swagger-ui/index.html)

---

## 🧪 Automated Testing & Verification

An end-to-end automated verification script is included in the project root:

```powershell
powershell -File .\test_apis.ps1
```

This script exercises the complete end-to-end integration lifecycle:
1. Actuator health probes across ports 8081–8084
2. Passenger & Driver registration and JWT authentication
3. Driver vehicle onboarding, activation, and Colombo location ping
4. Indicative fare estimation
5. Complete ride booking, driver assignment, trip start, trip completion, payment settlement, and digital receipt retrieval

---

## 🔒 Security & Environment Configuration

All microservices validate security using environment variables (or fallbacks for local evaluation):

- `JWT_SECRET`: Shared 256-bit signing key across all 4 services.
- `INTERNAL_SERVICE_KEY`: Pre-shared key for private service-to-service communication.
- `DRIVER_SERVICE_URL`: URL to Driver Service used by Ride Service (`http://localhost:8082`).
- `FARE_SERVICE_URL`: URL to Fare Service used by Ride Service (`http://localhost:8084`).
