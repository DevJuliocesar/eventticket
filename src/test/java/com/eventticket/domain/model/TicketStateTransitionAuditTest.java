package com.eventticket.domain.model;

import com.eventticket.domain.valueobject.TicketId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TicketStateTransitionAudit Tests")
class TicketStateTransitionAuditTest {

    @Test
    @DisplayName("Should create successful audit entry")
    void shouldCreateSuccessfulAuditEntry() {
        // Given
        TicketId ticketId = TicketId.of("ticket-123");
        TicketStatus fromStatus = TicketStatus.AVAILABLE;
        TicketStatus toStatus = TicketStatus.RESERVED;
        String performedBy = "system";
        String reason = "Order created";

        // When
        TicketStateTransitionAudit audit = TicketStateTransitionAudit.success(
                ticketId, fromStatus, toStatus, performedBy, reason
        );

        // Then
        assertThat(audit.ticketId()).isEqualTo(ticketId);
        assertThat(audit.fromStatus()).isEqualTo(fromStatus);
        assertThat(audit.toStatus()).isEqualTo(toStatus);
        assertThat(audit.performedBy()).isEqualTo(performedBy);
        assertThat(audit.reason()).isEqualTo(reason);
        assertThat(audit.successful()).isTrue();
        assertThat(audit.errorMessage()).isNull();
        assertThat(audit.transitionTime()).isNotNull();
        assertThat(audit.wasSuccessful()).isTrue();
    }

    @Test
    @DisplayName("Should create failed audit entry")
    void shouldCreateFailedAuditEntry() {
        // Given
        TicketId ticketId = TicketId.of("ticket-456");
        TicketStatus fromStatus = TicketStatus.AVAILABLE;
        TicketStatus attemptedStatus = TicketStatus.SOLD;
        String performedBy = "user";
        String errorMessage = "Invalid transition";

        // When
        TicketStateTransitionAudit audit = TicketStateTransitionAudit.failure(
                ticketId, fromStatus, attemptedStatus, performedBy, errorMessage
        );

        // Then
        assertThat(audit.ticketId()).isEqualTo(ticketId);
        assertThat(audit.fromStatus()).isEqualTo(fromStatus);
        assertThat(audit.toStatus()).isEqualTo(attemptedStatus);
        assertThat(audit.performedBy()).isEqualTo(performedBy);
        assertThat(audit.reason()).isEqualTo("FAILED_ATTEMPT");
        assertThat(audit.successful()).isFalse();
        assertThat(audit.errorMessage()).isEqualTo(errorMessage);
        assertThat(audit.transitionTime()).isNotNull();
        assertThat(audit.wasSuccessful()).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when ticketId is null")
    void shouldThrowExceptionWhenTicketIdIsNull() {
        // When & Then
        assertThatThrownBy(() -> new TicketStateTransitionAudit(
                null,
                TicketStatus.AVAILABLE,
                TicketStatus.RESERVED,
                Instant.now(),
                "system",
                "test",
                true,
                null
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ticketId cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when fromStatus is null")
    void shouldThrowExceptionWhenFromStatusIsNull() {
        // When & Then
        assertThatThrownBy(() -> new TicketStateTransitionAudit(
                TicketId.of("ticket-123"),
                null,
                TicketStatus.RESERVED,
                Instant.now(),
                "system",
                "test",
                true,
                null
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("fromStatus cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when toStatus is null")
    void shouldThrowExceptionWhenToStatusIsNull() {
        // When & Then
        assertThatThrownBy(() -> new TicketStateTransitionAudit(
                TicketId.of("ticket-123"),
                TicketStatus.AVAILABLE,
                null,
                Instant.now(),
                "system",
                "test",
                true,
                null
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("toStatus cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when performedBy is null")
    void shouldThrowExceptionWhenPerformedByIsNull() {
        // When & Then
        assertThatThrownBy(() -> new TicketStateTransitionAudit(
                TicketId.of("ticket-123"),
                TicketStatus.AVAILABLE,
                TicketStatus.RESERVED,
                Instant.now(),
                null,
                "test",
                true,
                null
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("performedBy cannot be null");
    }

    @Test
    @DisplayName("Should generate correct description for successful transition")
    void shouldGenerateCorrectDescriptionForSuccessfulTransition() {
        // Given
        TicketStateTransitionAudit audit = TicketStateTransitionAudit.success(
                TicketId.of("ticket-123"),
                TicketStatus.AVAILABLE,
                TicketStatus.RESERVED,
                "system",
                "Order created"
        );

        // When
        String description = audit.getDescription();

        // Then
        assertThat(description).contains("ticket-123");
        assertThat(description).contains("AVAILABLE");
        assertThat(description).contains("RESERVED");
        assertThat(description).contains("system");
        assertThat(description).contains("Order created");
    }

    @Test
    @DisplayName("Should generate correct description for failed transition")
    void shouldGenerateCorrectDescriptionForFailedTransition() {
        // Given
        TicketStateTransitionAudit audit = TicketStateTransitionAudit.failure(
                TicketId.of("ticket-456"),
                TicketStatus.AVAILABLE,
                TicketStatus.SOLD,
                "user",
                "Invalid transition"
        );

        // When
        String description = audit.getDescription();

        // Then
        assertThat(description).contains("Failed");
        assertThat(description).contains("ticket-456");
        assertThat(description).contains("AVAILABLE");
        assertThat(description).contains("SOLD");
        assertThat(description).contains("user");
        assertThat(description).contains("Invalid transition");
    }
}
