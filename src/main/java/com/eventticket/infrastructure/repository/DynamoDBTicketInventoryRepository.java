package com.eventticket.infrastructure.repository;

import com.eventticket.domain.model.TicketInventory;
import com.eventticket.domain.repository.TicketInventoryRepository;
import com.eventticket.domain.valueobject.EventId;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory implementation of TicketInventoryRepository.
 * TODO: Replace with DynamoDB implementation.
 */
@Repository
public class DynamoDBTicketInventoryRepository implements TicketInventoryRepository {

    private final ConcurrentMap<String, TicketInventory> inventory = new ConcurrentHashMap<>();

    private String key(EventId eventId, String ticketType) {
        return "%s:%s".formatted(eventId.value(), ticketType);
    }

    @Override
    public Mono<TicketInventory> save(TicketInventory inv) {
        String key = key(inv.getEventId(), inv.getTicketType());
        inventory.put(key, inv);
        return Mono.just(inv);
    }

    @Override
    public Mono<TicketInventory> findByEventIdAndTicketType(EventId eventId, String ticketType) {
        String key = key(eventId, ticketType);
        TicketInventory inv = inventory.get(key);
        return inv != null ? Mono.just(inv) : Mono.empty();
    }

    @Override
    public Flux<TicketInventory> findByEventId(EventId eventId) {
        String prefix = eventId.value() + ":";
        return Flux.fromIterable(inventory.values())
                .filter(inv -> inv.getEventId().equals(eventId));
    }

    @Override
    public Mono<TicketInventory> updateWithOptimisticLock(TicketInventory inv) {
        String key = key(inv.getEventId(), inv.getTicketType());
        TicketInventory existing = inventory.get(key);
        if (existing != null && existing.getVersion() != inv.getVersion() - 1) {
            return Mono.error(new IllegalStateException("Optimistic lock failure"));
        }
        inventory.put(key, inv);
        return Mono.just(inv);
    }
}
