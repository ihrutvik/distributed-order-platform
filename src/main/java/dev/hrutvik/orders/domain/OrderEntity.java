package dev.hrutvik.orders.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders", indexes = @Index(name = "idx_orders_customer", columnList = "customerId"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderEntity {
    @Id
    private UUID id;
    @Column(nullable = false)
    private String customerId;
    @Column(nullable = false)
    private String productCode;
    @Column(nullable = false)
    private int quantity;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;
    @Column(nullable = false, unique = true)
    private String idempotencyKey;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public enum OrderStatus { PENDING, PROCESSING, COMPLETED, FAILED }
}
