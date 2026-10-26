# Inventory Management System REST API

A secure, professional, high-performance, and maintainable REST API backend for an **Inventory Management System** built with **Java 21** and **Spring Boot 3**.

Designed and structured specifically for university Year 4 graduation project presentation and integration with a **Flutter** mobile client application.

---

## 1. Technology Stack

* **Language**: Java 21 (LTS)
* **Framework**: Spring Boot 3.x
* **Build Tool**: Gradle
* **Database**: PostgreSQL 15+
* **ORM & Data**: Spring Data JPA + Hibernate 6
* **Database Migrations**: Flyway
* **Security & Auth**: Spring Security 6 + JWT (HMAC-SHA256)
* **Password Hashing**: BCrypt (`PasswordEncoder`)
* **Validation**: Jakarta Bean Validation (`@Valid`, `@NotNull`, `@Positive`, etc.)
* **Documentation**: SpringDoc OpenAPI 3 / Swagger UI
* **Testing**: JUnit 5, MockMvc, AssertJ

---

## 2. Layered Architecture

The application strictly follows the layered architecture:
```
Client (Flutter / Web)
       │
       ▼ [HTTP JSON + Bearer JWT]
 Security Filter Chain (JwtAuthFilter + SecurityContext)
       │
       ▼
 Controllers (DTO Validation, HTTP Statuses, ApiResponse wrapper)
       │
       ▼
 Services (Business Logic, Transactions, Concurrency Control)
       │
       ▼
 Repositories (Spring Data JPA, @EntityGraph, JPQL Projections)
       │
       ▼
 Database (PostgreSQL with Flyway Migrations & Constraints)
```

---

## 3. Database Schema & Migration

Database migrations are managed via **Flyway** (`classpath:db/migration`).
All tables have strict check constraints, foreign keys with `ON DELETE RESTRICT`, and performance indexes:

* `app_users`: User credentials, BCrypt password hash, full name, phone, role (`ADMIN`, `STAFF`), active status.
* `categories`: Inventory classifications with case-insensitive unique name constraint.
* `suppliers`: Supplier directory with phone, email, and address.
* `products`: Inventory catalog with SKU (unique), unit of measure (`PCS`, `BOX`, `PACK`, `BOTTLE`, `CAN`, `KG`, `LITER`, `DOZEN`, `CASE`, `CARTON`, `BUNDLE`, `BALE`, `SACK`, `BAG`, `PALLET`, `ROLL`, `SET`, `PAIR`, `TRAY`), unit price (`CHECK >= 0`), stock quantity (`CHECK >= 0`), minimum stock threshold, and optimistic lock `@Version`.
* `stock_transactions`: Immutable audit log of all stock changes (`STOCK_IN`, `STOCK_OUT`), unit of measure, before-and-after quantities, timestamp, and user audit trail.
* `app_user_tokens`: Secure OTP tokens for password reset verification.

---

## 4. Role & Authorization Matrix

The system enforces strict role-based access control (RBAC) at both the security filter and method level (`@PreAuthorize`):

| Resource / Endpoint | HTTP Method | Endpoint Path | Allowed Roles | Description |
| :--- | :---: | :--- | :---: | :--- |
| **Auth** | `POST` | `/api/v1/auth/login` | Public | Authenticate with credentials, returns JWT |
| **Auth** | `GET` | `/api/v1/auth/me` | ADMIN, STAFF | Get current user profile |
| **Auth** | `POST` | `/api/v1/auth/forgot-password` | Public | Send 6-digit OTP to staff email |
| **Auth** | `POST` | `/api/v1/auth/reset-password` | Public | Reset password using verified 6-digit OTP |
| **Auth** | `POST` | `/api/v1/auth/change-password` | ADMIN, STAFF | Change password for authenticated session |
| **Users** | `POST` | `/api/v1/users` | ADMIN, MANAGER | Create new user account (Staff allowed; Admin allowed up to max 3) |
| **Users** | `GET` | `/api/v1/users` | ADMIN, MANAGER | Paginated user list with keyword search |
| **Users** | `GET` | `/api/v1/users/{id}` | ADMIN, MANAGER | User details by ID |
| **Users** | `PUT` | `/api/v1/users/{id}` | ADMIN, MANAGER | Update user profile |
| **Users** | `PATCH` | `/api/v1/users/{id}/status` | ADMIN, MANAGER | Activate / deactivate user account |
| **Categories** | `POST` | `/api/v1/categories` | **ADMIN** | Create category |
| **Categories** | `GET` | `/api/v1/categories` | ADMIN, STAFF | List categories with search & pagination |
| **Categories** | `GET` | `/api/v1/categories/{id}` | ADMIN, STAFF | Get category by ID |
| **Categories** | `PUT` | `/api/v1/categories/{id}` | **ADMIN** | Update category |
| **Categories** | `DELETE`| `/api/v1/categories/{id}` | **ADMIN** | Delete category (rejected if referenced by products) |
| **Suppliers** | `POST` | `/api/v1/suppliers` | **ADMIN** | Create supplier |
| **Suppliers** | `GET` | `/api/v1/suppliers` | ADMIN, STAFF | List suppliers with search & pagination |
| **Suppliers** | `GET` | `/api/v1/suppliers/{id}` | ADMIN, STAFF | Get supplier by ID |
| **Suppliers** | `PUT` | `/api/v1/suppliers/{id}` | **ADMIN** | Update supplier |
| **Suppliers** | `DELETE`| `/api/v1/suppliers/{id}` | **ADMIN** | Delete supplier (rejected if referenced by products) |
| **Products** | `POST` | `/api/v1/products` | **ADMIN** | Create product (unique SKU, initial stock = 0) |
| **Products** | `GET` | `/api/v1/products` | ADMIN, STAFF | List products with filters & pagination |
| **Products** | `GET` | `/api/v1/products/{id}` | ADMIN, STAFF | Product details by ID |
| **Products** | `GET` | `/api/v1/products/sku/{sku}` | ADMIN, STAFF | Product details by SKU |
| **Products** | `PUT` | `/api/v1/products/{id}` | **ADMIN** | Update product (cannot alter stock quantity) |
| **Products** | `PATCH` | `/api/v1/products/{id}/status` | **ADMIN** | Enable or disable product status (stock untouched) |
| **Products** | `DELETE`| `/api/v1/products/{id}` | **ADMIN** | Delete product (fails if stock > 0 or history exists) |
| **Stock** | `POST` | `/api/v1/stock/in` | ADMIN, STAFF | Add inventory stock (creates audit log) |
| **Stock** | `POST` | `/api/v1/stock/out` | ADMIN, STAFF | Deduct stock (checks availability, prevents oversell) |
| **Stock** | `GET` | `/api/v1/stock/history` | ADMIN, STAFF | Stock transaction audit history |
| **Stock** | `GET` | `/api/v1/stock/history/{id}` | ADMIN, STAFF | Stock transaction details by ID |
| **Dashboard** | `GET` | `/api/v1/dashboard/summary` | ADMIN, STAFF | Real-time aggregate KPI metrics |
| **Dashboard** | `GET` | `/api/v1/dashboard/low-stock`| ADMIN, STAFF | Paginated low-stock items (`stock <= minStock`) |
| **Reports** | `GET` | `/api/v1/reports/stock-transactions` | ADMIN, STAFF | Paginated transaction report with date filtering |
| **Reports** | `GET` | `/api/v1/reports/inventory` | ADMIN, STAFF | Valuation report (`unitPrice × quantity`) |

---

## 5. Consistent API Response Format

Every endpoint returns a unified JSON envelope:

### Success Response (`200 OK` / `201 Created`):
```json
{
  "success": true,
  "message": "Stock added successfully",
  "data": {
    "id": 1,
    "productId": 10,
    "productSku": "PROD-001",
    "productName": "Laptop Sleeve",
    "transactionType": "STOCK_IN",
    "quantity": 5,
    "quantityBefore": 10,
    "quantityAfter": 15,
    "note": "Restock shipment",
    "createdById": 1,
    "createdByUsername": "admin",
    "createdAt": "2026-10-18T08:00:00Z"
  }
}
```

### Error Response (`400`, `401`, `403`, `404`, `409`):
```json
{
  "success": false,
  "message": "Validation failed",
  "data": null,
  "errors": [
    {
      "field": "quantity",
      "message": "Quantity must be greater than zero"
    }
  ]
}
```

---

## 6. Concurrency Control & Data Integrity

1. **Optimistic Locking (`@Version`)**:
   `Product` includes a version field to detect concurrent conflicting updates and throw `ObjectOptimisticLockingFailureException` (translated to HTTP 409 Conflict).
2. **Atomic Inventory Operations (`@Transactional`)**:
   Stock In and Stock Out update the product stock and write the immutable `StockTransaction` record in the same database transaction. If history creation fails, the stock change is rolled back.
3. **Negative Stock Prevention**:
   Stock Out verifies `currentStock >= requestedQuantity` before updating. If stock is insufficient, an `InsufficientStockException` is thrown (HTTP 409 Conflict).
4. **General Product Update Isolation**:
   `PUT /api/v1/products/{id}` strictly prevents client payloads from changing `stockQuantity`. Stock can only be changed via `/api/v1/stock/in` or `/api/v1/stock/out`.

---

## 7. Flutter Integration Guide

### 1. Base URL
* Android Emulator: `http://10.0.2.2:8080`
* iOS Simulator / Desktop: `http://localhost:8080`
* Real Device: `http://<YOUR_LOCAL_IP>:8080`

### 2. Authorization Header
Store the JWT from `/api/v1/auth/login` and attach it to subsequent requests:
```http
Authorization: Bearer <your_jwt_token>
Content-Type: application/json
Accept: application/json
```

### 3. Displaying Field Errors
When receiving HTTP 400 with `"success": false`, parse `errors` array to show inline field errors on Flutter `TextFormField`:
```dart
if (!response['success'] && response['errors'] != null) {
  for (var error in response['errors']) {
    print("Field: ${error['field']}, Error: ${error['message']}");
  }
}
```

---

## 8. Setup & Running the Application

### 1. Prerequisites
* **Java 21 JDK** installed (`java -version`)
* **PostgreSQL** running locally on port `5432`

### 2. Configure Environment Variables
Copy `.env.example` or adjust `application.yaml`:
```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=inventory_management_db
DB_USER=postgres
DB_PASSWORD=pgsql
SERVER_PORT=8080
JWT_SECRET=2abb5fca-2b53-4251-8aa2-b62de8b00135-d8033911-b5bd-4137-9621-01009810bc6d
```

### 3. Run the Backend
```powershell
.\gradlew.bat bootRun
```

### 4. Run Automated Test Suite
```powershell
.\gradlew.bat test
```
To force rerun all tests:
```powershell
.\gradlew.bat test --rerun-tasks
```

---

## 9. Seeded Default Accounts

| Role | Username | Password |
| :--- | :--- | :--- |
| **ADMIN** | `admin` | `Admin@12345` |
| **STAFF** | `staff` | `Staff@12345` |

---

## 10. Swagger UI & API Documentation

Interactive OpenAPI / Swagger UI:
* **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
* **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
