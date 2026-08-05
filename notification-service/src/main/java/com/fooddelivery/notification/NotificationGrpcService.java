package com.fooddelivery.notification;

import com.fooddelivery.grpc.notification.NotificationServiceGrpc;
import com.fooddelivery.grpc.notification.NotificationStatus;
import com.fooddelivery.grpc.notification.NotifyRequest;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * SERVER STREAMING gRPC endpoint. Takes ONE request and streams back a
 * NotificationStatus message per channel (push, sms, email) as each one
 * is "sent". In a real system these sends would be async and the stream
 * would emit as each one actually completes; here we simulate that with
 * a short per-channel delay so the streaming behaviour is visible.
 */
@GrpcService
public class NotificationGrpcService extends NotificationServiceGrpc.NotificationServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(NotificationGrpcService.class);
    private static final List<String> CHANNELS = List.of("PUSH", "SMS", "EMAIL");

    @Override
    public void subscribeOrderNotifications(NotifyRequest request, StreamObserver<NotificationStatus> responseObserver) {
        log.info("Streaming notifications for order {} ({})", request.getOrderId(), request.getEventType());

        for (String channel : CHANNELS) {
            try {
                Thread.sleep(150); // simulate dispatch latency per channel
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            NotificationStatus status = NotificationStatus.newBuilder()
                    .setOrderId(request.getOrderId())
                    .setChannel(channel)
                    .setStatus("SENT")
                    .setTimestamp(System.currentTimeMillis())
                    .build();

            responseObserver.onNext(status);
        }

        responseObserver.onCompleted();
    }
}
