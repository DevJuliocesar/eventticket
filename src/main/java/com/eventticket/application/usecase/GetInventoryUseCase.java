package com.eventticket.application.usecase;

import com.eventticket.application.dto.InventoryResponse;
import com.eventticket.domain.repository.EventRepository;
import com.eventticket.domain.repository.TicketInventoryRepository;
import com.eventticket.domain.valueobject.EventId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Use case for listing ticket inventory for an event.
 * Returns all ticket types and their availability for a given event.
 * Using Java 25 - constructor injection without Lombok.
 */
@Service
public class GetInventoryUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetInventoryUseCase.class);

    private final TicketInventoryRepository inventoryRepository;
    private final EventRepository eventRepository;

    public GetInventoryUseCase(
            TicketInventoryRepository inventoryRepository,
            EventRepository eventRepository
    ) {
        this.inventoryRepository = inventoryRepository;
        this.eventRepository = eventRepository;
    }

    /**
     * Executes the get inventory use case.
     * Validates that the event exists before listing inventory.
     *
     * @param eventId Event identifier
     * @return Flux of inventory responses
     */
    public Flux<InventoryResponse> execute(String eventId) {
        log.debug("Listing inventory for event: {}", eventId);
        
        EventId id = EventId.of(eventId);
        
        // First, verify that the event exists
        return eventRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Event not found: " + eventId)))
                .flatMapMany(event -> 
                    // Get all inventory for this event
                    inventoryRepository.findByEventId(id)
                            .map(InventoryResponse::fromDomain)
                )
                .doOnComplete(() -> log.debug("Finished listing inventory for event: {}", eventId))
                .doOnError(error -> log.error("Error listing inventory for event: {}", eventId, error));
    }
}
