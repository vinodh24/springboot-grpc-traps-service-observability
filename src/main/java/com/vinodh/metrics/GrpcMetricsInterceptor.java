package com.vinodh.metrics;

import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import com.google.protobuf.Message;
import java.nio.charset.StandardCharsets;

@Component
public class GrpcMetricsInterceptor implements ServerInterceptor {

    private final DistributionSummary requestSize;
    private final DistributionSummary responseSize;

    public GrpcMetricsInterceptor(MeterRegistry registry) {
        this.requestSize = DistributionSummary.builder("grpc_server_request_size_bytes")
                .baseUnit("bytes")
                .description("Size of incoming gRPC request messages")
                .publishPercentileHistogram()
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        this.responseSize = DistributionSummary.builder("grpc_server_response_size_bytes")
                .baseUnit("bytes")
                .description("Size of outgoing gRPC response messages")
                .publishPercentileHistogram()
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        // Wrap ServerCall to intercept outgoing responses
        ServerCall<ReqT, RespT> monitoringCall = new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
            @Override
            public void sendMessage(RespT message) {
                int size = getMessageSize(message);
                if (size > 0) {
                    responseSize.record(size);
                }
                super.sendMessage(message);
            }
        };

        // Start the call with wrapped ServerCall
        ServerCall.Listener<ReqT> listener = next.startCall(monitoringCall, headers);

        // Wrap the listener to intercept incoming requests
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(listener) {
            @Override
            public void onMessage(ReqT message) {
                int size = getMessageSize(message);
                if (size > 0) {
                    requestSize.record(size);
                }
                super.onMessage(message);
            }
        };
    }

    // Helper to compute message byte length safely
    private int getMessageSize(Object message) {
        if (message == null) {
            return 0;
        }
        // If it's a protobuf Message, use toByteArray for accurate size
        if (message instanceof Message) {
            return ((Message) message).toByteArray().length;
        }
        // Fallback: use UTF-8 representation length
        try {
            return message.toString().getBytes(StandardCharsets.UTF_8).length;
        } catch (Exception e) {
            return 0;
        }
    }
}
