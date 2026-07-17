package dev.hrutvik.orders.application;

import dev.hrutvik.orders.domain.OrderEntity;
import dev.hrutvik.orders.domain.OrderEntity.OrderStatus;
import dev.hrutvik.orders.domain.OrderRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.kafka.order-created-topic}")
    private String orderCreatedTopic;

    @Transactional
    public OrderEntity createOrder(
            String idempotencyKey,
            String customerId,
            String productCode,
            int quantity,
            BigDecimal amount) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }

        return orderRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> createAndPublish(
                        idempotencyKey, customerId, productCode, quantity, amount));
    }

    private OrderEntity createAndPublish(
            String idempotencyKey,
            String customerId,
            String productCode,
            int quantity,
            BigDecimal amount) {

        OrderEntity order = OrderEntity.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .productCode(productCode)
                .quantity(quantity)
                .amount(amount)
                .status(OrderStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .build();

        OrderEntity saved = orderRepository.saveAndFlush(order);
        redisTemplate.opsForValue().set(
                "idempotency:" + idempotencyKey,
                saved.getId().toString(),
                Duration.ofHours(24));
        kafkaTemplate.send(orderCreatedTopic, saved.getId().toString(), saved.getId().toString());
        return saved;
    }

    @Transactional(readOnly = true)
    public OrderEntity getOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
    }

    @KafkaListener(topics = "${app.kafka.order-created-topic}")
    @Transactional
    public void processOrder(String orderId) {
        UUID id = UUID.fromString(orderId);
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + id));

        if (order.getStatus() == OrderStatus.COMPLETED) {
            return;
        }

        order.setStatus(OrderStatus.PROCESSING);
        orderRepository.save(order);

        // Production integrations would call payment and inventory services here.
        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);
    }
}
