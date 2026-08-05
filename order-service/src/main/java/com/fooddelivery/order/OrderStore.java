package com.fooddelivery.order;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OrderStore {

    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    public void save(Order order) {
        orders.put(order.getOrderId(), order);
    }

    public Order find(String orderId) {
        return orders.get(orderId);
    }
}
