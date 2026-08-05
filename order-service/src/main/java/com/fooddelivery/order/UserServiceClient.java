package com.fooddelivery.order;

import com.fooddelivery.grpc.user.GetUserRequest;
import com.fooddelivery.grpc.user.UserResponse;
import com.fooddelivery.grpc.user.UserServiceGrpc;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * UNARY gRPC client. One request, one response - used to validate the
 * customer exists and is active before an order is accepted.
 */
@Service
public class UserServiceClient {

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    public Optional<UserResponse> fetchUser(String userId) {
        try {
            GetUserRequest request = GetUserRequest.newBuilder()
                    .setUserId(userId)
                    .build();
            return Optional.of(userServiceStub.getUser(request));
        } catch (StatusRuntimeException e) {
            return Optional.empty();
        }
    }
}
