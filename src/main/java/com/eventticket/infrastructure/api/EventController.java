package com.eventticket.infrastructure.api;

import com.eventticket.application.dto.CreateEventRequest;
import com.eventticket.application.dto.CreateInventoryRequest;
import com.eventticket.application.dto.EventAvailabilityResponse;
import com.eventticket.application.dto.EventResponse;
import com.eventticket.application.dto.InventoryResponse;
import com.eventticket.application.usecase.CreateEventUseCase;
import com.eventticket.application.usecase.CreateInventoryUseCase;
import com.eventticket.application.usecase.GetEventAvailabilityUseCase;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST controller for event operations.
 * Functional Requirements #1 and #7: Event management and availability query.
 * Using Java 25 - constructor injection without Lombok.
 */
@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private static final Logger log = LoggerFactory.getLogger(EventController.class);

    private final CreateEventUseCase createEventUseCase;
    private final GetEventAvailabilityUseCase getEventAvailabilityUseCase;
    private final CreateInventoryUseCase createInventoryUseCase;

    public EventController(
            CreateEventUseCase createEventUseCase,
            GetEventAvailabilityUseCase getEventAvailabilityUseCase,
            CreateInventoryUseCase createInventoryUseCase
    ) {
        this.createEventUseCase = createEventUseCase;
        this.getEventAvailabilityUseCase = getEventAvailabilityUseCase;
        this.createInventoryUseCase = createInventoryUseCase;
    }

    /**
     * Creates a new event.
     * Functional Requirement #1: Event creation.
     *
     * @param request Event creation request
     * @return Created event response
     */
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        log.info("Received create event request: {}", request.name());
        return createEventUseCase.execute(request);
    }

    /**
     * Gets real-time availability for an event.
     * Functional Requirement #7: Reactive availability query.
     *
     * @param eventId Event identifier
     * @return Real-time availability response
     */
    @GetMapping(
            value = "/{eventId}/availability",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<EventAvailabilityResponse> getEventAvailability(@PathVariable String eventId) {
        log.debug("Received get availability request for event: {}", eventId);
        return getEventAvailabilityUseCase.execute(eventId);
    }

    /**
     * Creates ticket inventory for an event.
     * Functional Requirement #1: Event inventory management.
     * Allows creating multiple ticket types (VIP, General, etc.) for the same event.
     *
     * @param request Inventory creation request
     * @return Created inventory response
     */
    @PostMapping(
            value = "/inventory",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<InventoryResponse> createInventory(@Valid @RequestBody CreateInventoryRequest request) {
        log.info("Received create inventory request: eventId={}, ticketType={}", 
                request.eventId(), request.ticketType());
        return createInventoryUseCase.execute(request)
                .map(InventoryResponse::fromDomain);
    }
}
