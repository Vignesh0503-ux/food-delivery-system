package com.fooddelivery.order;

import com.fooddelivery.grpc.user.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderStore orderStore;
    private final UserServiceClient userServiceClient;
    private final OrderEventPublisher eventPublisher;
    private final NotificationStreamClient notificationStreamClient;

    public OrderController(OrderStore orderStore,
                            UserServiceClient userServiceClient,
                            OrderEventPublisher eventPublisher,
                            NotificationStreamClient notificationStreamClient) {
        this.orderStore = orderStore;
        this.userServiceClient = userServiceClient;
        this.eventPublisher = eventPublisher;
        this.notificationStreamClient = notificationStreamClient;
    }

    public record CreateOrderRequest(String userId, List<String> items) {}

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request) {
        // 1. UNARY gRPC call -> validate the customer via User Service
        Optional<UserResponse> user = userServiceClient.fetchUser(request.userId());
        if (user.isEmpty() || !user.get().getActive()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("error", "user not found or inactive: " + request.userId()));
        }

        // 2. Create and persist the order
        Order order = new Order(UUID.randomUUID().toString(), request.userId(), request.items());
        orderStore.save(order);

        // 3. Kafka event -> anyone subscribed to "order-created" hears about it
        eventPublisher.publishOrderCreated(order);

        // 4. SERVER STREAMING gRPC call -> get a live stream of notification
        //    delivery statuses (push/sms/email) as Notification Service sends them
        notificationStreamClient.requestNotifications(order.getOrderId(), order.getUserId(), "ORDER_CREATED");

        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable String orderId) {
        Order order = orderStore.find(orderId);
        return order == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(order);
    }

    @PostMapping("/{orderId}/deliver")
    public ResponseEntity<Order> markDelivered(@PathVariable String orderId) {
        Order order = orderStore.find(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        order.setStatus(Order.Status.DELIVERED);
        eventPublisher.publishOrderDelivered(order);
        notificationStreamClient.requestNotifications(order.getOrderId(), order.getUserId(), "ORDER_DELIVERED");
        return ResponseEntity.ok(order);
    }
}
