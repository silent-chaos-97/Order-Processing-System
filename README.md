# Order Processing System

A backend REST API for an e-commerce order processing system, built with **Java 17**, **Spring Boot 3**, **Spring Data JPA**, and **PostgreSQL**.

## Features

- Create an order with one or more line items (total amount computed server-side)
- Retrieve order details by ID
- List all orders, optionally filtered by status
- Update an order's status, enforcing valid status transitions
- Cancel an order (only allowed while `PENDING`)
- Background job that automatically promotes `PENDING` orders to `PROCESSING` every 5 minutes

## Tech Stack

- Java 17, Spring Boot 3.5
- Spring Web, Spring Data JPA, Spring Validation, Spring Scheduling
- PostgreSQL (runtime), H2 (tests)
- Flyway for database migrations
- Lombok
- JUnit 5, Mockito, MockMvc

## Order Status Model

```
PENDING ──► PROCESSING ──► SHIPPED ──► DELIVERED
   │
   └──► CANCELLED
```

- A new order starts as `PENDING`.
- Every 5 minutes, a scheduled job promotes all `PENDING` orders to `PROCESSING`.
- An order can only be cancelled while it is `PENDING`.
- Status updates must follow the transitions above; any other transition returns `409 Conflict`.

## Getting Started

### Prerequisites

- Java 17+
- Docker (for PostgreSQL)

### 1. Start PostgreSQL

```bash
docker compose up -d
```

This starts a `postgres:16-alpine` container with database `orderdb`, user `orderuser`, password `orderpass`, exposed on `localhost:5432`. Flyway will create the schema automatically on application startup.

### 2. Run the application

```bash
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.

#### Running without Docker

If you don't have Docker available, run with the `local` profile, which uses an in-memory H2 database instead of PostgreSQL:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### 3. Run the tests

```bash
./mvnw test
```

Tests run against an in-memory H2 database (Postgres-compatible mode) and do not require Docker.

## API Reference

Base path: `/api/orders`

### Create an order

```
POST /api/orders
Content-Type: application/json

{
  "customerName": "Jane Doe",
  "customerEmail": "jane@example.com",
  "items": [
    { "productName": "Widget", "quantity": 2, "unitPrice": 9.99 },
    { "productName": "Gadget", "quantity": 1, "unitPrice": 19.99 }
  ]
}
```

`201 Created`

```json
{
  "id": "5f3c1c2a-2b0c-4e7a-9c3b-9b6e2d4f5a11",
  "customerName": "Jane Doe",
  "customerEmail": "jane@example.com",
  "status": "PENDING",
  "totalAmount": 39.97,
  "createdAt": "2026-06-13T10:00:00Z",
  "updatedAt": "2026-06-13T10:00:00Z",
  "items": [
    { "id": "...", "productName": "Widget", "quantity": 2, "unitPrice": 9.99, "subtotal": 19.98 },
    { "id": "...", "productName": "Gadget", "quantity": 1, "unitPrice": 19.99, "subtotal": 19.99 }
  ]
}
```

Validation errors return `400 Bad Request` with a `fieldErrors` map.

### Get order details

```
GET /api/orders/{orderId}
```

`200 OK` with the order, or `404 Not Found` if it doesn't exist.

### List orders

```
GET /api/orders
GET /api/orders?status=PENDING
```

`200 OK` with an array of orders. The `status` query parameter is optional and must be one of `PENDING`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`.

### Update order status

```
PATCH /api/orders/{orderId}/status
Content-Type: application/json

{ "status": "PROCESSING" }
```

`200 OK` with the updated order, `404 Not Found` if the order doesn't exist, or `409 Conflict` if the transition isn't allowed.

### Cancel an order

```
POST /api/orders/{orderId}/cancel
```

`200 OK` with the cancelled order, `404 Not Found` if the order doesn't exist, or `409 Conflict` if the order is not `PENDING`.

## Project Structure

```
src/main/java/com/winter/orderprocessing/
├── controller/      REST controllers
├── service/         Business logic
├── repository/      Spring Data JPA repositories
├── model/           JPA entities (Order, OrderItem, OrderStatus)
├── dto/             Request/response payloads
├── mapper/          Entity <-> DTO mapping
├── exception/       Custom exceptions + global exception handler
└── scheduler/       Background job (PENDING -> PROCESSING)

src/main/resources/
├── application.yml
└── db/migration/    Flyway migrations
```
