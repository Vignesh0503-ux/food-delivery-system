package com.fooddelivery.order;

import com.fooddelivery.grpc.notification.NotificationServiceGrpc;
import com.fooddelivery.grpc.notification.NotificationStatus;
import com.fooddelivery.grpc.notification.NotifyRequest;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * SERVER STREAMING gRPC client. One request ("notify the customer about
 * this order") produces a stream of NotificationStatus updates back from
 * Notification Service - one per channel (push/sms/email) as it completes.
 */
@Service
public class NotificationStreamClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationStreamClient.class);

    @GrpcClient("notification-service")
    private NotificationServiceGrpc.NotificationServiceStub notificationServiceStub;

    public void requestNotifications(String orderId, String userId, String eventType) {
        NotifyRequest request = NotifyRequest.newBuilder()
                .setOrderId(orderId)
                .setUserId(userId)
                .setEventType(eventType)
                .build();

        notificationServiceStub.subscribeOrderNotifications(request, new StreamObserver<>() {
            @Override
            public void onNext(NotificationStatus status) {
                log.info("[order {}] notification channel={} status={}",
                        status.getOrderId(), status.getChannel(), status.getStatus());
            }

            @Override
            public void onError(Throwable t) {
                log.warn("Notification stream for order {} failed: {}", orderId, t.getMessage());
            }

            @Override
            public void onCompleted() {
                log.info("Notification stream for order {} complete", orderId);
            }
        });
    }
}
