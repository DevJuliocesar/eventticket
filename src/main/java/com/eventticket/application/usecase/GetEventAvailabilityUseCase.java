package com.eventticket.application.usecase;

import com.eventticket.application.dto.EventAvailabilityResponse;
import com.eventticket.domain.exception.OrderNotFoundException;
import com.eventticket.domain.repository.EventRepository;
import com.eventticket.domain.valueobject.EventId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Use case for querying real-time event ticket availability.
 * Functional Requirement #7: Reactive availability query.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetEventAvailabilityUseCase {

    private final EventRepository eventRepository;

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
