package com.fooddelivery.order;

import java.time.Instant;
import java.util.List;

public class Order {

    public enum Status { CREATED, ACCEPTED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED }

    private final String orderId;
    private final String userId;
    private final List<String> items;
    private volatile Status status;
    private final Instant createdAt;

    public Order(String orderId, String userId, List<String> items) {
        this.orderId = orderId;
        this.userId = userId;
        this.items = items;
        this.status = Status.CREATED;
        this.createdAt = Instant.now();
    }

    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public List<String> getItems() { return items; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
}
