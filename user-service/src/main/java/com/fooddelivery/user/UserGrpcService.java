package com.fooddelivery.user;

import com.fooddelivery.grpc.user.GetUserRequest;
import com.fooddelivery.grpc.user.UserResponse;
import com.fooddelivery.grpc.user.UserServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * UNARY gRPC endpoint. Order Service calls this once per order to fetch
 * / validate the customer placing the order.
 */
@GrpcService
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserStore userStore;

    public UserGrpcService(UserStore userStore) {
        this.userStore = userStore;
    }

    @Override
    public void getUser(GetUserRequest request, StreamObserver<UserResponse> responseObserver) {
        UserStore.User user = userStore.find(request.getUserId());

        if (user == null) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("No user found with id " + request.getUserId())
                    .asRuntimeException());
            return;
        }

        UserResponse response = UserResponse.newBuilder()
                .setUserId(user.id())
                .setName(user.name())
                .setPhone(user.phone())
                .setAddress(user.address())
                .setActive(user.active())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
