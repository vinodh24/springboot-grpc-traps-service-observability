package com.vinodh.config;

import com.vinodh.metrics.GrpcMetricsInterceptor;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.micrometer.core.instrument.MeterRegistry;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import net.devh.boot.grpc.server.serverfactory.GrpcServerConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import java.util.concurrent.TimeUnit;

@Configuration
@PropertySource("classpath:grpc.properties")
public class GrpcServerConfig {

    @Value("${grpc.server.keepAliveTime}")
    private long keepAliveTime;

    @Value("${grpc.server.keepAliveTimeout}")
    private long keepAliveTimeout;

    @Value("${grpc.server.permitKeepAliveWithoutCalls}")
    private boolean permitKeepAliveWithoutCalls;

    @Value("${grpc.server.maxConnectionIdle}")
    private long maxConnectionIdle;

    @Value("${grpc.server.maxConnectionAge}")
    private long maxConnectionAge;

    @Value("${grpc.server.maxConnectionAgeGrace}")
    private long maxConnectionAgeGrace;

    @Value("${grpc.server.maxInboundMessageSize}")
    private int maxInboundMessageSize;

    @Value("${grpc.server.maxConcurrentCallsPerConnection}")
    private int maxConcurrentCallsPerConnection;

    @Value("${grpc.server.flowControlWindow}")
    private int flowControlWindow;

    @Bean
    public GrpcServerConfigurer grpcServerConfigurer() {
        return serverBuilder -> {
            if (serverBuilder instanceof NettyServerBuilder netty) {

                // Keep-alive settings
                netty.keepAliveTime(keepAliveTime, TimeUnit.SECONDS)
                        .keepAliveTimeout(keepAliveTimeout, TimeUnit.SECONDS)
                        .permitKeepAliveWithoutCalls(permitKeepAliveWithoutCalls)
                        .maxConnectionIdle(maxConnectionIdle, TimeUnit.SECONDS)
                        .maxConnectionAge(maxConnectionAge, TimeUnit.SECONDS)
                        .maxConnectionAgeGrace(maxConnectionAgeGrace, TimeUnit.SECONDS);

                // Payload and concurrency
                netty.maxInboundMessageSize(maxInboundMessageSize)
                        .maxConcurrentCallsPerConnection(maxConcurrentCallsPerConnection)
                        .flowControlWindow(flowControlWindow);

                // Compression
                netty.compressorRegistry(CompressorRegistry.getDefaultInstance());
                netty.decompressorRegistry(DecompressorRegistry.getDefaultInstance());
            }
        };
    }

    @Bean
    @GrpcGlobalServerInterceptor
    public GrpcMetricsInterceptor grpcMetricsInterceptor(MeterRegistry registry) {
        return new GrpcMetricsInterceptor(registry);
    }
}
