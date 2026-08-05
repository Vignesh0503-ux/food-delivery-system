package com.fooddelivery.delivery;

import com.fooddelivery.grpc.delivery.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@GrpcService
public class DeliveryGrpcService extends DeliveryServiceGrpc.DeliveryServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(DeliveryGrpcService.class);

    private final DriverLocationCache locationCache;

    public DeliveryGrpcService(DriverLocationCache locationCache) {
        this.locationCache = locationCache;
    }

    /**
     * CLIENT STREAMING. A driver coming back online streams every location
     * ping it buffered while offline. The server just counts them and
     * caches the most recent one, then sends a single ack once the
     * client signals it's done (onCompleted).
     */
    @Override
    public StreamObserver<LocationPing> uploadLocationHistory(StreamObserver<LocationHistoryAck> responseObserver) {
        return new StreamObserver<>() {
            private String driverId;
            private final AtomicInteger count = new AtomicInteger(0);

            @Override
            public void onNext(LocationPing ping) {
                driverId = ping.getDriverId();
                count.incrementAndGet();
                locationCache.updateLocation(ping.getDriverId(), ping.getLatitude(), ping.getLongitude(), "SYNCING");
            }

            @Override
            public void onError(Throwable t) {
                log.warn("Location history upload failed for driver {}: {}", driverId, t.getMessage());
            }

            @Override
            public void onCompleted() {
                LocationHistoryAck ack = LocationHistoryAck.newBuilder()
                        .setDriverId(driverId == null ? "unknown" : driverId)
                        .setPingsReceived(count.get())
                        .build();
                log.info("Driver {} synced {} buffered location pings", ack.getDriverId(), ack.getPingsReceived());
                responseObserver.onNext(ack);
                responseObserver.onCompleted();
            }
        };
    }


    @Override
    public StreamObserver<DriverUpdate> trackDriver(StreamObserver<DispatchInstruction> responseObserver) {
        return new StreamObserver<>() {
            private final AtomicInteger updateCount = new AtomicInteger(0);

            @Override
            public void onNext(DriverUpdate update) {
                locationCache.updateLocation(update.getDriverId(), update.getLatitude(), update.getLongitude(), update.getStatus());
                log.info("Driver {} @ ({}, {}) status={}",
                        update.getDriverId(), update.getLatitude(), update.getLongitude(), update.getStatus());

                int n = updateCount.incrementAndGet();

                DispatchInstruction instruction;
                if ("AVAILABLE".equals(update.getStatus()) && n % 5 == 0) {
                    instruction = DispatchInstruction.newBuilder()
                            .setDriverId(update.getDriverId())
                            .setInstructionType("ASSIGN_ORDER")
                            .setOrderId(UUID.randomUUID().toString())
                            .setMessage("New pickup assigned near your location")
                            .build();
                } else {
                    instruction = DispatchInstruction.newBuilder()
                            .setDriverId(update.getDriverId())
                            .setInstructionType("NOOP")
                            .setMessage("Keep going")
                            .build();
                }

                responseObserver.onNext(instruction);
            }

            @Override
            public void onError(Throwable t) {
                log.warn("Driver tracking stream failed: {}", t.getMessage());
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }
}
