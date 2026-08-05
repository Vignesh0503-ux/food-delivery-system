package com.fooddelivery.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Publishes OrderCreated / OrderDelivered events to Kafka. Notification
 * Service (and any other interested consumer, e.g. analytics) subscribes
 * to these topics independently of the synchronous gRPC calls.
 */
@Component
public class OrderEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${order.kafka.topic.created}")
    private String createdTopic;

    @Value("${order.kafka.topic.delivered}")
    private String deliveredTopic;

    public OrderEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderCreated(Order order) {
        publish(createdTopic, order, "ORDER_CREATED");
    }

    public void publishOrderDelivered(Order order) {
        publish(deliveredTopic, order, "ORDER_DELIVERED");
    }

    private void publish(String topic, Order order, String eventType) {
        try {
            Map<String, Object> event = Map.of(
                    "eventType", eventType,
                    "orderId", order.getOrderId(),
                    "userId", order.getUserId(),
                    "items", order.getItems(),
                    "status", order.getStatus().name()
            );
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, order.getOrderId(), payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to publish " + eventType + " event", e);
        }
    }
}
