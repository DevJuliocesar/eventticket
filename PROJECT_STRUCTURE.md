# EventTicket - Clean Architecture Project Structure

## 📐 Architecture Overview

This project follows **Clean Architecture** principles with clear separation of concerns across three main layers:

```
┌─────────────────────────────────────────────────────────┐
│                   Presentation Layer                     │
│              (Controllers, DTOs, Handlers)               │
└──────────────────┬──────────────────────────────────────┘
                   │ depends on
┌──────────────────▼──────────────────────────────────────┐
│                  Application Layer                       │
│           (Use Cases, Application Services)              │
└──────────────────┬──────────────────────────────────────┘
                   │ depends on
┌──────────────────▼──────────────────────────────────────┐
│                   Domain Layer                           │
│   (Entities, Value Objects, Domain Services, Ports)      │
└─────────────────────────────────────────────────────────┘
                   ▲
                   │ implemented by
┌──────────────────┴──────────────────────────────────────┐
│                Infrastructure Layer                      │
│  (Repositories, External Services, Messaging, Config)    │
└─────────────────────────────────────────────────────────┘
```

## 🗂️ Project Structure

```
src/main/java/com/eventticket/
│
├── domain/                                # 🟢 Domain Layer (Core Business Logic)
│   ├── model/                            # Domain entities
│   │   ├── TicketOrder.java             # Order aggregate root
│   │   ├── TicketInventory.java         # Inventory aggregate
│   │   ├── TicketReservation.java       # Reservation entity
│   │   ├── TicketItem.java              # Value object
│   │   ├── OrderStatus.java             # Enum
│   │   └── ReservationStatus.java       # Enum
│   │
│   ├── valueobject/                      # Value Objects (DDD)
│   │   ├── Money.java                   # Monetary value
│   │   ├── OrderId.java                 # Type-safe ID
│   │   ├── CustomerId.java              # Type-safe ID
│   │   ├── EventId.java                 # Type-safe ID
│   │   ├── TicketId.java                # Type-safe ID
│   │   └── ReservationId.java           # Type-safe ID
│   │
│   ├── repository/                       # Repository interfaces (Ports)
│   │   ├── TicketOrderRepository.java
│   │   ├── TicketInventoryRepository.java
│   │   └── TicketReservationRepository.java
│   │
│   └── exception/                        # Domain exceptions
│       ├── DomainException.java
│       ├── OrderNotFoundException.java
│       └── InsufficientInventoryException.java
│
├── application/                           # 🔵 Application Layer (Use Cases)
│   ├── usecase/                          # Use case implementations
│   │   ├── CreateTicketOrderUseCase.java
│   │   ├── ConfirmTicketOrderUseCase.java
│   │   └── GetTicketOrderUseCase.java
│   │
│   └── dto/                              # Data Transfer Objects
│       ├── CreateOrderRequest.java
│       ├── OrderResponse.java
│       └── TicketItemResponse.java
│
├── infrastructure/                        # 🟡 Infrastructure Layer (Adapters)
│   ├── api/                              # REST Controllers
│   │   ├── TicketOrderController.java
│   │   └── GlobalExceptionHandler.java
│   │
│   ├── persistence/                       # Repository implementations
│   │   ├── dynamodb/
│   │   │   ├── DynamoDBTicketOrderRepository.java
│   │   │   ├── DynamoDBTicketInventoryRepository.java
│   │   │   ├── DynamoDBTicketReservationRepository.java
│   │   │   └── mapper/
│   │   │       ├── TicketOrderMapper.java
│   │   │       └── TicketInventoryMapper.java
│   │   └── entity/
│   │       ├── TicketOrderEntity.java
│   │       └── TicketInventoryEntity.java
│   │
│   ├── messaging/                         # SQS consumers & publishers
│   │   ├── consumer/
│   │   │   ├── TicketOrderConsumer.java
│   │   │   └── PaymentConsumer.java
│   │   └── publisher/
│   │       └── TicketOrderPublisher.java
│   │
│   ├── cache/                             # Redis cache implementation
│   │   └── RedisCacheService.java
│   │
│   └── config/                            # Configuration classes
│       ├── AwsConfig.java
│       ├── DynamoDBConfig.java
│       ├── SqsConfig.java
│       ├── RedisConfig.java
│       └── WebFluxConfig.java
│
└── EventTicketApplication.java            # Main application class
```

## 🎯 SOLID Principles Applied

### 1. **Single Responsibility Principle (SRP)**
- Each use case has **one responsibility** (e.g., `CreateTicketOrderUseCase` only creates orders)
- Domain entities manage their own business rules
- Controllers only handle HTTP concerns

### 2. **Open/Closed Principle (OCP)**
- Domain entities are **immutable** (with methods for modifications)
- Use of interfaces (`TicketOrderRepository`) allows extension without modification
- Strategy pattern for different payment methods (extensible)

### 3. **Liskov Substitution Principle (LSP)**
- All repository implementations can replace their interfaces
- Value objects are interchangeable where their base type is used

### 4. **Interface Segregation Principle (ISP)**
- Repository interfaces are **specific** to each aggregate
- Use cases depend only on the methods they need
- No "fat" interfaces with unused methods

### 5. **Dependency Inversion Principle (DIP)**
- Application layer depends on **abstractions** (repository interfaces)
- Infrastructure implements those abstractions
- Domain has **zero** dependencies on outer layers

## 🔄 Design Patterns Used

### 1. **Repository Pattern**
- Abstracts data access logic
- `TicketOrderRepository` interface in domain
- `DynamoDBTicketOrderRepository` implementation in infrastructure

### 2. **Value Object Pattern**
- Immutable objects representing domain concepts
- `Money`, `OrderId`, `CustomerId`, etc.
- Encapsulate validation and business logic

### 3. **Aggregate Pattern (DDD)**
- `TicketOrder` is an aggregate root
- Maintains consistency boundaries
- Controls access to entities within the aggregate

### 4. **Factory Pattern**
- Static factory methods in domain entities
- `TicketOrder.create(...)` ensures valid object creation
- Encapsulates complex construction logic

### 5. **Strategy Pattern**
- Different inventory reservation strategies
- Pluggable payment processors (future)

### 6. **Adapter Pattern (Hexagonal Architecture)**
- Controllers adapt HTTP requests to use cases
- Repository implementations adapt DynamoDB to domain

### 7. **Command Pattern**
- Use cases represent commands
- `CreateTicketOrderUseCase.execute()` encapsulates operation

### 8. **Builder Pattern**
- Lombok `@Builder` for DTOs and domain objects
- Fluent API for object construction

## 📦 Layer Dependencies

```
Infrastructure  ──┐
                  ├──> Application ──> Domain
   API          ──┘

✅ Domain has ZERO dependencies
✅ Application depends only on Domain
✅ Infrastructure depends on Application & Domain
✅ Dependency direction: Inward towards Domain
```

## 🔐 Key Features

### Domain Layer
- **Pure business logic** - No frameworks
- **Immutable entities** - Thread-safe by design
- **Rich domain model** - Business rules in entities
- **Type-safe IDs** - Prevent ID mixing errors
- **Value objects** - Encapsulate complex values

### Application Layer
- **Use case orchestration** - Coordinate domain objects
- **Transaction boundaries** - Define consistency boundaries
- **DTO mapping** - Convert between layers
- **Validation** - Input validation with Jakarta Validation

### Infrastructure Layer
- **DynamoDB integration** - Reactive NoSQL access
- **SQS messaging** - Async event processing
- **Redis caching** - Performance optimization
- **REST API** - WebFlux reactive endpoints
- **Configuration** - Spring Boot auto-configuration

## 🚀 Benefits of This Architecture

1. **Testability**: Business logic isolated and easy to test
2. **Maintainability**: Clear boundaries between layers
3. **Flexibility**: Easy to swap implementations
4. **Scalability**: Reactive, non-blocking throughout
5. **Domain-Centric**: Business rules in one place
6. **Independence**: Domain free from frameworks

## 📝 Naming Conventions

- **Entities**: Nouns (TicketOrder, TicketInventory)
- **Value Objects**: Descriptive nouns (Money, OrderId)
- **Use Cases**: Verb + Noun + UseCase (CreateTicketOrderUseCase)
- **Repositories**: Noun + Repository (TicketOrderRepository)
- **DTOs**: Noun + Request/Response (CreateOrderRequest)
- **Controllers**: Noun + Controller (TicketOrderController)

## 🧪 Testing Strategy

```
Unit Tests (Domain)     ──> Test business logic
Integration Tests (App) ──> Test use case orchestration
E2E Tests (Infra)      ──> Test full stack with TestContainers
```

## 📚 Further Reading

- [Clean Architecture (Robert C. Martin)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Domain-Driven Design (Eric Evans)](https://www.domainlanguage.com/ddd/)
- [Hexagonal Architecture (Alistair Cockburn)](https://alistair.cockburn.us/hexagonal-architecture/)
