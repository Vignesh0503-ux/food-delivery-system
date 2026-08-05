package com.fooddelivery.user;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory user store, seeded with a few demo users so the
 * gRPC and REST layers have something real to return. Swap for a
 * JPA repository backed by Postgres/MySQL for anything beyond a demo.
 */
@Component
public class UserStore {

    private final Map<String, User> users = new ConcurrentHashMap<>();

    public UserStore() {
        users.put("u1", new User("u1", "Asha Rao", "+91-9000000001", "12 MG Road, Chennai", true));
        users.put("u2", new User("u2", "Ben Fernandes", "+91-9000000002", "45 Anna Salai, Chennai", true));
        users.put("u3", new User("u3", "Chitra Iyer", "+91-9000000003", "8 OMR, Chennai", false));
    }

    public User find(String userId) {
        return users.get(userId);
    }

    public record User(String id, String name, String phone, String address, boolean active) {}
}
