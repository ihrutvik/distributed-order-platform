package dev.hrutvik.orders.api;

import dev.hrutvik.orders.application.OrderService;
import dev.hrutvik.orders.domain.OrderEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderEntity createOrder(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(idempotencyKey, request.customerId(), request.amount());
    }

    @GetMapping("/{orderId}")
    public OrderEntity getOrder(@PathVariable UUID orderId) {
        return orderService.getOrder(orderId);
    }

    public record CreateOrderRequest(
            @NotBlank String customerId,
            @DecimalMin(value = "0.01") BigDecimal amount) {
    }
}
