package com.eventticket.infrastructure.api;

import com.eventticket.application.dto.CreateOrderRequest;
import com.eventticket.application.dto.OrderResponse;
import com.eventticket.application.usecase.ConfirmTicketOrderUseCase;
import com.eventticket.application.usecase.CreateTicketOrderUseCase;
import com.eventticket.application.usecase.GetTicketOrderUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST controller for ticket order operations.
 * Implements the Adapter pattern (Hexagonal Architecture).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class TicketOrderController {

    private final CreateTicketOrderUseCase createOrderUseCase;
    private final ConfirmTicketOrderUseCase confirmOrderUseCase;
    private final GetTicketOrderUseCase getOrderUseCase;

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
     * Confirms an order.
     *
     * @param orderId Order identifier
     * @return Confirmed order response
     */
    @PostMapping(
            value = "/{orderId}/confirm",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<OrderResponse> confirmOrder(@PathVariable String orderId) {
        log.info("Received confirm order request for orderId: {}", orderId);
        return confirmOrderUseCase.execute(orderId);
    }
}
