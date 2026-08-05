package com.fooddelivery.simulator;

import com.fooddelivery.grpc.delivery.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Standalone CLI that plays the role of a driver's mobile app.
 *
 * Usage:
 *   java -jar driver-simulator.jar <host> <port> <driverId> <mode>
 *   mode = "history"  -> demonstrates CLIENT STREAMING (UploadLocationHistory)
 *   mode = "track"    -> demonstrates BIDIRECTIONAL STREAMING (TrackDriver)
 *
 * Example:
 *   java -jar driver-simulator.jar localhost 9094 driver-42 track
 */
public class DriverSimulator {

    public static void main(String[] args) throws InterruptedException {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9094;
        String driverId = args.length > 2 ? args[2] : "driver-42";
        String mode = args.length > 3 ? args[3] : "track";

        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();

        DeliveryServiceGrpc.DeliveryServiceStub asyncStub = DeliveryServiceGrpc.newStub(channel);

        if ("history".equalsIgnoreCase(mode)) {
            runLocationHistoryUpload(asyncStub, driverId);
        } else {
            runLiveTracking(asyncStub, driverId);
        }

        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }


    private static void runLocationHistoryUpload(DeliveryServiceGrpc.DeliveryServiceStub stub, String driverId)
            throws InterruptedException {

        CountDownLatch done = new CountDownLatch(1);

        StreamObserver<LocationPing> requestObserver = stub.uploadLocationHistory(new StreamObserver<>() {
            @Override
            public void onNext(LocationHistoryAck ack) {
                System.out.printf("Server acked %d pings for driver %s%n", ack.getPingsReceived(), ack.getDriverId());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("History upload failed: " + t.getMessage());
                done.countDown();
            }

            @Override
            public void onCompleted() {
                done.countDown();
            }
        });

        double lat = 13.0827, lon = 80.2707; // Chennai, drifting slightly each ping
        for (int i = 0; i < 10; i++) {
            requestObserver.onNext(LocationPing.newBuilder()
                    .setDriverId(driverId)
                    .setLatitude(lat + i * 0.001)
                    .setLongitude(lon + i * 0.001)
                    .setTimestamp(System.currentTimeMillis())
                    .build());
        }
        requestObserver.onCompleted();

        done.await(5, TimeUnit.SECONDS);
    }

    /** BIDIRECTIONAL STREAMING demo: driver pushes updates, server pushes dispatch instructions concurrently. */
    private static void runLiveTracking(DeliveryServiceGrpc.DeliveryServiceStub stub, String driverId)
            throws InterruptedException {

        CountDownLatch done = new CountDownLatch(1);

        StreamObserver<DriverUpdate> requestObserver = stub.trackDriver(new StreamObserver<>() {
            @Override
            public void onNext(DispatchInstruction instruction) {
                System.out.printf("Dispatch -> %s: %s (order=%s)%n",
                        instruction.getInstructionType(), instruction.getMessage(), instruction.getOrderId());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Tracking stream failed: " + t.getMessage());
                done.countDown();
            }

            @Override
            public void onCompleted() {
                done.countDown();
            }
        });

        double lat = 13.0827, lon = 80.2707;
        try {
            for (int i = 0; i < 15; i++) {
                requestObserver.onNext(DriverUpdate.newBuilder()
                        .setDriverId(driverId)
                        .setLatitude(lat + i * 0.0008)
                        .setLongitude(lon + i * 0.0008)
                        .setStatus("AVAILABLE")
                        .setTimestamp(System.currentTimeMillis())
                        .build());
                Thread.sleep(500);
            }
        } finally {
            requestObserver.onCompleted();
        }

        done.await(5, TimeUnit.SECONDS);
    }
}
