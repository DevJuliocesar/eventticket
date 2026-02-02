package com.eventticket.domain.repository;

import com.eventticket.domain.model.Event;
import com.eventticket.domain.model.EventStatus;
import com.eventticket.domain.valueobject.EventId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Repository interface for Event aggregate.
 */
public interface EventRepository {

    /**
     * Saves an event.
     *
     * @param event Event to save
     * @return Saved event
     */
    Mono<Event> save(Event event);

    /**
     * Finds an event by its identifier.
     *
     * @param eventId Event identifier
     * @return Event if found, empty Mono otherwise
     */
    Mono<Event> findById(EventId eventId);

    /**
     * Finds all active events.
     *
     * @return Flux of active events
     */
    Flux<Event> findByStatus(EventStatus status);

    /**
     * Finds upcoming events.
     *
     * @param from From date
     * @return Flux of upcoming events
     */
    Flux<Event> findUpcomingEvents(Instant from);

    /**
     * Updates event with optimistic locking.
     *
     * @param event Event to update
     * @return Updated event
     */
    Mono<Event> updateWithOptimisticLock(Event event);

    /**
     * Deletes an event by its identifier.
     *
     * @param eventId Event identifier
     * @return Mono that completes when deletion is done
     */
    Mono<Void> deleteById(EventId eventId);

    /**
     * Finds all events with pagination.
     *
     * @param page Page number (0-indexed)
     * @param pageSize Number of items per page
     * @return Flux of events for the requested page
     */
    Flux<Event> findAll(int page, int pageSize);

    /**
     * Counts total number of events.
     *
     * @return Total count of events
     */
    Mono<Long> count();
}
