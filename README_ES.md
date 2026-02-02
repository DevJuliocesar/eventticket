# EventTicket - Sistema Reactivo de Procesamiento de Eventos y Entradas

Sistema de gestión de entradas y procesamiento de eventos construido con arquitectura reactiva usando Spring WebFlux, Java 25, DynamoDB, SQS y procesamiento asíncrono de mensajes.

## Tecnologías

- **Java 25**: Características modernas del lenguaje
  - **Records**: Portadores de datos inmutables para DTOs, Value Objects y entidades de Auditoría
  - **Pattern Matching**: Expresiones mejoradas de `instanceof` y `switch`
  - **Virtual Threads**: Hilos ligeros para mejorar la concurrencia (habilitado vía Spring Boot)
  - **Text Blocks & String Templates**: Cadenas multilínea limpias con `.formatted()`
  - **Sealed Classes**: Jerarquías de clases restringidas para modelado de dominio
- **Spring Boot 4.0-M1**: Framework base con soporte para Java 25 y Virtual Threads
- **Spring WebFlux**: API reactiva no bloqueante
- **LocalStack**: Emulador completo de servicios AWS
  - **DynamoDB**: Event Sourcing y persistencia de datos
  - **SQS**: Colas de mensajes asíncronas
- **AWS SDK v2 Async**: Cliente reactivo para DynamoDB y SQS
- **Redis**: Caché distribuido para alto rendimiento
- **Docker & Docker Compose**: Containerización

## Servicios Docker Compose

### 1. Aplicación Spring Boot (`app`)
- **Puerto**: 8080
- API Reactiva con Spring WebFlux
- Conectada a LocalStack (DynamoDB + SQS) y Redis

### 2. LocalStack (`localstack`)
- **Puerto Gateway**: 4566 (todos los servicios AWS)
- Emulador completo de servicios AWS
- **Servicios Habilitados**:
  - **DynamoDB**: Event Sourcing y tablas de datos
  - **SQS**: Colas de mensajes asíncronas
- Persistencia habilitada para desarrollo
- **Health Check**: http://localhost:4566/_localstack/health

### 3. Redis (`redis`)
- **Puerto**: 6379
- Caché distribuido para alto rendimiento
- Persistencia AOF (Append Only File)

## Inicio Rápido

### Iniciar Servicios

```bash
# Iniciar todos los servicios
docker-compose up -d

# Ver logs
docker-compose logs -f

# Ver logs de un servicio específico
docker-compose logs -f localstack
```

### Detener Servicios

```bash
# Detener sin eliminar volúmenes
docker-compose down

# Detener y eliminar volúmenes (empezar desde cero)
docker-compose down -v
```

### Reconstruir Aplicación

```bash
# Reconstruir la imagen de la aplicación
docker-compose build app

# Reconstruir y reiniciar
docker-compose up -d --build app
```

## Acceso a Servicios

| Servicio | URL | Credenciales |
|----------|-----|--------------|
| API REST | http://localhost:8080 | - |
| Actuator | http://localhost:8080/actuator | - |
| LocalStack Gateway | http://localhost:4566 | test/test |
| LocalStack Health | http://localhost:4566/_localstack/health | - |
| Redis | localhost:6379 | - |

## Endpoints de la API

### URL Base
```
http://localhost:8080/api/v1
```

### Endpoints de Eventos

| Método | Endpoint | Descripción | Código de Estado |
|--------|----------|-------------|------------------|
| `POST` | `/events` | Crear un nuevo evento | 201 Created |
| `GET` | `/events` | Listar todos los eventos (paginado) | 200 OK |
| `GET` | `/events/{eventId}/availability` | Obtener disponibilidad en tiempo real de un evento | 200 OK |
| `POST` | `/events/inventories` | Crear inventario de tickets para un evento | 201 Created |
| `GET` | `/events/{eventId}/inventories` | Listar todo el inventario de un evento (paginado) | 200 OK |

### Endpoints de Órdenes

| Método | Endpoint | Descripción | Código de Estado |
|--------|----------|-------------|------------------|
| `POST` | `/orders` | Crear una nueva orden de tickets | 201 Created |
| `GET` | `/orders/{orderId}` | Obtener orden por ID | 200 OK |
| `POST` | `/orders/{orderId}/confirm` | Confirmar orden con información de pago del cliente | 200 OK |
| `POST` | `/orders/{orderId}/mark-as-sold` | Marcar orden como vendida (pago completado) | 200 OK |

### Documentación de la API

Para documentación detallada de la API con ejemplos de solicitud/respuesta, ver:
- **Colección Postman**: `doc/api.json` (importar en Postman)
- **Ejemplos cURL**: `COMANDOS_CURL_PRUEBA.md`

### Ejemplos de Solicitudes

#### Crear Evento
```bash
curl -X POST http://localhost:8080/api/v1/events \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Concierto 2026",
    "description": "Concierto increíble",
    "venue": "Estadio",
    "eventDate": "2026-02-28T18:00:00Z",
    "totalCapacity": 1000
  }'
```

#### Crear Orden
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-123",
    "eventId": "event-456",
    "eventName": "Concierto 2026",
    "ticketType": "VIP",
    "quantity": 2
  }'
```

#### Confirmar Orden
```bash
curl -X POST http://localhost:8080/api/v1/orders/{orderId}/confirm \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Juan Pérez",
    "email": "juan@ejemplo.com",
    "phoneNumber": "+57 300 123 4567",
    "address": "Calle 123",
    "city": "Bogotá",
    "country": "Colombia",
    "paymentMethod": "Nequi"
  }'
```

## Estructura de Datos DynamoDB

### Tablas

#### 1. **Events** - Event Sourcing
- **Partition Key**: `aggregateId` (HASH)
- **Sort Key**: `version` (RANGE) - Asegura el orden de eventos
- **GSI**: `EventTypeIndex` (eventType + createdAt) - Consulta por tipo
- **Atributos**:
  - `aggregateId`: ID del agregado (TicketOrder, Ticket, etc.)
  - `version`: Versión del evento (1, 2, 3...)
  - `aggregateType`: Tipo de agregado
  - `eventType`: TicketOrderCreated, TicketReserved, PaymentProcessed, etc.
  - `eventData`: Datos del evento (JSON serializado)
  - `createdAt`: Timestamp ISO 8601
  - `metadata`: Metadatos (correlationId, userId, etc.)

#### 2. **TicketOrders** - Órdenes de Entradas
- **Partition Key**: `orderId` (HASH)
- **GSI**: 
  - `CustomerIndex` (customerId + createdAt)
  - `StatusIndex` (status + createdAt)
- **Atributos**:
  - `orderId`: ID único de orden
  - `customerId`: ID del cliente
  - `orderNumber`: Número de orden legible por humanos
  - `eventId`: ID del evento
  - `eventName`: Nombre del evento
  - `status`: PENDING, CONFIRMED, PROCESSING, COMPLETED, CANCELLED, FAILED
  - `tickets`: Lista de items de entradas (embebidos)
  - `totalAmount`: Monto total de la orden
  - `currency`: Moneda (COP, USD, etc.)
  - `createdAt`, `updatedAt`: Timestamps

#### 3. **TicketInventory** - Inventario de Entradas
- **Partition Key**: `eventId` (HASH)
- **Sort Key**: `ticketType` (RANGE)
- **Atributos**:
  - `eventId`: ID del evento
  - `ticketType`: VIP, GENERAL, etc.
  - `eventName`: Nombre del evento
  - `totalQuantity`: Total de entradas
  - `availableQuantity`: Entradas disponibles
  - `reservedQuantity`: Entradas reservadas
  - `price`: Precio de la entrada
  - `currency`: Moneda
  - `version`: Versión para bloqueo optimista

#### 4. **TicketReservations** - Reservas Temporales
- **Partition Key**: `reservationId` (HASH)
- **GSI**: 
  - `OrderIndex` (orderId)
  - `ExpirationIndex` (expiresAt)
- **Atributos**:
  - `reservationId`: ID único de reserva
  - `orderId`: ID de orden asociada
  - `eventId`: ID del evento
  - `ticketType`: Tipo de entrada
  - `quantity`: Número de entradas
  - `status`: ACTIVE, CONFIRMED, RELEASED, EXPIRED
  - `expiresAt`: Timestamp de expiración (Unix epoch)
  - `createdAt`: Timestamp de creación

### Colas SQS

**Colas Creadas**:
1. **ticket-order-queue**: Procesamiento de órdenes de entradas
2. **ticket-payment-queue**: Procesamiento de pagos (timeout 60s)
3. **ticket-notification-queue**: Envío de notificaciones
4. **ticket-dlq**: Dead Letter Queue para mensajes fallidos
5. **ticket-order-fifo.fifo**: Cola FIFO para procesamiento ordenado

Ver detalles en: `init-scripts/03-init-localstack.sh`

## Arquitectura

```
┌─────────────────────────────────────────────┐
│      API REST (Spring WebFlux)              │
│         Puerto 8080 (Reactivo)               │
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
                       Puerto 4566
```

### Separación de Responsabilidades

| Tecnología | Caso de Uso | Razón |
|------------|-------------|-------|
| **DynamoDB** | Event Sourcing, Órdenes, Inventario | Append-only, alto throughput de escritura, escalabilidad |
| **SQS** | Mensajería Asíncrona | Desacoplamiento, procesamiento distribuido, reintento automático |
| **Redis** | Caché | Baja latencia, datos temporales |

### Características de Arquitectura

- **Sin Bloqueo**: DynamoDB con concurrencia optimista
- **NoSQL Nativo**: DynamoDB para toda la persistencia de datos
- **Flujos Asíncronos**: SQS para orquestación de mensajes
- **Reactivo End-to-End**: AWS SDK v2 Async (DynamoDB + SQS)
- **Event Sourcing**: Eventos inmutables en DynamoDB con GSI
- **Alta Disponibilidad**: Particionado de DynamoDB + colas distribuidas SQS
- **Desarrollo Local**: LocalStack emula AWS sin costo

### Capas de Arquitectura Limpia

La aplicación sigue los principios de **Arquitectura Limpia** con clara separación de responsabilidades:

```
┌─────────────────────────────────────────────────────────┐
│              Capa de Infraestructura                     │
│  (Controladores, Repositorios, Mensajería, Config)      │
│  - EventController, TicketOrderController                 │
│  - Repositorios DynamoDB                                 │
│  - Consumidores/Publicadores SQS                         │
└──────────────────┬──────────────────────────────────────┘
                   │ depende de
┌──────────────────▼──────────────────────────────────────┐
│              Capa de Aplicación                          │
│  (Casos de Uso, DTOs)                                   │
│  - CreateEventUseCase, CreateTicketOrderUseCase         │
│  - ConfirmTicketOrderUseCase, ProcessTicketOrderUseCase │
│  - DTOs de Request/Response                             │
└──────────────────┬──────────────────────────────────────┘
                   │ depende de
┌──────────────────▼──────────────────────────────────────┐
│              Capa de Dominio                             │
│  (Entidades, Objetos de Valor, Interfaces de Repositorio)│
│  - TicketOrder, TicketInventory, Event                 │
│  - Money, OrderId, CustomerId, EventId                   │
│  - Interfaces de Repositorio (Puertos)                   │
│  - Excepciones de Dominio                                │
└─────────────────────────────────────────────────────────┘
```

**Principios Clave**:
- **Inversión de Dependencias**: El dominio tiene cero dependencias de capas externas
- **Principios SOLID**: Responsabilidad Única, Abierto/Cerrado, Liskov, Segregación de Interfaces, Inversión de Dependencias
- **Domain-Driven Design**: Modelo de dominio rico con lógica de negocio en entidades
- **Arquitectura Hexagonal**: Adaptadores (controladores, repositorios) adaptan el mundo externo al dominio

## Flujo de la Aplicación

### 1. Flujo de Creación de Eventos

```
Solicitud del Usuario
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
    ├─► Valida la solicitud
    ├─► Crea entidad Event del dominio
    ├─► Guarda en DynamoDB (EventRepository)
    └─► Retorna EventResponse
```

### 2. Flujo de Creación de Órdenes (Procesamiento Asíncrono)

```
Solicitud del Usuario
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
    ├─► Valida disponibilidad de inventario
    ├─► Reserva tickets (bloqueo optimista)
    ├─► Crea TicketOrder (estado: RESERVED)
    ├─► Crea TicketReservation (timeout: 10 min)
    ├─► Guarda tickets en tabla TicketItems
    ├─► Publica mensaje a SQS (ticket-order-queue)
    └─► Retorna OrderResponse inmediatamente
    
    [Procesamiento Asíncrono - En Segundo Plano]
    │
    ▼
SqsOrderConsumer.pollAndProcessMessages()
    │ (se ejecuta cada 5 segundos)
    ▼
ProcessTicketOrderUseCase.execute()
    │
    ├─► Valida disponibilidad en tiempo real
    ├─► Actualiza inventario (bloqueo optimista)
    ├─► Cambia estado de orden: RESERVED → PENDING_CONFIRMATION
    └─► Actualiza orden en DynamoDB
```

### 3. Flujo de Confirmación de Orden

```
Solicitud del Usuario
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
    ├─► Carga orden del repositorio
    ├─► Valida estado de orden (debe ser RESERVED o PENDING_CONFIRMATION)
    ├─► Guarda información del cliente (CustomerInfoRepository)
    ├─► Actualiza estado de orden a PENDING_CONFIRMATION
    ├─► Actualiza estados de tickets a PENDING_CONFIRMATION
    └─► Retorna OrderResponse actualizado
```

### 4. Flujo de Marcar Orden como Vendida

```
Solicitud del Usuario
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
    ├─► Carga orden (debe estar en PENDING_CONFIRMATION)
    ├─► Asigna números de asiento únicos a tickets
    ├─► Actualiza estado de orden a SOLD
    ├─► Actualiza estados de tickets a SOLD (estado final)
    ├─► Actualiza inventario (decrementa disponible, incrementa vendido)
    ├─► Libera reserva
    └─► Retorna OrderResponse final
```

### 5. Flujo de Expiración de Reservas (Programado)

```
Programador (cada 1 minuto)
    │
    ▼
ReleaseExpiredReservationsUseCase.execute()
    │
    ├─► Consulta reservas donde expiresAt < ahora()
    ├─► Para cada reserva expirada:
    │   ├─► Devuelve tickets al inventario (bloqueo optimista)
    │   ├─► Actualiza estado de reserva a EXPIRED
    │   ├─► Actualiza estado de orden a CANCELLED (si aplica)
    │   └─► Registra acción de liberación
    └─► Retorna conteo de reservas liberadas
```

### 6. Flujo de Consulta de Disponibilidad (Reactivo)

```
Solicitud del Usuario
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
    ├─► Carga evento de DynamoDB
    ├─► Consulta inventario para todos los tipos de tickets
    ├─► Calcula disponibilidad en tiempo real
    │   (totalQuantity - reservedQuantity - soldQuantity)
    └─► Retorna EventAvailabilityResponse
```

### 7. Máquina de Estados de Tickets

```
AVAILABLE (Inicial)
    │
    ├─► RESERVED (Usuario inicia compra)
    │       │
    │       ├─► PENDING_CONFIRMATION (Procesando pago)
    │       │       │
    │       │       ├─► SOLD (Pago confirmado) [FINAL]
    │       │       └─► AVAILABLE (Pago fallido)
    │       │
    │       └─► AVAILABLE (Reserva expirada)
    │
    └─► COMPLIMENTARY (Asignado por admin) [FINAL]
```

**Reglas de Estado**:
- **SOLD** y **COMPLIMENTARY** son estados finales (irreversibles)
- Solo los tickets **SOLD** cuentan como ingresos
- Todas las transiciones de estado son atómicas y auditables
- Ver `TICKET_STATUS_FLOW.md` para reglas detalladas

## Pruebas

### Estructura de Pruebas

El proyecto incluye pruebas completas organizadas por capa:

```
src/test/java/com/eventticket/
├── application/
│   ├── dto/                          # Pruebas de DTOs
│   │   ├── PagedEventResponseTest.java
│   │   └── PagedInventoryResponseTest.java
│   └── usecase/                      # Pruebas de casos de uso
│       ├── ConcurrencyTest.java      # Creación concurrente de órdenes
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
    ├── api/                          # Pruebas de controladores
    │   ├── EventControllerTest.java
    │   └── GlobalExceptionHandlerTest.java
    └── repository/                   # Pruebas de integración de repositorios
        ├── DynamoDBEventRepositoryTest.java
        ├── DynamoDBCustomerInfoRepositoryTest.java
        ├── DynamoDBTicketInventoryRepositoryTest.java
        ├── DynamoDBTicketItemRepositoryTest.java
        ├── DynamoDBTicketOrderRepositoryTest.java
        ├── DynamoDBTicketReservationRepositoryTest.java
        └── DynamoDBTicketStateTransitionAuditRepositoryTest.java
```

### Ejecutar Pruebas

```bash
# Ejecutar todas las pruebas
mvn test

# Ejecutar clase de prueba específica
mvn test -Dtest=CreateTicketOrderUseCaseTest

# Ejecutar pruebas con reporte de cobertura
mvn clean test jacoco:report

# Ver reporte de cobertura
# Abrir: target/site/jacoco/index.html
```

### Cobertura de Pruebas

Estado actual de cobertura (ver `COVERAGE_REPORT.md` para detalles):

- **Cobertura General**: ~11% (objetivo: 90%)
- **Mejor Cobertura**:
  - `application.dto`: 57%
  - `infrastructure.api`: 64%
  - `domain.exception`: 46%
- **Necesita Mejora**:
  - `infrastructure.repository`: 0% (crítico)
  - `domain.model`: 9%
  - `application.usecase`: 13%
  - `infrastructure.messaging`: 0%

### Tipos de Pruebas

#### 1. Pruebas Unitarias
- **Ubicación**: `application/usecase/`, `application/dto/`
- **Propósito**: Probar lógica de negocio en aislamiento
- **Herramientas**: JUnit 5, Mockito, Reactor Test
- **Ejemplo**: `CreateTicketOrderUseCaseTest` - Prueba creación de órdenes con repositorios simulados

#### 2. Pruebas de Integración
- **Ubicación**: `infrastructure/repository/`
- **Propósito**: Probar implementaciones de repositorios con DynamoDB real (LocalStack)
- **Herramientas**: JUnit 5, Testcontainers (LocalStack), AWS SDK v2
- **Ejemplo**: `DynamoDBTicketOrderRepositoryTest` - Prueba operaciones CRUD en DynamoDB

#### 3. Pruebas de API
- **Ubicación**: `infrastructure/api/`
- **Propósito**: Probar endpoints REST de extremo a extremo
- **Herramientas**: WebTestClient, Mockito
- **Ejemplo**: `EventControllerTest` - Prueba solicitudes/respuestas HTTP

#### 4. Pruebas de Concurrencia
- **Ubicación**: `application/usecase/ConcurrencyTest.java`
- **Propósito**: Probar creación concurrente de órdenes y actualizaciones de inventario
- **Herramientas**: JUnit 5, Reactor Test, Virtual Threads
- **Escenario**: Múltiples usuarios intentando comprar los mismos tickets simultáneamente

### Configuración de Pruebas

Las pruebas usan **LocalStack** para emulación de servicios AWS:
- Tablas DynamoDB creadas automáticamente
- Colas SQS inicializadas
- No se necesita cuenta AWS real
- Ejecución de pruebas rápida y aislada

### Ver Resultados de Pruebas

```bash
# Ubicación de reportes de pruebas
target/surefire-reports/

# Ubicación de reporte de cobertura
target/site/jacoco/index.html

# Ver en navegador
python3 -m http.server 8000 --directory target/site/jacoco
# Abrir: http://localhost:8000
```

## Comandos AWS CLI con LocalStack

### Configurar alias awslocal (opcional pero recomendado)

```bash
alias awslocal="aws --endpoint-url=http://localhost:4566 --region=us-east-1"
```

### Comandos DynamoDB

```bash
# Listar tablas
awslocal dynamodb list-tables

# Escanear eventos
awslocal dynamodb scan --table-name Events --max-items 10

# Escanear órdenes de entradas
awslocal dynamodb scan --table-name TicketOrders

# Escanear inventario
awslocal dynamodb scan --table-name TicketInventory

# Consultar por cliente
awslocal dynamodb query \
  --table-name TicketOrders \
  --index-name CustomerIndex \
  --key-condition-expression "customerId = :cid" \
  --expression-attribute-values '{":cid": {"S": "customer-001"}}'

# Obtener orden específica
awslocal dynamodb get-item \
  --table-name TicketOrders \
  --key '{"orderId": {"S": "order-001"}}'
```

### Comandos SQS

```bash
# Listar colas
awslocal sqs list-queues

# Obtener URL de cola
awslocal sqs get-queue-url --queue-name ticket-order-queue

# Recibir mensajes
QUEUE_URL=$(awslocal sqs get-queue-url --queue-name ticket-order-queue --output text --query 'QueueUrl')
awslocal sqs receive-message --queue-url $QUEUE_URL --max-number-of-messages 10

# Enviar mensaje
awslocal sqs send-message \
  --queue-url $QUEUE_URL \
  --message-body '{"eventId":"concert-001","ticketCount":2}'

# Obtener atributos de cola
awslocal sqs get-queue-attributes --queue-url $QUEUE_URL --attribute-names All

# Purgar cola (eliminar todos los mensajes)
awslocal sqs purge-queue --queue-url $QUEUE_URL
```

## Comandos de Verificación

### Verificar Servicios

```bash
# Health Check de LocalStack
curl http://localhost:4566/_localstack/health | jq

# Redis
docker-compose exec redis redis-cli ping

# Health de la Aplicación
curl http://localhost:8080/actuator/health
```

### Ver Logs de Servicios

```bash
# Todos los servicios
docker-compose logs -f

# Servicio específico
docker-compose logs -f localstack
docker-compose logs -f app
docker-compose logs -f redis
```

## Solución de Problemas

### Los contenedores no inician

```bash
# Verificar estado de servicios
docker-compose ps

# Ver logs de errores
docker-compose logs
```

### LocalStack no responde

```bash
# Ver logs de LocalStack
docker-compose logs -f localstack

# Reiniciar LocalStack
docker-compose restart localstack

# Verificar servicios habilitados
curl http://localhost:4566/_localstack/health | jq

# Re-ejecutar script de inicialización
docker-compose exec localstack sh /etc/localstack/init/ready.d/03-init-localstack.sh
```

### Limpiar todo y empezar desde cero

```bash
docker-compose down -v
docker system prune -a
docker-compose up -d --build
```

## Próximos Pasos

1. Crear estructura del proyecto Spring Boot
2. Implementar controladores reactivos con WebFlux
3. Configurar repositorios DynamoDB con AWS SDK v2 Async
4. Implementar Event Store con DynamoDB
5. Configurar listeners SQS para procesamiento asíncrono de mensajes
6. Implementar bloqueo optimista para inventario de entradas
7. Configurar Redis Reactivo para caché
8. Agregar tests con WebTestClient y Testcontainers (LocalStack)

## Por Qué Esta Arquitectura con LocalStack

### LocalStack - Emulador AWS

- **Desarrollo Local**: No se necesita cuenta AWS ni costos
- **Múltiples Servicios**: DynamoDB + SQS (y 90+ servicios más)
- **Persistencia**: Los datos persisten entre reinicios
- **API Compatible**: 100% compatible con AWS SDK
- **Testing**: Perfecto para tests de integración
- **Listo para CI/CD**: Fácil integración en pipelines

### SQS vs RabbitMQ

| Característica | SQS (LocalStack) | RabbitMQ |
|----------------|------------------|----------|
| Simplicidad | Muy simple | Más complejo |
| Escalabilidad | Ilimitada (AWS) | Requiere configuración |
| Dead Letter Queue | Nativo | Configurable |
| FIFO | Soporte nativo | Colas duraderas |
| Reintento Automático | Integrado | Manual |
| Visibility Timeout | Nativo | Manual |
| Colas con Delay | Integrado | Plugins |
| Listo para Cloud | AWS directo | Requiere hosting |
| Desarrollo Local | LocalStack | Docker |

### DynamoDB para Todo

- **Event Sourcing**: Diseño perfecto append-only
- **Partition Key + Sort Key**: `aggregateId` + `version` asegura el orden
- **Sin Conflictos**: Escrituras concurrentes a diferentes particiones
- **GSI**: Índices secundarios globales para consultas complejas
- **Escalabilidad**: Auto-escalado sin límites prácticos
- **Single-Table Design**: Patrón opcional para alto rendimiento
- **Bloqueo Optimista**: Campo version para inventario
- **TTL**: Expiración automática para reservas (puede habilitarse)

### Beneficios del Stack de Producción

| Componente | Desarrollo | Producción |
|------------|------------|------------|
| **DynamoDB** | LocalStack | AWS DynamoDB |
| **SQS** | LocalStack | AWS SQS |
| **Redis** | Docker | AWS ElastiCache |
| **LocalStack** | Dev/Test | AWS Real |

**¡No se necesitan cambios de código al pasar a producción!**

## Descripción del Proyecto

**EventTicket** es un sistema de procesamiento de eventos y entradas que demuestra:

- **Operaciones Concurrentes Sin Bloqueo**: Usando DynamoDB con concurrencia optimista
- **Persistencia NoSQL**: DynamoDB para acceso rápido a datos
- **Flujos Asíncronos**: Orquestación vía SQS entre componentes
- **Arquitectura Reactiva**: Spring WebFlux con programación no bloqueante end-to-end
- **Event Sourcing**: Todos los eventos de entradas almacenados inmutablemente en DynamoDB
- **Java 25**: Aprovechando Records, Pattern Matching y Virtual Threads

## Variables de Entorno

Crear un archivo `.env` (opcional):

```bash
# AWS LocalStack
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=test
AWS_SECRET_ACCESS_KEY=test
AWS_ENDPOINT_URL=http://localhost:4566

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Aplicación
APP_PORT=8080
SPRING_PROFILES_ACTIVE=docker
```

## Nota de Seguridad

Esta configuración es **solo para desarrollo**. Para producción:
- Usar credenciales AWS reales con roles IAM
- Habilitar encriptación en reposo de DynamoDB
- Usar endpoints VPC para networking privado
- Habilitar encriptación del lado del servidor en SQS
- Usar AUTH y TLS en Redis
- Implementar autenticación/autorización apropiada

## Licencia

Proyecto de desarrollo para EventTicket.
