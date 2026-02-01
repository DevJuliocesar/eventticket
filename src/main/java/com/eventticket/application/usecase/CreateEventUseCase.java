package com.eventticket.application.usecase;

import com.eventticket.application.dto.CreateEventRequest;
import com.eventticket.application.dto.EventResponse;
import com.eventticket.domain.model.Event;
import com.eventticket.domain.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Use case for creating an event.
 * Using Java 25 - constructor injection without Lombok.
 */
@Service
public class CreateEventUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateEventUseCase.class);

    private final EventRepository eventRepository;

    public CreateEventUseCase(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * Executes the create event use case.
     *
     * @param request Event creation request
     * @return Created event response
     */
    public Mono<EventResponse> execute(CreateEventRequest request) {
        log.info("Creating event: {}", request.name());
        log.debug("Creating event: {}", request.name());
        
        Event event = Event.create(
                request.name(),
                request.description(),
                request.venue(),
                request.eventDate(),
                request.totalCapacity()
        );
        
        return eventRepository.save(event)
                .map(EventResponse::fromDomain)
                .doOnSuccess(response -> log.info("Event created: {}", response.eventId()))
                .doOnError(error -> log.error("Error creating event", error));
    }
}
