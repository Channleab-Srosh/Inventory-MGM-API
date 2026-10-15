# Inventory Management System REST API

A secure, professional, and maintainable REST API backend for an Inventory Management System built with **Spring Boot 3** and **Java 21**.

## Tech Stack
* **Java**: 21 (LTS)
* **Framework**: Spring Boot
* **Build Tool**: Gradle
* **Database**: PostgreSQL with Flyway Migrations
* **Security**: Spring Security + JWT (HMAC-SHA)
* **Documentation**: SpringDoc OpenAPI / Swagger UI (v3)
* **Testing**: JUnit 5, MockMvc, AssertJ

---

## Getting Started

### 1. Prerequisites
* **Java 21 JDK** installed
* **PostgreSQL** running locally on port `5432` with a database named `inventory_management_db`

### 2. Configure Environment Variables
Copy `.env.example` or set the following environment variables (or rely on the sensible defaults in `src/main/resources/application.yaml`):

```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=inventory_management_db
DB_USER=postgres
DB_PASSWORD=pgsql
JWT_SECRET=2abb5fca-2b53-4251-8aa2-b62de8b00135-d8033911-b5bd-4137-9621-01009810bc6d
```

### 3. Run the Application
```bash
./gradlew bootRun
```
*(On Windows: `.\gradlew.bat bootRun`)*

### 4. Run Automated Tests
```bash
./gradlew test
```
*(On Windows: `.\gradlew.bat test`)*

---

## Default Accounts
Upon first boot, `DataInitializer` seeds the following accounts:

| Role | Username | Default Password | Permissions |
| :--- | :--- | :--- | :--- |
| **ADMIN** | `admin` | `Admin@12345` | Full system access (Users, Categories, Suppliers, Products, Stock) |
| **STAFF** | `staff` | `Staff@12345` | Permitted inventory operations & viewing data |

---

## API Documentation (Swagger UI)
Once started, access the interactive Swagger UI at:
* **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
* **OpenAPI Docs**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

To test secured endpoints in Swagger:
1. Call `POST /api/v1/auth/login` with your credentials.
2. Copy the token from `data.token`.
3. Click the **Authorize** button in Swagger UI and paste the token.
