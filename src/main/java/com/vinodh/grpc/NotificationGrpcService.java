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
import net.devh.boot.grpc.server.service.GrpcService;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * High-performance gRPC server implementation using Java Virtual Threads (JDK 21+).
 * Handles massive SNMP notification streams efficiently by processing each request
 * concurrently without blocking OS threads.
 */
@GrpcService
public class NotificationGrpcService extends NotificationServiceGrpc.NotificationServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(NotificationGrpcService.class);

    @Autowired
    private NotificationProcessorService processorService;

    // Virtual thread executor (lightweight concurrency for I/O-bound workloads)
    private static final ExecutorService VIRTUAL_EXECUTOR =
            Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("grpc-vt-", 0).factory());

    private static final ExecutorService VIRTUAL_THREAD_EXECUTOR =
            Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public StreamObserver<SNMPNotification> streamNotifications(StreamObserver<ProcessStatus> responseObserver) {

        log.info("🚀 gRPC streamNotifications() started — using Virtual Threads for concurrent processing.");

        List<CompletableFuture<Void>> tasks = new CopyOnWriteArrayList<>();

        return new StreamObserver<>() {
            private volatile boolean isClosed = false;

            @Override
            public void onNext(SNMPNotification request) {
                if (isClosed) {
                    log.warn("⚠️ Received message after stream closed, ignoring...");
                    return;
                }

                log.debug("📩 Incoming SNMP notification: deviceId={}, oid={}, value={}, timestamp={}",
                        request.getDeviceId(), request.getOid(), request.getValue(), request.getTimestamp());

                // Each request handled by a lightweight virtual thread
                CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
                    try {

                        // Get and log the virtual thread's name
                        String threadName = Thread.currentThread().getName();
                        log.info("🔧 Processing SNMP notification on thread: {}", threadName);

                        // Validation
                        if (request.getDeviceId().isBlank()) {
                            throw new IllegalArgumentException("Device ID is required");
                        }

                        // Simulate controlled error scenario
                        if ("1.3.6.1.2.1.22".equals(request.getOid())) {
                            throw new RuntimeException("Simulated database failure");
                        }

                        // Actual persistence
                        processorService.processNotification(
                                request.getDeviceId(),
                                request.getOid(),
                                request.getValue(),
                                request.getTimestamp()
                        );

                        log.info("✅ [VT] Processed notification for deviceId={}", request.getDeviceId());

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
                        log.error("❌ Internal error for deviceId={}: {}", request.getDeviceId(), e.getMessage(), e);
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
                                responseObserver.onError(Status.INTERNAL
                                        .withDescription("Internal server error: " + e.getMessage())
                                        .asRuntimeException());
                            }
                        }
                    }
                }, VIRTUAL_EXECUTOR);

                tasks.add(task);
            }

            @Override
            public void onError(Throwable t) {
                log.error("❌ Client stream error: {}", t.getMessage(), t);
                isClosed = true;
            }

            @Override
            public void onCompleted() {
                log.info("⏳ Client finished sending — waiting for all virtual threads to complete...");
                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
                        .whenComplete((v, ex) -> {
                            if (ex != null) {
                                log.error("❌ Error completing virtual thread tasks: {}", ex.getMessage(), ex);
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
