# EventTicket - Reactive Ticketing Event Processing System

Ticket management and event processing system built with reactive architecture using Spring WebFlux, Java 25, DynamoDB, SQS, and asynchronous message processing.

## Technologies

- **Java 25**: Modern language features
  - **Records**: Immutable data carriers for DTOs, Value Objects, and Audit entities
  - **Pattern Matching**: Enhanced `instanceof` and `switch` expressions
  - **Virtual Threads**: Lightweight threads for improved concurrency (enabled via Spring Boot)
  - **Text Blocks & String Templates**: Clean multiline strings with `.formatted()`
  - **Sealed Classes**: Restricted class hierarchies for domain modeling
- **Spring Boot 4.0-M1**: Core framework with Java 25 support and Virtual Threads
- **Spring WebFlux**: Non-blocking reactive API
- **LocalStack**: Complete AWS services emulator
  - **DynamoDB**: Event Sourcing and data persistence
  - **SQS**: Asynchronous message queues
- **AWS SDK v2 Async**: Reactive client for DynamoDB and SQS
- **Redis**: Distributed cache for high performance
- **Docker & Docker Compose**: Containerization

## Docker Compose Services

### 1. Spring Boot Application (`app`)
- **Port**: 8080
- Reactive API with Spring WebFlux
- Connected to LocalStack (DynamoDB + SQS) and Redis

### 2. LocalStack (`localstack`)
- **Gateway Port**: 4566 (all AWS services)
- Complete AWS services emulator
- **Enabled Services**:
  - **DynamoDB**: Event Sourcing and data tables
  - **SQS**: Asynchronous message queues
- Persistence enabled for development
- **Health Check**: http://localhost:4566/_localstack/health

### 3. Redis (`redis`)
- **Port**: 6379
- Distributed cache for high performance
- AOF (Append Only File) persistence

## Quick Start

### Start Services

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f localstack
```

### Stop Services

```bash
# Stop without removing volumes
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

### Rebuild Application

```bash
# Rebuild the application image
docker-compose build app

# Rebuild and restart
docker-compose up -d --build app
```

## Service Access

| Service | URL | Credentials |
|---------|-----|-------------|
| API REST | http://localhost:8080 | - |
| Actuator | http://localhost:8080/actuator | - |
| LocalStack Gateway | http://localhost:4566 | test/test |
| LocalStack Health | http://localhost:4566/_localstack/health | - |
| Redis | localhost:6379 | - |

## API Endpoints

### Base URL
```
http://localhost:8080/api/v1
```

### Events Endpoints

| Method | Endpoint | Description | Status Code |
|--------|----------|-------------|------------|
| `POST` | `/events` | Create a new event | 201 Created |
| `GET` | `/events` | List all events (paginated) | 200 OK |
| `GET` | `/events/{eventId}/availability` | Get real-time availability for an event | 200 OK |
| `POST` | `/events/inventories` | Create ticket inventory for an event | 201 Created |
| `GET` | `/events/{eventId}/inventories` | List all inventory for an event (paginated) | 200 OK |

### Orders Endpoints

| Method | Endpoint | Description | Status Code |
|--------|----------|-------------|------------|
| `POST` | `/orders` | Create a new ticket order | 201 Created |
| `GET` | `/orders/{orderId}` | Get order by ID | 200 OK |
| `POST` | `/orders/{orderId}/confirm` | Confirm order with customer payment info | 200 OK |
| `POST` | `/orders/{orderId}/mark-as-sold` | Mark order as sold (payment completed) | 200 OK |

### API Documentation

For detailed API documentation with request/response examples, see:
- **Postman Collection**: `doc/api.json` (import into Postman)
- **cURL Examples**: `COMANDOS_CURL_PRUEBA.md`

### Example Requests

#### Create Event
```bash
curl -X POST http://localhost:8080/api/v1/events \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Concert 2026",
    "description": "Amazing concert",
    "venue": "Stadium",
    "eventDate": "2026-02-28T18:00:00Z",
    "totalCapacity": 1000
  }'
```

#### Create Order
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-123",
    "eventId": "event-456",
    "eventName": "Concert 2026",
    "ticketType": "VIP",
    "quantity": 2
  }'
```

#### Confirm Order
```bash
curl -X POST http://localhost:8080/api/v1/orders/{orderId}/confirm \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "John Doe",
    "email": "john@example.com",
    "phoneNumber": "+57 300 123 4567",
    "address": "Street 123",
    "city": "Bogotá",
    "country": "Colombia",
    "paymentMethod": "Nequi"
  }'
```

## DynamoDB Data Structure

### Tables

#### 1. **Events** - Event Sourcing
- **Partition Key**: `aggregateId` (HASH)
- **Sort Key**: `version` (RANGE) - Ensures event ordering
- **GSI**: `EventTypeIndex` (eventType + createdAt) - Query by type
- **Attributes**:
  - `aggregateId`: Aggregate ID (TicketOrder, Ticket, etc.)
  - `version`: Event version (1, 2, 3...)
  - `aggregateType`: Aggregate type
  - `eventType`: TicketOrderCreated, TicketReserved, PaymentProcessed, etc.
  - `eventData`: Event data (serialized JSON)
  - `createdAt`: ISO 8601 timestamp
  - `metadata`: Metadata (correlationId, userId, etc.)

#### 2. **TicketOrders** - Ticket Orders
- **Partition Key**: `orderId` (HASH)
- **GSI**: 
  - `CustomerIndex` (customerId + createdAt)
  - `StatusIndex` (status + createdAt)
- **Attributes**:
  - `orderId`: Unique order ID
  - `customerId`: Customer ID
  - `orderNumber`: Human-readable order number
  - `eventId`: Event ID
  - `eventName`: Event name
  - `status`: PENDING, CONFIRMED, PROCESSING, COMPLETED, CANCELLED, FAILED
  - `tickets`: List of ticket items (embedded)
  - `totalAmount`: Total order amount
  - `currency`: Currency (COP, USD, etc.)
  - `createdAt`, `updatedAt`: Timestamps

#### 3. **TicketInventory** - Ticket Inventory
- **Partition Key**: `eventId` (HASH)
- **Sort Key**: `ticketType` (RANGE)
- **Attributes**:
  - `eventId`: Event ID
  - `ticketType`: VIP, GENERAL, etc.
  - `eventName`: Event name
  - `totalQuantity`: Total tickets
  - `availableQuantity`: Available tickets
  - `reservedQuantity`: Reserved tickets
  - `price`: Ticket price
  - `currency`: Currency
  - `version`: Version for optimistic locking

#### 4. **TicketReservations** - Temporary Reservations
- **Partition Key**: `reservationId` (HASH)
- **GSI**: 
  - `OrderIndex` (orderId)
  - `ExpirationIndex` (expiresAt)
- **Attributes**:
  - `reservationId`: Unique reservation ID
  - `orderId`: Associated order ID
  - `eventId`: Event ID
  - `ticketType`: Ticket type
  - `quantity`: Number of tickets
  - `status`: ACTIVE, CONFIRMED, RELEASED, EXPIRED
  - `expiresAt`: Expiration timestamp (Unix epoch)
  - `createdAt`: Creation timestamp

### SQS Queues

**Created Queues**:
1. **ticket-order-queue**: Ticket order processing
2. **ticket-payment-queue**: Payment processing (60s timeout)
3. **ticket-notification-queue**: Notification sending
4. **ticket-dlq**: Dead Letter Queue for failed messages
5. **ticket-order-fifo.fifo**: FIFO queue for ordered processing

See details in: `init-scripts/03-init-localstack.sh`

## Architecture

```
┌─────────────────────────────────────────────┐
│      API REST (Spring WebFlux)              │
│         Port 8080 (Reactive)                │
└──────────────┬──────────────────────────────┘
               │
       ┌───────┴────────┬──────────────┐
       │                │              │
       ▼                ▼              ▼
┌─────────────┐  ┌──────────────────┐  ┌──────────┐
│  Redis      │  │   LocalStack     │  │  Spring  │
│  (Cache)    │  │  ┌────────────┐  │  │  Cloud   │
│             │  │  │ DynamoDB   │  │  │  AWS     │
└─────────────┘  │  │  (Tables)  │  │  └──────────┘
                 │  └────────────┘  │
                 │  ┌────────────┐  │
                 │  │    SQS     │  │
                 │  │  (Queues)  │  │
                 │  └────────────┘  │
                 └──────────────────┘
                       Port 4566
```

### Responsibility Separation

| Technology | Use Case | Reason |
|------------|----------|--------|
| **DynamoDB** | Event Sourcing, Orders, Inventory | Append-only, high write throughput, scalability |
| **SQS** | Async Messaging | Decoupling, distributed processing, auto-retry |
| **Redis** | Cache | Low latency, temporary data |

### Architecture Features

- **No Blocking**: DynamoDB with optimistic concurrency
- **NoSQL Native**: DynamoDB for all data persistence
- **Async Flows**: SQS for message orchestration
- **End-to-End Reactive**: AWS SDK v2 Async (DynamoDB + SQS)
- **Event Sourcing**: Immutable events in DynamoDB with GSI
- **High Availability**: DynamoDB partitioning + SQS distributed queues
- **Local Development**: LocalStack emulates AWS at no cost

### Clean Architecture Layers

The application follows **Clean Architecture** principles with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────┐
│              Infrastructure Layer                        │
│  (Controllers, Repositories, Messaging, Config)          │
│  - EventController, TicketOrderController                 │
│  - DynamoDB Repositories                                 │
│  - SQS Consumers/Publishers                               │
└──────────────────┬──────────────────────────────────────┘
                   │ depends on
┌──────────────────▼──────────────────────────────────────┐
│              Application Layer                           │
│  (Use Cases, DTOs)                                      │
│  - CreateEventUseCase, CreateTicketOrderUseCase         │
│  - ConfirmTicketOrderUseCase, ProcessTicketOrderUseCase │
│  - Request/Response DTOs                                │
└──────────────────┬──────────────────────────────────────┘
                   │ depends on
┌──────────────────▼──────────────────────────────────────┐
│              Domain Layer                                │
│  (Entities, Value Objects, Repository Interfaces)        │
│  - TicketOrder, TicketInventory, Event                   │
│  - Money, OrderId, CustomerId, EventId                   │
│  - Repository Interfaces (Ports)                        │
│  - Domain Exceptions                                     │
└─────────────────────────────────────────────────────────┘
```

**Key Principles**:
- **Dependency Inversion**: Domain has zero dependencies on outer layers
- **SOLID Principles**: Single Responsibility, Open/Closed, Liskov, Interface Segregation, Dependency Inversion
- **Domain-Driven Design**: Rich domain model with business logic in entities
- **Hexagonal Architecture**: Adapters (controllers, repositories) adapt external world to domain

## Application Flow

### 1. Event Creation Flow

```
User Request
    │
    ▼
POST /api/v1/events
    │
    ▼
EventController.createEvent()
    │
    ▼
CreateEventUseCase.execute()
    │
    ├─► Validates request
    ├─► Creates Event domain entity
    ├─► Saves to DynamoDB (EventRepository)
    └─► Returns EventResponse
```

### 2. Ticket Order Creation Flow (Async Processing)

```
User Request
    │
    ▼
POST /api/v1/orders
    │
    ▼
TicketOrderController.createOrder()
    │
    ▼
CreateTicketOrderUseCase.execute()
    │
    ├─► Validates inventory availability
    ├─► Reserves tickets (optimistic locking)
    ├─► Creates TicketOrder (status: RESERVED)
    ├─► Creates TicketReservation (10 min timeout)
    ├─► Saves tickets to TicketItems table
    ├─► Publishes message to SQS (ticket-order-queue)
    └─► Returns OrderResponse immediately
    
    [Async Processing - Background]
    │
    ▼
SqsOrderConsumer.pollAndProcessMessages()
    │ (runs every 5 seconds)
    ▼
ProcessTicketOrderUseCase.execute()
    │
    ├─► Validates real-time availability
    ├─► Updates inventory (optimistic locking)
    ├─► Changes order status: RESERVED → PENDING_CONFIRMATION
    └─► Updates order in DynamoDB
```

### 3. Order Confirmation Flow

```
User Request
    │
    ▼
POST /api/v1/orders/{orderId}/confirm
    │
    ▼
TicketOrderController.confirmOrder()
    │
    ▼
ConfirmTicketOrderUseCase.execute()
    │
    ├─► Loads order from repository
    ├─► Validates order status (must be RESERVED or PENDING_CONFIRMATION)
    ├─► Saves customer information (CustomerInfoRepository)
    ├─► Updates order status to PENDING_CONFIRMATION
    ├─► Updates ticket statuses to PENDING_CONFIRMATION
    └─► Returns updated OrderResponse
```

### 4. Mark Order as Sold Flow

```
User Request
    │
    ▼
POST /api/v1/orders/{orderId}/mark-as-sold
    │
    ▼
TicketOrderController.markOrderAsSold()
    │
    ▼
MarkOrderAsSoldUseCase.execute()
    │
    ├─► Loads order (must be PENDING_CONFIRMATION)
    ├─► Assigns unique seat numbers to tickets
    ├─► Updates order status to SOLD
    ├─► Updates ticket statuses to SOLD (final state)
    ├─► Updates inventory (decrements available, increments sold)
    ├─► Releases reservation
    └─► Returns final OrderResponse
```

### 5. Reservation Expiration Flow (Scheduled)

```
Scheduler (every 1 minute)
    │
    ▼
ReleaseExpiredReservationsUseCase.execute()
    │
    ├─► Queries reservations where expiresAt < now()
    ├─► For each expired reservation:
    │   ├─► Returns tickets to inventory (optimistic locking)
    │   ├─► Updates reservation status to EXPIRED
    │   ├─► Updates order status to CANCELLED (if applicable)
    │   └─► Logs release action
    └─► Returns count of released reservations
```

### 6. Availability Query Flow (Reactive)

```
User Request
    │
    ▼
GET /api/v1/events/{eventId}/availability
    │
    ▼
EventController.getEventAvailability()
    │
    ▼
GetEventAvailabilityUseCase.execute()
    │
    ├─► Loads event from DynamoDB
    ├─► Queries inventory for all ticket types
    ├─► Calculates real-time availability
    │   (totalQuantity - reservedQuantity - soldQuantity)
    └─► Returns EventAvailabilityResponse
```

### 7. Ticket Status State Machine

```
AVAILABLE (Initial)
    │
    ├─► RESERVED (User initiates purchase)
    │       │
    │       ├─► PENDING_CONFIRMATION (Payment processing)
    │       │       │
    │       │       ├─► SOLD (Payment confirmed) [FINAL]
    │       │       └─► AVAILABLE (Payment failed)
    │       │
    │       └─► AVAILABLE (Reservation expired)
    │
    └─► COMPLIMENTARY (Admin assigned) [FINAL]
```

**State Rules**:
- **SOLD** and **COMPLIMENTARY** are final states (irreversible)
- Only **SOLD** tickets count as revenue
- All state transitions are atomic and auditable
- See `TICKET_STATUS_FLOW.md` for detailed rules

## Testing

### Test Structure

The project includes comprehensive tests organized by layer:

```
src/test/java/com/eventticket/
├── application/
│   ├── dto/                          # DTO tests
│   │   ├── PagedEventResponseTest.java
│   │   └── PagedInventoryResponseTest.java
│   └── usecase/                      # Use case tests
│       ├── ConcurrencyTest.java      # Concurrent order creation
│       ├── CreateEventUseCaseTest.java
│       ├── CreateInventoryUseCaseTest.java
│       ├── CreateTicketOrderUseCaseTest.java
│       ├── ConfirmTicketOrderUseCaseTest.java
│       ├── GetEventAvailabilityUseCaseTest.java
│       ├── GetInventoryUseCaseTest.java
│       ├── GetTicketOrderUseCaseTest.java
│       ├── ListEventsUseCaseTest.java
│       ├── MarkOrderAsSoldUseCaseTest.java
│       ├── ProcessTicketOrderUseCaseTest.java
│       └── ReleaseExpiredReservationsUseCaseTest.java
└── infrastructure/
    ├── api/                          # Controller tests
    │   ├── EventControllerTest.java
    │   └── GlobalExceptionHandlerTest.java
    └── repository/                   # Repository integration tests
        ├── DynamoDBEventRepositoryTest.java
        ├── DynamoDBCustomerInfoRepositoryTest.java
        ├── DynamoDBTicketInventoryRepositoryTest.java
        ├── DynamoDBTicketItemRepositoryTest.java
        ├── DynamoDBTicketOrderRepositoryTest.java
        ├── DynamoDBTicketReservationRepositoryTest.java
        └── DynamoDBTicketStateTransitionAuditRepositoryTest.java
```

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=CreateTicketOrderUseCaseTest

# Run tests with coverage report
mvn clean test jacoco:report

# View coverage report
# Open: target/site/jacoco/index.html
```

### Test Coverage

Current coverage status (see `COVERAGE_REPORT.md` for details):

- **Overall Coverage**: ~11% (target: 90%)
- **Best Coverage**:
  - `application.dto`: 57%
  - `infrastructure.api`: 64%
  - `domain.exception`: 46%
- **Needs Improvement**:
  - `infrastructure.repository`: 0% (critical)
  - `domain.model`: 9%
  - `application.usecase`: 13%
  - `infrastructure.messaging`: 0%

### Test Types

#### 1. Unit Tests
- **Location**: `application/usecase/`, `application/dto/`
- **Purpose**: Test business logic in isolation
- **Tools**: JUnit 5, Mockito, Reactor Test
- **Example**: `CreateTicketOrderUseCaseTest` - Tests order creation with mocked repositories

#### 2. Integration Tests
- **Location**: `infrastructure/repository/`
- **Purpose**: Test repository implementations with real DynamoDB (LocalStack)
- **Tools**: JUnit 5, Testcontainers (LocalStack), AWS SDK v2
- **Example**: `DynamoDBTicketOrderRepositoryTest` - Tests CRUD operations on DynamoDB

#### 3. API Tests
- **Location**: `infrastructure/api/`
- **Purpose**: Test REST endpoints end-to-end
- **Tools**: WebTestClient, Mockito
- **Example**: `EventControllerTest` - Tests HTTP requests/responses

#### 4. Concurrency Tests
- **Location**: `application/usecase/ConcurrencyTest.java`
- **Purpose**: Test concurrent order creation and inventory updates
- **Tools**: JUnit 5, Reactor Test, Virtual Threads
- **Scenario**: Multiple users trying to purchase the same tickets simultaneously

### Test Configuration

Tests use **LocalStack** for AWS services emulation:
- DynamoDB tables created automatically
- SQS queues initialized
- No real AWS account needed
- Fast and isolated test execution

### Viewing Test Results

```bash
# Test reports location
target/surefire-reports/

# Coverage report location
target/site/jacoco/index.html

# View in browser
python3 -m http.server 8000 --directory target/site/jacoco
# Open: http://localhost:8000
```

## AWS CLI Commands with LocalStack

### Configure awslocal alias (optional but recommended)

```bash
alias awslocal="aws --endpoint-url=http://localhost:4566 --region=us-east-1"
```

### DynamoDB Commands

```bash
# List tables
awslocal dynamodb list-tables

# Scan events
awslocal dynamodb scan --table-name Events --max-items 10

# Scan ticket orders
awslocal dynamodb scan --table-name TicketOrders

# Scan inventory
awslocal dynamodb scan --table-name TicketInventory

# Query by customer
awslocal dynamodb query \
  --table-name TicketOrders \
  --index-name CustomerIndex \
  --key-condition-expression "customerId = :cid" \
  --expression-attribute-values '{":cid": {"S": "customer-001"}}'

# Get specific order
awslocal dynamodb get-item \
  --table-name TicketOrders \
  --key '{"orderId": {"S": "order-001"}}'
```

### SQS Commands

```bash
# List queues
awslocal sqs list-queues

# Get queue URL
awslocal sqs get-queue-url --queue-name ticket-order-queue

# Receive messages
QUEUE_URL=$(awslocal sqs get-queue-url --queue-name ticket-order-queue --output text --query 'QueueUrl')
awslocal sqs receive-message --queue-url $QUEUE_URL --max-number-of-messages 10

# Send message
awslocal sqs send-message \
  --queue-url $QUEUE_URL \
  --message-body '{"eventId":"concert-001","ticketCount":2}'

# Get queue attributes
awslocal sqs get-queue-attributes --queue-url $QUEUE_URL --attribute-names All

# Purge queue (delete all messages)
awslocal sqs purge-queue --queue-url $QUEUE_URL
```

## Verification Commands

### Verify Services

```bash
# LocalStack Health Check
curl http://localhost:4566/_localstack/health | jq

# Redis
docker-compose exec redis redis-cli ping

# Application Health
curl http://localhost:8080/actuator/health
```

### View Service Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f localstack
docker-compose logs -f app
docker-compose logs -f redis
```

## Troubleshooting

### Containers won't start

```bash
# Check service status
docker-compose ps

# View error logs
docker-compose logs
```

### LocalStack not responding

```bash
# View LocalStack logs
docker-compose logs -f localstack

# Restart LocalStack
docker-compose restart localstack

# Check enabled services
curl http://localhost:4566/_localstack/health | jq

# Re-run initialization script
docker-compose exec localstack sh /etc/localstack/init/ready.d/03-init-localstack.sh
```

### Clean everything and start fresh

```bash
docker-compose down -v
docker system prune -a
docker-compose up -d --build
```

## Next Steps

1. Create Spring Boot project structure
2. Implement reactive controllers with WebFlux
3. Configure DynamoDB repositories with AWS SDK v2 Async
4. Implement Event Store with DynamoDB
5. Configure SQS listeners for async message processing
6. Implement optimistic locking for ticket inventory
7. Configure Redis Reactive for caching
8. Add tests with WebTestClient and Testcontainers (LocalStack)

## Why This Architecture with LocalStack

### LocalStack - AWS Emulator

- **Local Development**: No AWS account or costs needed
- **Multiple Services**: DynamoDB + SQS (and 90+ more services)
- **Persistence**: Data persists between restarts
- **API Compatible**: 100% compatible with AWS SDK
- **Testing**: Perfect for integration tests
- **CI/CD Ready**: Easy integration in pipelines

### SQS vs RabbitMQ

| Feature | SQS (LocalStack) | RabbitMQ |
|---------|------------------|----------|
| Simplicity | Very simple | More complex |
| Scalability | Unlimited (AWS) | Requires config |
| Dead Letter Queue | Native | Configurable |
| FIFO | Native support | Durable queues |
| Auto-Retry | Built-in | Manual |
| Visibility Timeout | Native | Manual |
| Delay Queues | Built-in | Plugins |
| Cloud Ready | AWS direct | Requires hosting |
| Local Development | LocalStack | Docker |

### DynamoDB for Everything

- **Event Sourcing**: Perfect append-only design
- **Partition Key + Sort Key**: `aggregateId` + `version` ensures order
- **No Conflicts**: Concurrent writes to different partitions
- **GSI**: Global Secondary Indexes for complex queries
- **Scalability**: Auto-scaling with no practical limits
- **Single-Table Design**: Optional pattern for high performance
- **Optimistic Locking**: Version field for inventory
- **TTL**: Automatic expiration for reservations (can be enabled)

### Production Stack Benefits

| Component | Development | Production |
|-----------|-------------|------------|
| **DynamoDB** | LocalStack | AWS DynamoDB |
| **SQS** | LocalStack | AWS SQS |
| **Redis** | Docker | AWS ElastiCache |
| **LocalStack** | Dev/Test | Real AWS |

**Zero code changes needed when moving to production!**

## Project Description

**EventTicket** is a ticketing event processing system that demonstrates:

- **Concurrent Operations Without Blocking**: Using DynamoDB with optimistic concurrency
- **NoSQL Persistence**: DynamoDB for fast data access
- **Async Flows**: Orchestration via SQS between components
- **Reactive Architecture**: Spring WebFlux with non-blocking end-to-end programming
- **Event Sourcing**: All ticketing events stored immutably in DynamoDB
- **Java 25**: Leveraging Records, Pattern Matching, and Virtual Threads

## Environment Variables

Create a `.env` file (optional):

```bash
# AWS LocalStack
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=test
AWS_SECRET_ACCESS_KEY=test
AWS_ENDPOINT_URL=http://localhost:4566

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Application
APP_PORT=8080
SPRING_PROFILES_ACTIVE=docker
```

## Security Note

This setup is for **development only**. For production:
- Use real AWS credentials with IAM roles
- Enable DynamoDB encryption at rest
- Use VPC endpoints for private networking
- Enable SQS server-side encryption
- Use Redis AUTH and TLS
- Implement proper authentication/authorization

## License

Development project for EventTicket.
