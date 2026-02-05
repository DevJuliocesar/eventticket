package com.eventticket.infrastructure.api;

import com.eventticket.application.dto.ComplimentaryOrderRequest;
import com.eventticket.application.dto.ConfirmOrderRequest;
import com.eventticket.application.dto.CreateOrderRequest;
import com.eventticket.application.dto.OrderResponse;
import com.eventticket.application.usecase.ConfirmTicketOrderUseCase;
import com.eventticket.application.usecase.CreateTicketOrderUseCase;
import com.eventticket.application.usecase.GetTicketOrderUseCase;
import com.eventticket.application.usecase.MarkOrderAsComplimentaryUseCase;
import com.eventticket.application.usecase.MarkOrderAsSoldUseCase;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST controller for ticket order operations.
 * Implements the Adapter pattern (Hexagonal Architecture).
 * Using Java 25 - constructor injection without Lombok.
 */
@RestController
@RequestMapping("/api/v1/orders")
public class TicketOrderController {

    private static final Logger log = LoggerFactory.getLogger(TicketOrderController.class);

    private final CreateTicketOrderUseCase createOrderUseCase;
    private final ConfirmTicketOrderUseCase confirmOrderUseCase;
    private final GetTicketOrderUseCase getOrderUseCase;
    private final MarkOrderAsSoldUseCase markOrderAsSoldUseCase;
    private final MarkOrderAsComplimentaryUseCase markOrderAsComplimentaryUseCase;

    public TicketOrderController(
            CreateTicketOrderUseCase createOrderUseCase,
            ConfirmTicketOrderUseCase confirmOrderUseCase,
            GetTicketOrderUseCase getOrderUseCase,
            MarkOrderAsSoldUseCase markOrderAsSoldUseCase,
            MarkOrderAsComplimentaryUseCase markOrderAsComplimentaryUseCase
    ) {
        this.createOrderUseCase = createOrderUseCase;
        this.confirmOrderUseCase = confirmOrderUseCase;
        this.getOrderUseCase = getOrderUseCase;
        this.markOrderAsSoldUseCase = markOrderAsSoldUseCase;
        this.markOrderAsComplimentaryUseCase = markOrderAsComplimentaryUseCase;
    }

    /**
     * Creates a new ticket order.
     *
     * @param request Order creation request
     * @return Created order response
     */
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("Received create order request for customer: {}", request.customerId());
        return createOrderUseCase.execute(request);
    }

    /**
     * Retrieves an order by its identifier.
     *
     * @param orderId Order identifier
     * @return Order response
     */
    @GetMapping(
            value = "/{orderId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<OrderResponse> getOrder(@PathVariable String orderId) {
        log.debug("Received get order request for orderId: {}", orderId);
        return getOrderUseCase.execute(orderId);
    }

    /**
     * Confirms an order with customer payment information.
     *
     * @param orderId Order identifier
     * @param request Customer payment information
     * @return Confirmed order response
     */
    @PostMapping(
            value = "/{orderId}/confirm",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<OrderResponse> confirmOrder(
            @PathVariable String orderId,
            @Valid @RequestBody ConfirmOrderRequest request
    ) {
        log.info("Received confirm order request for orderId: {} with customer info", orderId);
        return confirmOrderUseCase.execute(orderId, request);
    }

    /**
     * Marks an order as sold (payment completed).
     * Assigns unique seat numbers to tickets and updates inventory.
     * Order must be in PENDING_CONFIRMATION status.
     *
     * @param orderId Order identifier
     * @return Sold order response
     */
    @PostMapping(
            value = "/{orderId}/mark-as-sold",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<OrderResponse> markOrderAsSold(@PathVariable String orderId) {
        log.info("Received mark order as sold request for orderId: {}", orderId);
        return markOrderAsSoldUseCase.execute(orderId);
    }

    /**
     * Marks an order as complimentary (free tickets).
     * Assigns unique seat numbers to tickets and adjusts inventory.
     * Order can be in AVAILABLE, RESERVED, or PENDING_CONFIRMATION status.
     * Business Rule: COMPLIMENTARY is final but NOT counted as revenue.
     *
     * @param orderId Order identifier
     * @param request Reason for marking as complimentary
     * @return Complimentary order response
     */
    @PostMapping(
            value = "/{orderId}/mark-as-complimentary",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<OrderResponse> markOrderAsComplimentary(
            @PathVariable String orderId,
            @Valid @RequestBody ComplimentaryOrderRequest request
    ) {
        log.info("Received mark order as complimentary request for orderId: {}, reason: {}",
                orderId, request.reason());
        return markOrderAsComplimentaryUseCase.execute(orderId, request.reason());
    }
}
