package com.vinodh.grpc;

import com.vinodh.NotificationServiceGrpc;
import com.vinodh.ProcessStatus;
import com.vinodh.SNMPNotification;
import com.vinodh.service.NotificationProcessorService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

//@GrpcService
public class NotificationGrpcServiceBkp extends NotificationServiceGrpc.NotificationServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(NotificationGrpcServiceBkp.class);

    @Autowired
    private NotificationProcessorService processorService;

    @Override
    public StreamObserver<SNMPNotification> streamNotifications(StreamObserver<ProcessStatus> responseObserver) {

        log.info("🚀 gRPC streamNotifications() started — ready to receive SNMP trap notifications...");

        // List to track async tasks
        List<CompletableFuture<Void>> tasks = new CopyOnWriteArrayList<>();

        return new StreamObserver<>() {

            private volatile boolean isClosed = false;

            @Override
            public void onNext(SNMPNotification request) {
                if (isClosed) {
                    log.warn("⚠️ Received message after stream closed, ignoring...");
                    return;
                }

                log.debug("📩 Received SNMP notification: deviceId={}, oid={}, value={}, timestamp={}",
                        request.getDeviceId(), request.getOid(), request.getValue(), request.getTimestamp());

                // Launch async task for Mongo insert
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        if (request.getDeviceId().isBlank()) {
                            throw new IllegalArgumentException("Device ID is required");
                        }

                        if ("1.3.6.1.2.1.22".equals(request.getOid())) {
                            throw new RuntimeException("Simulated database failure");
                        }

                        processorService.processNotification(
                                request.getDeviceId(),
                                request.getOid(),
                                request.getValue(),
                                request.getTimestamp()
                        );

                        log.info("✅ Successfully processed notification for device: {}", request.getDeviceId());

                        synchronized (responseObserver) {
                            responseObserver.onNext(ProcessStatus.newBuilder()
                                    .setSuccess(true)
                                    .setMessage("Processed: " + request.getDeviceId())
                                    .build());
                        }

                    } catch (IllegalArgumentException e) {
                        log.warn("⚠️ Validation failed for deviceId={}, reason={}", request.getDeviceId(), e.getMessage());
                        processorService.handleError(
                                request.getDeviceId(),
                                request.getOid(),
                                request.getValue(),
                                request.getTimestamp(),
                                e.getMessage()
                        );

                        synchronized (responseObserver) {
                            responseObserver.onNext(ProcessStatus.newBuilder()
                                    .setSuccess(false)
                                    .setErrorCode("VALIDATION_ERROR")
                                    .setMessage(e.getMessage())
                                    .build());
                        }

                    } catch (Exception e) {
                        log.error("❌ Internal error while processing deviceId={}: {}", request.getDeviceId(), e.getMessage(), e);
                        processorService.handleError(
                                request.getDeviceId(),
                                request.getOid(),
                                request.getValue(),
                                request.getTimestamp(),
                                "Internal error: " + e.getMessage()
                        );

                        if (!isClosed) {
                            isClosed = true;
                            synchronized (responseObserver) {
                                responseObserver.onError(
                                        Status.INTERNAL
                                                .withDescription("Internal server error: " + e.getMessage())
                                                .asRuntimeException()
                                );
                            }
                        }
                    }
                });

                tasks.add(future);
            }

            @Override
            public void onError(Throwable t) {
                log.error("❌ Client stream error: {}", t.getMessage(), t);
                isClosed = true;
            }

            @Override
            public void onCompleted() {
                log.info("⏳ Client finished sending messages — waiting for async MongoDB tasks to complete...");
                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
                        .whenComplete((v, ex) -> {
                            if (ex != null) {
                                log.error("❌ Error while completing async tasks: {}", ex.getMessage(), ex);
                            }
                            if (!isClosed) {
                                log.info("✅ All SNMP notifications processed successfully. Closing gRPC stream.");
                                responseObserver.onCompleted();
                                isClosed = true;
                            }
                        });
            }
        };
    }
}
