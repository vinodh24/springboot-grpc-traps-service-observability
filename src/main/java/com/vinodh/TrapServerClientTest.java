package com.vinodh;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

public class TrapServerClientTest {

    public static void main(String[] args) throws InterruptedException {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 9090)
                .usePlaintext()
                .build();

        NotificationServiceGrpc.NotificationServiceStub asyncStub =
                NotificationServiceGrpc.newStub(channel);

        NotificationServiceGrpc.NotificationServiceBlockingStub blockedStub =
                NotificationServiceGrpc.newBlockingStub(channel);

        StreamObserver<ProcessStatus> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(ProcessStatus status) {
                if (status.getSuccess()) {
                    System.out.println("✅ Success: " + status.getMessage());
                } else {
                    System.err.println("⚠️  Failed: " + status.getMessage() +
                            " (Code: " + status.getErrorCode() + ")");
                }
            }

            @Override
            public void onError(Throwable t) {
                if (t instanceof StatusRuntimeException e) {
                    System.err.println("❌ gRPC Error: " + e.getStatus().getCode() +
                            " - " + e.getStatus().getDescription());
                } else {
                    System.err.println("❌ Unknown Error: " + t.getMessage());
                }
            }

            @Override
            public void onCompleted() {
                System.out.println("Stream completed.");
            }
        };

        // Open client streaming
        StreamObserver<SNMPNotification> requestObserver = asyncStub.streamNotifications(responseObserver);

        try {
            // Send valid notification
            requestObserver.onNext(SNMPNotification.newBuilder()
                    .setDeviceId("Router-1")
                    .setOid("1.3.6.1.2.1")
                    .setValue("Up")
                    .setTimestamp(String.valueOf(System.currentTimeMillis()))
                    .build());

            // ❌ Invalid notification (missing deviceId)
            requestObserver.onNext(SNMPNotification.newBuilder()
                    .setDeviceId("")
                    .setOid("1.3.6.1.4.1.999")
                    .setValue("Temp=99")
                    .setTimestamp("2025-11-04T22:01:00Z")
                    .build());

            // Send invalid notification (trigger validation error)
            requestObserver.onNext(SNMPNotification.newBuilder()
                    .setDeviceId("Router-2") // empty id
                    .setOid("1.3.6.1.2.1.22")
                    .setValue("Down")
                    .setTimestamp(String.valueOf(System.currentTimeMillis()))
                    .build());

            // Finish stream
            requestObserver.onCompleted();

        } catch (Exception e) {
            requestObserver.onError(e);
        }

        Thread.sleep(2000);
        channel.shutdown();
    }
}
