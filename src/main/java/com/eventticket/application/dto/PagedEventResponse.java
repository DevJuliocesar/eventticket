package com.eventticket.application.dto;

import java.util.List;

/**
 * Data Transfer Object for paginated event response.
 * Pure Java Record - Java 25 features.
 */
public record PagedEventResponse(
        List<EventResponse> events,
        int page,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
    public static PagedEventResponse of(
            List<EventResponse> events,
            int page,
            int pageSize,
            long totalElements
    ) {
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        boolean hasNext = page < totalPages;
        boolean hasPrevious = page > 1;
        
        return new PagedEventResponse(
                events,
                page,
                pageSize,
                totalElements,
                totalPages,
                hasNext,
                hasPrevious
        );
    }
}
