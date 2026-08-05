package com.fooddelivery.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Independent async path: Notification Service also consumes order
 * events straight from Kafka (e.g. for audit logging, or to trigger
 * notifications that don't originate from the synchronous gRPC call
 * in OrderController - for example a downstream system publishing
 * "order-delivered" directly to Kafka).
 */
@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    @KafkaListener(topics = "${order.kafka.topic.created}", groupId = "notification-service")
    public void onOrderCreated(String payload) {
        log.info("[kafka] order-created event received: {}", payload);
    }

    @KafkaListener(topics = "${order.kafka.topic.delivered}", groupId = "notification-service")
    public void onOrderDelivered(String payload) {
        log.info("[kafka] order-delivered event received: {}", payload);
    }
}
