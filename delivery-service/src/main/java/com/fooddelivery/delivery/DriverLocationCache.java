package com.fooddelivery.delivery;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Caches the last known location of each driver in Redis so any service
 * instance (or a future "find nearby drivers" query) can read it without
 * needing a sticky connection to the exact instance handling that
 * driver's gRPC stream.
 */
@Component
public class DriverLocationCache {

    private static final String KEY_PREFIX = "driver:location:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;

    public DriverLocationCache(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void updateLocation(String driverId, double lat, double lon, String status) {
        String value = lat + "," + lon + "," + status + "," + System.currentTimeMillis();
        redisTemplate.opsForValue().set(KEY_PREFIX + driverId, value, TTL);
    }

    public String getLocation(String driverId) {
        return redisTemplate.opsForValue().get(KEY_PREFIX + driverId);
    }
}
