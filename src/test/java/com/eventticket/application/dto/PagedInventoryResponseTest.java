package com.eventticket.application.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PagedInventoryResponse DTO Tests")
class PagedInventoryResponseTest {

    @Test
    @DisplayName("Should create paginated inventory response correctly")
    void shouldCreatePaginatedInventoryResponse() {
        // Given
        List<InventoryResponse> inventories = List.of(
                new InventoryResponse("event-1", "VIP", "Event 1",
                        100, 80, 10, 10, BigDecimal.valueOf(100.0), "USD", 0),
                new InventoryResponse("event-1", "General", "Event 1",
                        200, 150, 30, 20, BigDecimal.valueOf(50.0), "USD", 0)
        );

        // When
        PagedInventoryResponse response = PagedInventoryResponse.of(inventories, 1, 10, 15L);

        // Then
        assertThat(response.inventories()).hasSize(2);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.pageSize()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(15);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.hasPrevious()).isFalse();
    }

    @Test
    @DisplayName("Should handle empty inventory results")
    void shouldHandleEmptyInventoryResults() {
        // Given
        List<InventoryResponse> inventories = new ArrayList<>();

        // When
        PagedInventoryResponse response = PagedInventoryResponse.of(inventories, 1, 10, 0L);

        // Then
        assertThat(response.inventories()).isEmpty();
        assertThat(response.totalElements()).isEqualTo(0);
        assertThat(response.totalPages()).isEqualTo(0);
    }
}
