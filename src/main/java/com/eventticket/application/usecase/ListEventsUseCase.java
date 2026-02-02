package com.eventticket.application.usecase;

import com.eventticket.application.dto.EventResponse;
import com.eventticket.application.dto.PagedEventResponse;
import com.eventticket.domain.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Use case for listing events with pagination.
 * Using Java 25 - constructor injection without Lombok.
 */
@Service
public class ListEventsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ListEventsUseCase.class);
    
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final EventRepository eventRepository;

    public ListEventsUseCase(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * Executes the list events use case with pagination.
     *
     * @param page Page number (0-indexed, defaults to 0)
     * @param pageSize Number of items per page (defaults to 10, max 100)
     * @return Paginated event response
     */
    public Mono<PagedEventResponse> execute(Integer page, Integer pageSize) {
        int validPage = page != null && page >= 0 ? page : DEFAULT_PAGE;
        int validPageSize = pageSize != null && pageSize > 0 
                ? Math.min(pageSize, MAX_PAGE_SIZE) 
                : DEFAULT_PAGE_SIZE;
        
        log.debug("Listing events - page: {}, pageSize: {}", validPage, validPageSize);
        
        Mono<List<EventResponse>> eventsMono = eventRepository.findAll(validPage, validPageSize)
                .map(EventResponse::fromDomain)
                .collectList();
        
        Mono<Long> countMono = eventRepository.count();
        
        return Mono.zip(eventsMono, countMono)
                .map(tuple -> {
                    List<EventResponse> events = tuple.getT1();
                    long totalElements = tuple.getT2();
                    return PagedEventResponse.of(events, validPage + 1, validPageSize, totalElements);
                })
                .doOnSuccess(response -> log.debug(
                        "Listed {} events (page {}, total: {})", 
                        response.events().size(), 
                        response.page(), 
                        response.totalElements()))
                .doOnError(error -> log.error("Error listing events", error));
    }
}
