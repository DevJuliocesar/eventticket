package com.eventticket.application.dto;

import java.util.List;

/**
 * Data Transfer Object for paginated inventory response.
 * Pure Java Record - Java 25 features.
 */
public record PagedInventoryResponse(
        List<InventoryResponse> inventories,
        int page,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
    public static PagedInventoryResponse of(
            List<InventoryResponse> inventories,
            int page,
            int pageSize,
            long totalElements
    ) {
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        boolean hasNext = page < totalPages;
        boolean hasPrevious = page > 1;
        
        return new PagedInventoryResponse(
                inventories,
                page,
                pageSize,
                totalElements,
                totalPages,
                hasNext,
                hasPrevious
        );
    }
}
