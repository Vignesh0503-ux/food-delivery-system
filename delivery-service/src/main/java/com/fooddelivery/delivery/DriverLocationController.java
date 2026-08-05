package com.fooddelivery.delivery;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DriverLocationController {

    private final DriverLocationCache locationCache;

    public DriverLocationController(DriverLocationCache locationCache) {
        this.locationCache = locationCache;
    }

    @GetMapping("/api/drivers/{driverId}/location")
    public ResponseEntity<?> getLocation(
            @PathVariable("driverId") String driverId) {

        String value = locationCache.getLocation(driverId);

        if (value == null) {
            return ResponseEntity.notFound().build();
        }

        String[] parts = value.split(",");

        return ResponseEntity.ok(Map.of(
                "driverId", driverId,
                "latitude", parts[0],
                "longitude", parts[1],
                "status", parts[2],
                "updatedAt", parts[3]
        ));
    }

    }

