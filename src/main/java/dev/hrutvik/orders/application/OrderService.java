package dev.hrutvik.orders.application;

import dev.hrutvik.orders.domain.OrderEntity;
import dev.hrutvik.orders.domain.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework