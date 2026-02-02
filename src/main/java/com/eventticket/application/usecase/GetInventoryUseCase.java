package com.eventticket.application.usecase;

import com.eventticket.application.dto.InventoryResponse;
import com.eventticket.application.dto.PagedInventoryResponse;
import com.eventticket.domain.repository.EventRepository;
import com.eventticket.domain.repository.TicketInventoryRepository;
import com.eventticket.domain.valueobject.EventId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Use case for listing ticket inventory for an event.
 * Returns all ticket types and their availability for a given event.
 * Using Java 25 - constructor injection without Lombok.
 */
@Service
public class GetInventoryUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetInventoryUseCase.class);
    
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

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
     * Executes the get inventory use case without pagination (backward compatibility).
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

    /**
     * Executes the get inventory use case with pagination.
     * Validates that the event exists before listing inventory.
     *
     * @param eventId Event identifier
     * @param page Page number (0-indexed, defaults to 0)
     * @param pageSize Number of items per page (defaults to 10, max 100)
     * @return Paginated inventory response
     */
    public Mono<PagedInventoryResponse> execute(String eventId, Integer page, Integer pageSize) {
        int validPage = page != null && page >= 0 ? page : DEFAULT_PAGE;
        int validPageSize = pageSize != null && pageSize > 0 
                ? Math.min(pageSize, MAX_PAGE_SIZE) 
                : DEFAULT_PAGE_SIZE;
        
        log.debug("Listing inventory for event: {}, page: {}, pageSize: {}", eventId, validPage, validPageSize);
        
        EventId id = EventId.of(eventId);
        
        // First, verify that the event exists
        return eventRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Event not found: " + eventId)))
                .flatMap(event -> {
                    Mono<List<InventoryResponse>> inventoriesMono = inventoryRepository
                            .findByEventId(id, validPage, validPageSize)
                            .map(InventoryResponse::fromDomain)
                            .collectList();
                    
                    Mono<Long> countMono = inventoryRepository.countByEventId(id);
                    
                    return Mono.zip(inventoriesMono, countMono)
                            .map(tuple -> {
                                List<InventoryResponse> inventories = tuple.getT1();
                                long totalElements = tuple.getT2();
                                return PagedInventoryResponse.of(inventories, validPage + 1, validPageSize, totalElements);
                            });
                })
                .doOnSuccess(response -> log.debug(
                        "Listed {} inventories for event {} (page {}, total: {})", 
                        response.inventories().size(), 
                        eventId,
                        response.page(), 
                        response.totalElements()))
                .doOnError(error -> log.error("Error listing inventory for event: {}", eventId, error));
    }
}
