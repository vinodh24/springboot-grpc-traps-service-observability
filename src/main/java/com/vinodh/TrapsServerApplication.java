package com.vinodh;

import com.vinodh.grpc.NotificationGrpcService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.io.IOException;

@SpringBootApplication
@ComponentScan(basePackages = "com.vinodh.*")
@EnableMongoRepositories(basePackages = "com.vinodh.repository")
public class TrapsServerApplication {

    private static final Logger logger = LoggerFactory.getLogger(TrapsServerApplication.class);
    private Server server;

    public static void main(String[] args) {
        SpringApplication.run(TrapsServerApplication.class, args);
    }

    /*@Bean
    public ApplicationRunner grpcServerRunner() {
        return args -> {
            try {

                // Start gRPC server
                server = ServerBuilder.forPort(9095)
                        .addService(new NotificationGrpcService())
                        .build()
                        .start();

                // Confirm server has started
                if (server.isTerminated()) {
                    logger.error("gRPC server failed to start!");
                } else {
                    logger.info("gRPC server started successfully on port 9095");
                }

                // Shutdown hook for graceful shutdown
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    logger.info("Shutting down gRPC server...");
                    if (server != null) {
                        server.shutdown();
                        logger.info("gRPC server stopped.");
                    }
                }));

                // Keep Spring Boot application running
                server.awaitTermination();
            } catch (IOException e) {
                logger.error("Failed to start gRPC server: {}", e.getMessage(), e);
            } catch (InterruptedException e) {
                logger.error("gRPC server interrupted: {}", e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        };
    }

    @Bean
    public ApplicationRunner serverStatusChecker() {
        return args -> {
            if (server != null && !server.isTerminated()) {
                logger.info("Server status check: gRPC server is running.");
            } else {
                logger.warn("Server status check: gRPC server is NOT running.");
            }
        };
    }*/
}
