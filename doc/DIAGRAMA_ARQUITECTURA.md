# Diagrama de Arquitectura - EventTicket System

## Arquitectura General del Sistema (Simplificada)

```mermaid
graph TB
    CLIENT[Cliente HTTP<br/>REST API]
    
    subgraph "Spring Boot Application - Port 8080"
        subgraph "Infrastructure Layer"
            API[Controllers<br/>EventController<br/>TicketOrderController]
            REPO[DynamoDB Repositories<br/>7 Repositories]
            MSG[Messaging<br/>SqsOrderPublisher<br/>SqsOrderConsumer]
            SCHED[Scheduler<br/>ReservationExpirationScheduler]
        end
        
        subgraph "Application Layer"
            UC[Use Cases<br/>11 Use Cases]
            DTO[DTOs<br/>Request/Response]
        end
        
        subgraph "Domain Layer"
            DOMAIN[Domain Entities<br/>Event, TicketOrder<br/>TicketInventory, etc.]
        end
    end

    subgraph "External Services"
        DDB[(DynamoDB<br/>LocalStack:4566)]
        SQS[SQS Queues<br/>LocalStack:4566]
        REDIS[(Redis Cache<br/>Port 6379)]
    end

    CLIENT --> API
    API --> UC
    UC --> DOMAIN
    UC --> REPO
    UC --> MSG
    SCHED --> UC
    REPO --> DDB
    MSG --> SQS
    SQS --> MSG
    UC -.-> REDIS
    
    style CLIENT fill:#e1f5ff
    style API fill:#fff4e1
    style UC fill:#e8f5e9
    style DOMAIN fill:#f3e5f5
    style DDB fill:#ffebee
    style SQS fill:#ffebee
    style REDIS fill:#ffebee
```

## Arquitectura por Capas (Clean Architecture)

```mermaid
graph TB
    subgraph "Infrastructure Layer"
        direction TB
        API[API Layer<br/>Controllers<br/>- EventController<br/>- TicketOrderController<br/>- GlobalExceptionHandler]
        REPO[Repository Implementations<br/>- DynamoDBEventRepository<br/>- DynamoDBTicketOrderRepository<br/>- DynamoDBTicketInventoryRepository<br/>- DynamoDBTicketReservationRepository<br/>- DynamoDBTicketItemRepository<br/>- DynamoDBCustomerInfoRepository<br/>- DynamoDBTicketStateTransitionAuditRepository]
        MSG[Messaging<br/>- SqsOrderPublisher<br/>- SqsOrderConsumer]
        SCHED[Scheduler<br/>- ReservationExpirationScheduler]
        CONFIG[Configuration<br/>- DynamoDBConfig<br/>- SqsConfig<br/>- SchedulerConfig<br/>- JacksonConfig]
    end

    subgraph "Application Layer"
        direction TB
        UC[Use Cases<br/>- CreateEventUseCase<br/>- ListEventsUseCase<br/>- GetEventAvailabilityUseCase<br/>- CreateInventoryUseCase<br/>- GetInventoryUseCase<br/>- CreateTicketOrderUseCase<br/>- ProcessTicketOrderUseCase<br/>- ConfirmTicketOrderUseCase<br/>- MarkOrderAsSoldUseCase<br/>- GetTicketOrderUseCase<br/>- ReleaseExpiredReservationsUseCase]
        DTO[DTOs<br/>Request DTOs<br/>Response DTOs]
    end

    subgraph "Domain Layer"
        direction TB
        ENT[Domain Entities<br/>- Event<br/>- TicketOrder<br/>- TicketInventory<br/>- TicketReservation<br/>- TicketItem<br/>- CustomerInfo]
        VO[Value Objects<br/>- EventId<br/>- OrderId<br/>- CustomerId<br/>- Money<br/>- TicketId<br/>- ReservationId]
        REPO_INT[Repository Interfaces<br/>Ports]
        EXC[Domain Exceptions<br/>- InsufficientInventoryException<br/>- OrderNotFoundException<br/>- InvalidTicketStateTransitionException<br/>- DomainException]
    end

    API --> UC
    UC --> DTO
    UC --> ENT
    UC --> VO
    UC --> REPO_INT
    UC --> EXC
    REPO_INT -.-> REPO
    MSG --> UC
    SCHED --> UC
    REPO --> ENT
    CONFIG --> REPO
    CONFIG --> MSG

    style API fill:#fff4e1
    style REPO fill:#fff4e1
    style MSG fill:#fff4e1
    style SCHED fill:#fff4e1
    style CONFIG fill:#fff4e1
    style UC fill:#e8f5e9
    style DTO fill:#e8f5e9
    style ENT fill:#f3e5f5
    style VO fill:#f3e5f5
    style REPO_INT fill:#f3e5f5
    style EXC fill:#f3e5f5
```

## Componentes y Tecnologías

### Stack Tecnológico

| Capa | Tecnología | Propósito |
|------|-----------|-----------|
| **Framework** | Spring Boot 4.0-M1 | Framework base con soporte Java 25 |
| **API** | Spring WebFlux | API reactiva no bloqueante |
| **Lenguaje** | Java 25 | Records, Pattern Matching, Virtual Threads |
| **Base de Datos** | DynamoDB (LocalStack) | Event Sourcing y persistencia |
| **Mensajería** | SQS (LocalStack) | Procesamiento asíncrono |
| **Cache** | Redis | Caché distribuido |
| **SDK AWS** | AWS SDK v2 Async | Cliente reactivo para DynamoDB y SQS |

### Principios Arquitectónicos

1. **Clean Architecture**: Separación clara en 3 capas (Infrastructure, Application, Domain)
2. **Hexagonal Architecture**: Adaptadores (Controllers, Repositories) adaptan el mundo externo
3. **Domain-Driven Design**: Modelo de dominio rico con lógica de negocio en entidades
4. **Event Sourcing**: Eventos inmutables almacenados en DynamoDB
5. **CQRS Pattern**: Separación entre comandos (write) y consultas (read)
6. **Reactive Programming**: End-to-end reactivo con Spring WebFlux y AWS SDK Async
7. **Optimistic Locking**: Control de concurrencia sin bloqueos

### Patrones de Diseño Utilizados

- **Repository Pattern**: Abstracción de acceso a datos
- **Use Case Pattern**: Lógica de aplicación encapsulada
- **Adapter Pattern**: Controllers y Repositories como adaptadores
- **Value Object Pattern**: Objetos inmutables para valores del dominio
- **Factory Pattern**: Creación de entidades del dominio
- **Strategy Pattern**: Diferentes estrategias de procesamiento
- **Observer Pattern**: Eventos y notificaciones

## Escalabilidad y Rendimiento

- **Horizontal Scaling**: DynamoDB y SQS escalan automáticamente
- **Non-Blocking I/O**: Spring WebFlux con operaciones reactivas
- **Virtual Threads**: Java 25 para mejor concurrencia
- **Caching**: Redis para consultas frecuentes
- **Optimistic Locking**: Evita bloqueos en operaciones concurrentes
- **Async Processing**: SQS para desacoplar procesamiento pesado

## Seguridad y Observabilidad

- **Exception Handling**: GlobalExceptionHandler centralizado
- **Audit Trail**: TicketStateTransitionAudit para trazabilidad
- **Event Sourcing**: Historial completo de eventos
- **Health Checks**: Spring Actuator endpoints
- **Logging**: SLF4J con niveles apropiados
