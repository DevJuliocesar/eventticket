# Enhanced Architecture Diagram - EventTicket System

## General System Architecture (Detailed)

```mermaid
graph TB
    CLIENT[HTTP Client<br/>REST API<br/>Spring WebFlux]
    
    subgraph "Spring Boot Application - Port 8080"
        subgraph "Infrastructure Layer"
            direction TB
            API[API Controllers<br/>━━━━━━━━━━━━━━━━<br/>EventController<br/>TicketOrderController<br/>GlobalExceptionHandler]
            REPO[Repository Implementations<br/>━━━━━━━━━━━━━━━━<br/>DynamoDBEventRepository<br/>DynamoDBTicketOrderRepository<br/>DynamoDBTicketInventoryRepository<br/>DynamoDBTicketReservationRepository<br/>DynamoDBTicketItemRepository<br/>DynamoDBCustomerInfoRepository<br/>DynamoDBTicketStateTransitionAuditRepository]
            MSG[Messaging<br/>━━━━━━━━━━━━━━━━<br/>SqsOrderPublisher<br/>SqsOrderConsumer<br/>OrderMessage]
            SCHED["Scheduler<br/>━━━━━━━━━━━━━━━━<br/>ReservationExpirationScheduler<br/>@Scheduled every 1min"]
            CONFIG[Configuration<br/>━━━━━━━━━━━━━━━━<br/>DynamoDBConfig<br/>SqsConfig<br/>SchedulerConfig<br/>RedisConfig]
        end
        
        subgraph "Application Layer"
            direction TB
            UC[Use Cases - 11 Cases<br/>━━━━━━━━━━━━━━━━<br/>CreateEventUseCase<br/>ListEventsUseCase<br/>GetEventAvailabilityUseCase<br/>CreateInventoryUseCase<br/>GetInventoryUseCase<br/>CreateTicketOrderUseCase<br/>ProcessTicketOrderUseCase<br/>ConfirmTicketOrderUseCase<br/>MarkOrderAsSoldUseCase<br/>GetTicketOrderUseCase<br/>ReleaseExpiredReservationsUseCase]
            DTO[DTOs<br/>━━━━━━━━━━━━━━━━<br/>Request: CreateEventRequest<br/>CreateOrderRequest<br/>ConfirmOrderRequest<br/>━━━━━━━━━━━━━━━━<br/>Response: EventResponse<br/>OrderResponse<br/>InventoryResponse<br/>PagedEventResponse]
        end
        
        subgraph "Domain Layer"
            direction TB
            ENT[Domain Entities<br/>━━━━━━━━━━━━━━━━<br/>Event<br/>TicketOrder<br/>TicketInventory<br/>TicketReservation<br/>TicketItem<br/>CustomerInfo]
            VO[Value Objects<br/>━━━━━━━━━━━━━━━━<br/>EventId<br/>OrderId<br/>CustomerId<br/>Money<br/>TicketId<br/>ReservationId]
            REPO_INT[Repository Interfaces<br/>Ports<br/>━━━━━━━━━━━━━━━━<br/>EventRepository<br/>TicketOrderRepository<br/>TicketInventoryRepository<br/>TicketReservationRepository<br/>TicketItemRepository<br/>CustomerInfoRepository<br/>TicketStateTransitionAuditRepository]
            EXC[Domain Exceptions<br/>━━━━━━━━━━━━━━━━<br/>InsufficientInventoryException<br/>OrderNotFoundException<br/>InvalidTicketStateTransitionException<br/>DomainException]
        end
    end

    subgraph "External Services"
        DDB[(DynamoDB<br/>LocalStack:4566<br/>━━━━━━━━━━━━━━━━<br/>EventAggregates<br/>TicketOrders<br/>TicketInventory<br/>TicketReservations<br/>TicketItems<br/>CustomerInfo<br/>TicketStateTransitions)]
        SQS[SQS Queues<br/>LocalStack:4566<br/>━━━━━━━━━━━━━━━━<br/>ticket-order-queue<br/>ticket-dlq]
        REDIS[(Redis Cache<br/>Port 6379<br/>━━━━━━━━━━━━━━━━<br/>Distributed Cache<br/>High Performance)]
    end

    CLIENT -->|HTTP/REST| API
    API -->|execute| UC
    UC -->|uses| ENT
    UC -->|uses| VO
    UC -->|uses| DTO
    UC -->|calls| REPO_INT
    UC -->|throws| EXC
    UC -->|publishes| MSG
    SCHED -->|triggers| UC
    REPO_INT -.->|implements| REPO
    REPO -->|persists| DDB
    MSG -->|sends| SQS
    SQS -->|consumes| MSG
    UC -.->|cache| REDIS
    CONFIG -->|configures| REPO
    CONFIG -->|configures| MSG
    
    style CLIENT fill:#e1f5ff,stroke:#01579b,stroke-width:2px
    style API fill:#fff4e1,stroke:#e65100,stroke-width:2px
    style REPO fill:#fff4e1,stroke:#e65100,stroke-width:2px
    style MSG fill:#fff4e1,stroke:#e65100,stroke-width:2px
    style SCHED fill:#fff4e1,stroke:#e65100,stroke-width:2px
    style CONFIG fill:#fff4e1,stroke:#e65100,stroke-width:2px
    style UC fill:#e8f5e9,stroke:#1b5e20,stroke-width:2px
    style DTO fill:#e8f5e9,stroke:#1b5e20,stroke-width:2px
    style ENT fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    style VO fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    style REPO_INT fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    style EXC fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    style DDB fill:#ffebee,stroke:#b71c1c,stroke-width:2px
    style SQS fill:#ffebee,stroke:#b71c1c,stroke-width:2px
    style REDIS fill:#ffebee,stroke:#b71c1c,stroke-width:2px
```

## Detailed Data Flow

```mermaid
sequenceDiagram
    participant Client
    participant EventController
    participant CreateEventUseCase
    participant EventRepository
    participant DynamoDB
    
    participant TicketOrderController
    participant CreateTicketOrderUseCase
    participant SqsOrderPublisher
    participant SQS
    
    participant SqsOrderConsumer
    participant ProcessTicketOrderUseCase
    
    participant Scheduler
    participant ReleaseExpiredReservationsUseCase

    Note over Client,DynamoDB: Flow 1: Create Event
    Client->>EventController: POST /api/v1/events
    EventController->>CreateEventUseCase: execute(CreateEventRequest)
    CreateEventUseCase->>EventRepository: save(Event)
    EventRepository->>DynamoDB: PutItem(EventAggregates)
    DynamoDB-->>EventRepository: Success
    EventRepository-->>CreateEventUseCase: Event
    CreateEventUseCase-->>EventController: EventResponse
    EventController-->>Client: 201 Created

    Note over Client,SQS: Flow 2: Create Order (Async)
    Client->>TicketOrderController: POST /api/v1/orders
    TicketOrderController->>CreateTicketOrderUseCase: execute(CreateOrderRequest)
    CreateTicketOrderUseCase->>DynamoDB: Reserve tickets (optimistic locking)
    CreateTicketOrderUseCase->>SqsOrderPublisher: publishOrder(OrderMessage)
    SqsOrderPublisher->>SQS: SendMessage(ticket-order-queue)
    CreateTicketOrderUseCase-->>TicketOrderController: OrderResponse
    TicketOrderController-->>Client: 202 Accepted

    Note over SQS,ProcessTicketOrderUseCase: Flow 3: Process Order (Background)
    SQS->>SqsOrderConsumer: Message received
    SqsOrderConsumer->>ProcessTicketOrderUseCase: execute(orderId)
    ProcessTicketOrderUseCase->>DynamoDB: Update order status
    ProcessTicketOrderUseCase-->>SqsOrderConsumer: Success
    SqsOrderConsumer->>SQS: DeleteMessage

    Note over Scheduler,ReleaseExpiredReservationsUseCase: Flow 4: Reservation Expiration
    Scheduler->>ReleaseExpiredReservationsUseCase: @Scheduled (every 1min)
    ReleaseExpiredReservationsUseCase->>DynamoDB: Query expired reservations
    ReleaseExpiredReservationsUseCase->>DynamoDB: Release tickets
```

## Layered Architecture (Clean Architecture)

```mermaid
graph TB
    subgraph "Infrastructure Layer - External Adapters"
        direction TB
        API[API Layer<br/>━━━━━━━━━━━━━━━━<br/>EventController<br/>TicketOrderController<br/>GlobalExceptionHandler]
        REPO[Repository Implementations<br/>━━━━━━━━━━━━━━━━<br/>7 DynamoDB Repositories<br/>AWS SDK v2 Async]
        MSG[Messaging<br/>━━━━━━━━━━━━━━━━<br/>SqsOrderPublisher<br/>SqsOrderConsumer<br/>@Scheduled polling]
        SCHED[Scheduler<br/>━━━━━━━━━━━━━━━━<br/>ReservationExpirationScheduler]
        CONFIG[Configuration<br/>━━━━━━━━━━━━━━━━<br/>DynamoDBConfig<br/>SqsConfig<br/>SchedulerConfig<br/>RedisConfig<br/>JacksonConfig]
    end

    subgraph "Application Layer - Use Cases"
        direction TB
        UC_EVENT[Event Use Cases<br/>━━━━━━━━━━━━━━━━<br/>CreateEventUseCase<br/>ListEventsUseCase<br/>GetEventAvailabilityUseCase]
        UC_INVENTORY[Inventory Use Cases<br/>━━━━━━━━━━━━━━━━<br/>CreateInventoryUseCase<br/>GetInventoryUseCase]
        UC_ORDER[Order Use Cases<br/>━━━━━━━━━━━━━━━━<br/>CreateTicketOrderUseCase<br/>ProcessTicketOrderUseCase<br/>ConfirmTicketOrderUseCase<br/>MarkOrderAsSoldUseCase<br/>GetTicketOrderUseCase]
        UC_RESERVATION[Reservation Use Cases<br/>━━━━━━━━━━━━━━━━<br/>ReleaseExpiredReservationsUseCase]
        DTO[DTOs<br/>━━━━━━━━━━━━━━━━<br/>11 Request DTOs<br/>11 Response DTOs]
    end

    subgraph "Domain Layer - Business Logic"
        direction TB
        ENT[Domain Entities<br/>━━━━━━━━━━━━━━━━<br/>Event<br/>TicketOrder<br/>TicketInventory<br/>TicketReservation<br/>TicketItem<br/>CustomerInfo]
        VO[Value Objects<br/>━━━━━━━━━━━━━━━━<br/>EventId, OrderId<br/>CustomerId, Money<br/>TicketId, ReservationId]
        REPO_INT[Repository Interfaces<br/>Ports<br/>━━━━━━━━━━━━━━━━<br/>7 Repository Interfaces]
        EXC[Domain Exceptions<br/>━━━━━━━━━━━━━━━━<br/>4 Custom Exceptions]
        ENUM[Enums<br/>━━━━━━━━━━━━━━━━<br/>OrderStatus<br/>TicketStatus<br/>EventStatus]
    end

    API --> UC_EVENT
    API --> UC_INVENTORY
    API --> UC_ORDER
    SCHED --> UC_RESERVATION
    MSG --> UC_ORDER
    
    UC_EVENT --> DTO
    UC_INVENTORY --> DTO
    UC_ORDER --> DTO
    UC_RESERVATION --> DTO
    
    UC_EVENT --> ENT
    UC_INVENTORY --> ENT
    UC_ORDER --> ENT
    UC_RESERVATION --> ENT
    
    UC_EVENT --> VO
    UC_ORDER --> VO
    UC_INVENTORY --> VO
    
    UC_EVENT --> REPO_INT
    UC_INVENTORY --> REPO_INT
    UC_ORDER --> REPO_INT
    UC_RESERVATION --> REPO_INT
    
    UC_ORDER --> EXC
    
    REPO_INT -.->|implements| REPO
    REPO --> ENT
    CONFIG --> REPO
    CONFIG --> MSG

    style API fill:#fff4e1,stroke:#e65100,stroke-width:2px
    style REPO fill:#fff4e1,stroke:#e65100,stroke-width:2px
    style MSG fill:#fff4e1,stroke:#e65100,stroke-width:2px
    style SCHED fill:#fff4e1,stroke:#e65100,stroke-width:2px
    style CONFIG fill:#fff4e1,stroke:#e65100,stroke-width:2px
    style UC_EVENT fill:#e8f5e9,stroke:#1b5e20,stroke-width:2px
    style UC_INVENTORY fill:#e8f5e9,stroke:#1b5e20,stroke-width:2px
    style UC_ORDER fill:#e8f5e9,stroke:#1b5e20,stroke-width:2px
    style UC_RESERVATION fill:#e8f5e9,stroke:#1b5e20,stroke-width:2px
    style DTO fill:#e8f5e9,stroke:#1b5e20,stroke-width:2px
    style ENT fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    style VO fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    style REPO_INT fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    style EXC fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    style ENUM fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
```

## Components and Responsibilities

### Infrastructure Layer
- **API Controllers**: Receive HTTP requests, validate, call Use Cases
- **Repository Implementations**: Implement domain interfaces using DynamoDB
- **Messaging**: Publish and consume SQS messages for asynchronous processing
- **Scheduler**: Execute scheduled tasks (reservation expiration)
- **Configuration**: Configure Spring beans (DynamoDB, SQS, Redis, etc.)

### Application Layer
- **Use Cases**: Orchestrate application logic, coordinate repositories and entities
- **DTOs**: Transfer data between layers without exposing domain model

### Domain Layer
- **Entities**: Contain business logic and domain rules
- **Value Objects**: Immutable objects representing domain concepts
- **Repository Interfaces**: Contracts (ports) that define data access
- **Exceptions**: Domain-specific exceptions
- **Enums**: Domain states and types
