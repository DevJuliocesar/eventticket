package com.eventticket.application.usecase;

import com.eventticket.application.dto.CreateEventRequest;
import com.eventticket.application.dto.EventResponse;
import com.eventticket.domain.model.Event;
import com.eventticket.domain.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Use case for creating an event.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateEventUseCase {

    private final EventRepository eventRepository;

    /**
     * Executes the create event use case.
     *
     * @param request Event creation request
     * @return Created event response
     */
    public Mono<EventResponse> execute(CreateEventRequest request) {
        log.info("Creating event: {}", request.name());
        
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
