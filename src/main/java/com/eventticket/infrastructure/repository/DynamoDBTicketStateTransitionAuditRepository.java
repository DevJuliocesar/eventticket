package com.eventticket.infrastructure.repository;

import com.eventticket.domain.model.TicketStateTransitionAudit;
import com.eventticket.domain.repository.TicketStateTransitionAuditRepository;
import com.eventticket.domain.valueobject.TicketId;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory implementation of TicketStateTransitionAuditRepository.
 * TODO: Replace with DynamoDB implementation.
 */
@Repository
public class DynamoDBTicketStateTransitionAuditRepository implements TicketStateTransitionAuditRepository {

    private final ConcurrentMap<String, List<TicketStateTransitionAudit>> audits = new ConcurrentHashMap<>();

    @Override
    public Mono<TicketStateTransitionAudit> save(TicketStateTransitionAudit audit) {
        String ticketId = audit.ticketId().value();
        audits.computeIfAbsent(ticketId, k -> new ArrayList<>()).add(audit);
        return Mono.just(audit);
    }

    @Override
    public Flux<TicketStateTransitionAudit> findByTicketId(TicketId ticketId) {
        List<TicketStateTransitionAudit> ticketAudits = audits.get(ticketId.value());
        return ticketAudits != null 
                ? Flux.fromIterable(ticketAudits)
                : Flux.empty();
    }

    @Override
    public Flux<TicketStateTransitionAudit> findByTimeRange(Instant from, Instant to) {
        return Flux.fromIterable(audits.values())
                .flatMap(Flux::fromIterable)
                .filter(audit -> {
                    Instant time = audit.transitionTime();
                    return !time.isBefore(from) && !time.isAfter(to);
                });
    }

    @Override
    public Flux<TicketStateTransitionAudit> findFailedTransitions() {
        return Flux.fromIterable(audits.values())
                .flatMap(Flux::fromIterable)
                .filter(audit -> !audit.successful());
    }
}
