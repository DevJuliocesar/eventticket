# Diagrama de Secuencia - Sistema de Tickets de Eventos

## Flujo Principal: Creación de Orden con Procesamiento Asíncrono

Este diagrama muestra el flujo completo de creación de una orden de tickets, incluyendo el procesamiento asíncrono mediante SQS.

```mermaid
sequenceDiagram
    participant Cliente
    participant TicketOrderController
    participant CreateTicketOrderUseCase
    participant TicketInventoryRepository
    participant TicketOrderRepository
    participant TicketReservationRepository
    participant TicketItemRepository
    participant SqsOrderPublisher
    participant SQS
    participant SqsOrderConsumer
    participant ProcessTicketOrderUseCase

    Note over Cliente,SQS: Fase 1: Creación de Orden (Síncrono)
    
    Cliente->>TicketOrderController: POST /api/v1/orders<br/>(CreateOrderRequest)
    activate TicketOrderController
    
    TicketOrderController->>CreateTicketOrderUseCase: execute(request)
    activate CreateTicketOrderUseCase
    
    CreateTicketOrderUseCase->>TicketInventoryRepository: findByEventIdAndTicketType(eventId, ticketType)
    activate TicketInventoryRepository
    TicketInventoryRepository-->>CreateTicketOrderUseCase: TicketInventory
    deactivate TicketInventoryRepository
    
    Note over CreateTicketOrderUseCase: Valida disponibilidad<br/>(isAvailable(quantity))
    
    CreateTicketOrderUseCase->>TicketInventoryRepository: updateWithOptimisticLock(reservedInventory)
    activate TicketInventoryRepository
    Note over TicketInventoryRepository: Reserva tickets<br/>(incrementa reservedQuantity)
    TicketInventoryRepository-->>CreateTicketOrderUseCase: TicketInventory actualizado
    deactivate TicketInventoryRepository
    
    CreateTicketOrderUseCase->>TicketOrderRepository: save(TicketOrder)
    activate TicketOrderRepository
    Note over TicketOrderRepository: Crea orden con status: RESERVED
    TicketOrderRepository-->>CreateTicketOrderUseCase: TicketOrder guardado
    deactivate TicketOrderRepository
    
    CreateTicketOrderUseCase->>TicketReservationRepository: save(TicketReservation)
    activate TicketReservationRepository
    Note over TicketReservationRepository: Crea reserva con timeout<br/>(10 minutos)
    TicketReservationRepository-->>CreateTicketOrderUseCase: TicketReservation guardada
    deactivate TicketReservationRepository
    
    CreateTicketOrderUseCase->>TicketItemRepository: saveAll(ticketsWithIds)
    activate TicketItemRepository
    Note over TicketItemRepository: Guarda tickets con<br/>orderId y reservationId
    TicketItemRepository-->>CreateTicketOrderUseCase: Tickets guardados
    deactivate TicketItemRepository
    
    CreateTicketOrderUseCase->>SqsOrderPublisher: publishOrder(OrderMessage)
    activate SqsOrderPublisher
    SqsOrderPublisher->>SQS: sendMessage(orderMessage)
    activate SQS
    SQS-->>SqsOrderPublisher: Mensaje encolado
    deactivate SQS
    SqsOrderPublisher-->>CreateTicketOrderUseCase: Mensaje publicado
    deactivate SqsOrderPublisher
    
    CreateTicketOrderUseCase->>TicketItemRepository: findByOrderId(orderId)
    activate TicketItemRepository
    TicketItemRepository-->>CreateTicketOrderUseCase: List<TicketItem>
    deactivate TicketItemRepository
    
    CreateTicketOrderUseCase-->>TicketOrderController: OrderResponse
    deactivate CreateTicketOrderUseCase
    
    TicketOrderController-->>Cliente: 201 Created<br/>(OrderResponse con orderId)
    deactivate TicketOrderController
    
    Note over Cliente,SQS: Respuesta inmediata al cliente ✅
    
    Note over SQS,ProcessTicketOrderUseCase: Fase 2: Procesamiento Asíncrono (Background)
    
    SqsOrderConsumer->>SQS: pollAndProcessMessages()<br/>(cada 5 segundos)
    activate SqsOrderConsumer
    activate SQS
    
    SQS-->>SqsOrderConsumer: receiveMessage()<br/>(OrderMessage)
    deactivate SQS
    
    SqsOrderConsumer->>ProcessTicketOrderUseCase: execute(orderId)
    activate ProcessTicketOrderUseCase
    
    ProcessTicketOrderUseCase->>TicketOrderRepository: findById(orderId)
    activate TicketOrderRepository
    TicketOrderRepository-->>ProcessTicketOrderUseCase: TicketOrder
    deactivate TicketOrderRepository
    
    Note over ProcessTicketOrderUseCase: Valida que order.status == AVAILABLE
    
    ProcessTicketOrderUseCase->>TicketItemRepository: findByOrderId(orderId)
    activate TicketItemRepository
    TicketItemRepository-->>ProcessTicketOrderUseCase: List<TicketItem>
    deactivate TicketItemRepository
    
    Note over ProcessTicketOrderUseCase: Actualiza tickets a RESERVED
    
    ProcessTicketOrderUseCase->>TicketItemRepository: saveAll(reservedTickets)
    activate TicketItemRepository
    TicketItemRepository-->>ProcessTicketOrderUseCase: Tickets actualizados
    deactivate TicketItemRepository
    
    ProcessTicketOrderUseCase->>TicketOrderRepository: save(updatedOrder)
    activate TicketOrderRepository
    Note over TicketOrderRepository: Cambia status:<br/>AVAILABLE → RESERVED
    TicketOrderRepository-->>ProcessTicketOrderUseCase: TicketOrder actualizado
    deactivate TicketOrderRepository
    
    ProcessTicketOrderUseCase-->>SqsOrderConsumer: TicketOrder procesado
    deactivate ProcessTicketOrderUseCase
    
    SqsOrderConsumer->>SQS: deleteMessage(receiptHandle)
    activate SQS
    SQS-->>SqsOrderConsumer: Mensaje eliminado
    deactivate SQS
    
    deactivate SqsOrderConsumer
```

## Flujo Alternativo: Confirmación de Orden

```mermaid
sequenceDiagram
    participant Cliente
    participant TicketOrderController
    participant ConfirmTicketOrderUseCase
    participant TicketOrderRepository
    participant CustomerInfoRepository
    participant TicketItemRepository

    Cliente->>TicketOrderController: POST /api/v1/orders/{orderId}/confirm<br/>(ConfirmOrderRequest)
    activate TicketOrderController
    
    TicketOrderController->>ConfirmTicketOrderUseCase: execute(orderId, request)
    activate ConfirmTicketOrderUseCase
    
    ConfirmTicketOrderUseCase->>TicketOrderRepository: findById(orderId)
    activate TicketOrderRepository
    TicketOrderRepository-->>ConfirmTicketOrderUseCase: TicketOrder
    deactivate TicketOrderRepository
    
    Note over ConfirmTicketOrderUseCase: Valida que status sea<br/>RESERVED o PENDING_CONFIRMATION
    
    ConfirmTicketOrderUseCase->>CustomerInfoRepository: save(customerInfo)
    activate CustomerInfoRepository
    CustomerInfoRepository-->>ConfirmTicketOrderUseCase: CustomerInfo guardado
    deactivate CustomerInfoRepository
    
    ConfirmTicketOrderUseCase->>TicketItemRepository: findByOrderId(orderId)
    activate TicketItemRepository
    TicketItemRepository-->>ConfirmTicketOrderUseCase: List<TicketItem>
    deactivate TicketItemRepository
    
    Note over ConfirmTicketOrderUseCase: Actualiza tickets a<br/>PENDING_CONFIRMATION
    
    ConfirmTicketOrderUseCase->>TicketItemRepository: saveAll(updatedTickets)
    activate TicketItemRepository
    TicketItemRepository-->>ConfirmTicketOrderUseCase: Tickets actualizados
    deactivate TicketItemRepository
    
    ConfirmTicketOrderUseCase->>TicketOrderRepository: save(updatedOrder)
    activate TicketOrderRepository
    Note over TicketOrderRepository: Cambia status a<br/>PENDING_CONFIRMATION
    TicketOrderRepository-->>ConfirmTicketOrderUseCase: TicketOrder actualizado
    deactivate TicketOrderRepository
    
    ConfirmTicketOrderUseCase-->>TicketOrderController: OrderResponse
    deactivate ConfirmTicketOrderUseCase
    
    TicketOrderController-->>Cliente: 200 OK<br/>(OrderResponse actualizado)
    deactivate TicketOrderController
```

## Flujo Alternativo: Marcar Orden como Vendida

```mermaid
sequenceDiagram
    participant Cliente
    participant TicketOrderController
    participant MarkOrderAsSoldUseCase
    participant TicketOrderRepository
    participant TicketItemRepository
    participant TicketInventoryRepository
    participant TicketReservationRepository

    Cliente->>TicketOrderController: POST /api/v1/orders/{orderId}/mark-as-sold
    activate TicketOrderController
    
    TicketOrderController->>MarkOrderAsSoldUseCase: execute(orderId)
    activate MarkOrderAsSoldUseCase
    
    MarkOrderAsSoldUseCase->>TicketOrderRepository: findById(orderId)
    activate TicketOrderRepository
    TicketOrderRepository-->>MarkOrderAsSoldUseCase: TicketOrder
    deactivate TicketOrderRepository
    
    Note over MarkOrderAsSoldUseCase: Valida que status sea<br/>PENDING_CONFIRMATION
    
    MarkOrderAsSoldUseCase->>TicketItemRepository: findByOrderId(orderId)
    activate TicketItemRepository
    TicketItemRepository-->>MarkOrderAsSoldUseCase: List<TicketItem>
    deactivate TicketItemRepository
    
    Note over MarkOrderAsSoldUseCase: Asigna números de asiento<br/>únicos a cada ticket
    
    MarkOrderAsSoldUseCase->>TicketItemRepository: saveAll(ticketsWithSeats)
    activate TicketItemRepository
    Note over TicketItemRepository: Actualiza tickets a SOLD<br/>(estado final)
    TicketItemRepository-->>MarkOrderAsSoldUseCase: Tickets actualizados
    deactivate TicketItemRepository
    
    MarkOrderAsSoldUseCase->>TicketInventoryRepository: updateInventory(order)
    activate TicketInventoryRepository
    Note over TicketInventoryRepository: Decrementa availableQuantity<br/>Incrementa soldQuantity
    TicketInventoryRepository-->>MarkOrderAsSoldUseCase: Inventory actualizado
    deactivate TicketInventoryRepository
    
    MarkOrderAsSoldUseCase->>TicketReservationRepository: releaseReservation(orderId)
    activate TicketReservationRepository
    Note over TicketReservationRepository: Libera la reserva
    TicketReservationRepository-->>MarkOrderAsSoldUseCase: Reserva liberada
    deactivate TicketReservationRepository
    
    MarkOrderAsSoldUseCase->>TicketOrderRepository: save(updatedOrder)
    activate TicketOrderRepository
    Note over TicketOrderRepository: Cambia status a SOLD<br/>(estado final)
    TicketOrderRepository-->>MarkOrderAsSoldUseCase: TicketOrder actualizado
    deactivate TicketOrderRepository
    
    MarkOrderAsSoldUseCase-->>TicketOrderController: OrderResponse
    deactivate MarkOrderAsSoldUseCase
    
    TicketOrderController-->>Cliente: 200 OK<br/>(OrderResponse final)
    deactivate TicketOrderController
```

## Flujo Alternativo: Expiración de Reservas (Scheduled)

```mermaid
sequenceDiagram
    participant Scheduler
    participant ReleaseExpiredReservationsUseCase
    participant TicketReservationRepository
    participant TicketInventoryRepository
    participant TicketOrderRepository
    participant TicketItemRepository

    Note over Scheduler: Ejecuta cada 1 minuto
    
    Scheduler->>ReleaseExpiredReservationsUseCase: execute()
    activate ReleaseExpiredReservationsUseCase
    
    ReleaseExpiredReservationsUseCase->>TicketReservationRepository: findExpiredReservations()
    activate TicketReservationRepository
    TicketReservationRepository-->>ReleaseExpiredReservationsUseCase: List<TicketReservation>
    deactivate TicketReservationRepository
    
    loop Para cada reserva expirada
        ReleaseExpiredReservationsUseCase->>TicketInventoryRepository: returnTicketsToInventory(reservation)
        activate TicketInventoryRepository
        Note over TicketInventoryRepository: Decrementa reservedQuantity<br/>Incrementa availableQuantity<br/>(optimistic locking)
        TicketInventoryRepository-->>ReleaseExpiredReservationsUseCase: Inventory actualizado
        deactivate TicketInventoryRepository
        
        ReleaseExpiredReservationsUseCase->>TicketReservationRepository: updateStatus(reservation, EXPIRED)
        activate TicketReservationRepository
        TicketReservationRepository-->>ReleaseExpiredReservationsUseCase: Reserva actualizada
        deactivate TicketReservationRepository
        
        ReleaseExpiredReservationsUseCase->>TicketItemRepository: findByOrderId(orderId)
        activate TicketItemRepository
        TicketItemRepository-->>ReleaseExpiredReservationsUseCase: List<TicketItem>
        deactivate TicketItemRepository
        
        Note over ReleaseExpiredReservationsUseCase: Actualiza tickets a AVAILABLE
        
        ReleaseExpiredReservationsUseCase->>TicketItemRepository: saveAll(availableTickets)
        activate TicketItemRepository
        TicketItemRepository-->>ReleaseExpiredReservationsUseCase: Tickets actualizados
        deactivate TicketItemRepository
        
        ReleaseExpiredReservationsUseCase->>TicketOrderRepository: updateStatus(orderId, CANCELLED)
        activate TicketOrderRepository
        TicketOrderRepository-->>ReleaseExpiredReservationsUseCase: Orden actualizada
        deactivate TicketOrderRepository
    end
    
    ReleaseExpiredReservationsUseCase-->>Scheduler: count (reservas liberadas)
    deactivate ReleaseExpiredReservationsUseCase
```

## Notas Importantes

### Estados de la Orden
- **AVAILABLE**: Estado inicial (antes de procesar)
- **RESERVED**: Orden creada, tickets reservados
- **PENDING_CONFIRMATION**: Orden confirmada con información de pago
- **SOLD**: Orden vendida (estado final)
- **CANCELLED**: Orden cancelada (reserva expirada)

### Estados de los Tickets
- **AVAILABLE**: Disponible para venta
- **RESERVED**: Reservado temporalmente
- **PENDING_CONFIRMATION**: Esperando confirmación de pago
- **SOLD**: Vendido (estado final)
- **COMPLIMENTARY**: Cortesía (estado final)

### Características Clave
1. **Respuesta Inmediata**: El cliente recibe el `orderId` inmediatamente después de crear la orden
2. **Procesamiento Asíncrono**: El procesamiento real ocurre en segundo plano mediante SQS
3. **Optimistic Locking**: Se usa para prevenir condiciones de carrera en el inventario
4. **Reservas Temporales**: Las reservas expiran después de 10 minutos
5. **Event Sourcing**: Todos los eventos se almacenan en DynamoDB para auditoría
