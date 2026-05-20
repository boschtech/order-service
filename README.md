# Order Service - Java Spring Boot Microservice

A Spring Boot microservice for order management. Validates products via the product-service before creating orders. Provides a RESTful API with full CORS support.

## Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL database (see [Database Setup](#database-setup))
- product-service running (for order creation product validation)

## Database Setup

This service uses **PostgreSQL** for persistent storage via Spring Data JPA. In production the database is hosted on [Neon](https://neon.tech) (free tier, no expiry).

### Neon (Production / Render)

1. Create a free account at [neon.tech](https://neon.tech).
2. Create a new project (e.g. `order-service`).
3. Copy the **JDBC connection string** from the Neon dashboard.  
   It looks like: `jdbc:postgresql://ep-xxx.region.aws.neon.tech/neondb?user=...&password=...&sslmode=require`
4. Set the `DATABASE_URL` environment variable in the Render dashboard for this service.

### Local Development

Option A — **Local PostgreSQL**:
```bash
createdb order_db
DATABASE_URL=jdbc:postgresql://localhost:5432/order_db mvn spring-boot:run
```

Option B — **Use your Neon database directly**:
```bash
DATABASE_URL="jdbc:postgresql://ep-xxx.region.aws.neon.tech/neondb?user=...&password=...&sslmode=require" mvn spring-boot:run
```

### Tests

Tests use an **H2 in-memory database** automatically — no database setup required.

```bash
mvn test
```

## Build & Run

```bash
mvn clean package
mvn spring-boot:run
```

The service starts on **http://localhost:8081**.

## API Endpoints

| Method | Endpoint                        | Description                    |
|--------|---------------------------------|--------------------------------|
| GET    | /api/orders                     | List all orders                |
| GET    | /api/orders/{id}                | Get order by ID                |
| POST   | /api/orders                     | Create an order                |
| GET    | /api/orders/product/{productId} | Get orders by product ID       |
| GET    | /actuator/health                | Health check                   |

## Microfrontend Integration

### CORS Configuration

The service allows cross-origin requests from these origins by default (configurable in `application.yml`):

- `http://localhost:3000` (React)
- `http://localhost:4200` (Angular)
- `http://localhost:5173` (Vite/Vue)

### Example Fetch (from microfrontend)

```javascript
const response = await fetch('http://localhost:8081/api/orders');
const orders = await response.json();
```

### Example POST

```javascript
await fetch('http://localhost:8081/api/orders', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    productId: 'some-product-id',
    quantity: 2
  })
});
```
