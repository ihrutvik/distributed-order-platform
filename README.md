# Distributed Order Platform

A production-style Java backend that demonstrates reliable order creation and asynchronous processing using Spring Boot, PostgreSQL, Kafka, Redis, Docker, and GitHub Actions.

## Why this project exists

Most portfolio projects show CRUD. This project focuses on backend concerns that matter in production:

- idempotent request handling
- durable database persistence
- asynchronous event processing
- safe duplicate-event handling
- clear order state transitions
- containerized local infrastructure
- health checks, metrics, validation, and CI

## Architecture

```text
Client
  |
  | POST /api/v1/orders + Idempotency-Key
  v
Order API
  |
  +----> PostgreSQL (durable order record)
  |
  +----> Redis (short-lived idempotency lookup)
  |
  +----> Kafka: order-created
                |
                v
          Order Processor
                |
                +----> PROCESSING
                +----> payment/inventory integration point
                +----> COMPLETED
```

PostgreSQL is the source of truth. Redis accelerates repeated idempotency lookups, while a unique database constraint provides the final correctness guarantee. Kafka separates request acceptance from order processing.

## Tech stack

- Java 17
- Spring Boot 3
- Spring Data JPA
- PostgreSQL
- Apache Kafka
- Redis
- Docker Compose
- Maven
- GitHub Actions
- Spring Boot Actuator

## Run locally

```bash
docker compose up --build
```

The API becomes available at `http://localhost:8080`.

Health check:

```bash
curl http://localhost:8080/actuator/health
```

## Create an order

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: checkout-2026-001' \
  -d '{
    "customerId": "customer-42",
    "productCode": "JAVA-BOOK",
    "quantity": 2,
    "amount": 59.98
  }'
```

Sending the same request with the same `Idempotency-Key` returns the existing order instead of creating a duplicate.

## Fetch an order

```bash
curl http://localhost:8080/api/v1/orders/{orderId}
```

## Order lifecycle

```text
PENDING -> PROCESSING -> COMPLETED
                       -> FAILED (planned integration-failure path)
```

The Kafka consumer ignores events for orders that are already completed, making repeated delivery safe.

## Repository structure

```text
src/main/java/dev/hrutvik/orders
├── api          REST controller and exception handling
├── application  order orchestration and Kafka consumer
└── domain       JPA entity and repository
```

## Reliability decisions

**Database-backed idempotency**  
The unique `idempotencyKey` column prevents duplicates even if Redis is unavailable or two requests race.

**Persist before publish**  
The order is saved before the Kafka event is emitted, so consumers never process an order that does not exist.

**Idempotent consumer**  
A completed order is not processed twice when Kafka redelivers an event.

**Observable service**  
Actuator exposes health and metrics endpoints for operational visibility.

## Production improvements

The next iterations will add:

- transactional outbox to remove the database/Kafka dual-write gap
- retry topics and a dead-letter topic
- payment and inventory service adapters
- optimistic locking for concurrent state changes
- Testcontainers integration tests
- OpenAPI documentation
- structured trace and correlation IDs

## CI

Every push and pull request runs:

```bash
mvn verify
```

through GitHub Actions.

## Author

Built by [Hrutvik Nagrale](https://github.com/ihrutvik), a backend engineer focused on Java, Spring Boot, distributed systems, reliability, and scalable APIs.
