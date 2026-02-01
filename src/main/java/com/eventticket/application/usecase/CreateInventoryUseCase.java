package com.eventticket.application.usecase;

import com.eventticket.application.dto.CreateInventoryRequest;
import com.eventticket.domain.model.TicketInventory;
import com.eventticket.domain.repository.EventRepository;
import com.eventticket.domain.repository.TicketInventoryRepository;
import com.eventticket.domain.valueobject.EventId;
import com.eventticket.domain.valueobject.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Use case for creating ticket inventory for an event.
 * Functional Requirement #1: Event inventory management.
 * Using Java 25 - constructor injection without Lombok.
 */
@Service
public class CreateInventoryUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateInventoryUseCase.class);

    private final TicketInventoryRepository inventoryRepository;
    private final EventRepository eventRepository;

    public CreateInventoryUseCase(
            TicketInventoryRepository inventoryRepository,
            EventRepository eventRepository
    ) {
        this.inventoryRepository = inventoryRepository;
        this.eventRepository = eventRepository;
    }

    /**
     * Executes the create inventory use case.
     * Validates that the event exists before creating inventory.
     *
     * @param request Inventory creation request
     * @return Created inventory
     */
    public Mono<TicketInventory> execute(CreateInventoryRequest request) {
        log.info("Creating inventory for event: {}, ticketType: {}", 
                request.eventId(), request.ticketType());
        
        EventId eventId = EventId.of(request.eventId());
        
        // First, verify that the event exists
        return eventRepository.findById(eventId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Event not found: " + request.eventId())))
                .flatMap(event -> {
                    // Check if inventory already exists for this event and ticket type
                    return inventoryRepository.findByEventIdAndTicketType(eventId, request.ticketType())
                            .flatMap(existing -> Mono.<TicketInventory>error(
                                    new IllegalStateException(
                                            "Inventory already exists for event: %s and ticket type: %s"
                                                    .formatted(request.eventId(), request.ticketType())
                                    )
                            ))
                            .switchIfEmpty(createInventory(eventId, event.getName(), request));
                })
                .doOnSuccess(inventory -> log.info(
                        "Inventory created successfully: eventId={}, ticketType={}, quantity={}", 
                        inventory.getEventId().value(), 
                        inventory.getTicketType(), 
                        inventory.getTotalQuantity()))
                .doOnError(error -> log.error("Error creating inventory", error));
    }

    private Mono<TicketInventory> createInventory(
            EventId eventId,
            String eventName,
            CreateInventoryRequest request
    ) {
        Money price = Money.of(request.priceAmount(), request.currencyCode());
        
        TicketInventory inventory = TicketInventory.create(
                eventId,
                request.ticketType(),
                eventName,
                request.totalQuantity(),
                price
        );
        
        return inventoryRepository.save(inventory);
    }
}
