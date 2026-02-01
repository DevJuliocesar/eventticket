package com.eventticket.application.usecase;

import com.eventticket.application.dto.EventAvailabilityResponse;
import com.eventticket.domain.exception.OrderNotFoundException;
import com.eventticket.domain.repository.EventRepository;
import com.eventticket.domain.valueobject.EventId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Use case for querying real-time event ticket availability.
 * Functional Requirement #7: Reactive availability query.
 * Using Java 25 - constructor injection without Lombok.
 */
@Service
public class GetEventAvailabilityUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetEventAvailabilityUseCase.class);

    private final EventRepository eventRepository;

    public GetEventAvailabilityUseCase(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * Executes the get availability use case.
     *
     * @param eventId Event identifier
     * @return Real-time availability response
     */
    public Mono<EventAvailabilityResponse> execute(String eventId) {
        log.debug("Querying availability for event: {}", eventId);
        
        return eventRepository.findById(EventId.of(eventId))
                .switchIfEmpty(Mono.error(new OrderNotFoundException("Event not found: " + eventId)))
                .map(EventAvailabilityResponse::fromDomain)
                .doOnSuccess(response -> log.debug(
                        "Event {} availability - Available: {}, Reserved: {}, Sold: {}",
                        eventId, response.availableTickets(), response.reservedTickets(), response.soldTickets()
                ));
    }
}
