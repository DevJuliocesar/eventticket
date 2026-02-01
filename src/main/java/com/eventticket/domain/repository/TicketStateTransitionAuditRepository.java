package com.eventticket.domain.repository;

import com.eventticket.domain.model.TicketStateTransitionAudit;
import com.eventticket.domain.valueobject.TicketId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Repository interface for TicketStateTransitionAudit.
 * Provides audit trail for all ticket state transitions.
 */
public interface TicketStateTransitionAuditRepository {

    /**
     * Saves an audit entry.
     *
     * @param audit Audit entry to save
     * @return Saved audit entry
     */
    Mono<TicketStateTransitionAudit> save(TicketStateTransitionAudit audit);

    /**
     * Finds all audit entries for a specific ticket.
     *
     * @param ticketId Ticket identifier
     * @return Flux of audit entries ordered by time
     */
    Flux<TicketStateTransitionAudit> findByTicketId(TicketId ticketId);

    /**
     * Finds audit entries within a time range.
     *
     * @param from Start time
     * @param to End time
     * @return Flux of audit entries
     */
    Flux<TicketStateTransitionAudit> findByTimeRange(Instant from, Instant to);

    /**
     * Finds failed transition attempts.
     *
     * @return Flux of failed audit entries
     */
    Flux<TicketStateTransitionAudit> findFailedTransitions();
}
