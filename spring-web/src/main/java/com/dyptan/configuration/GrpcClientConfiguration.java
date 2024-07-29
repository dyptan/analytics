package com.dyptan.configuration;

import com.dyptan.gen.proto.ExportServiceGrpc;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class GrpcClientConfiguration {

    @Bean
    ExportServiceGrpc.ExportServiceBlockingStub loansServiceStub() {
        Channel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();

        return ExportServiceGrpc.newBlockingStub(channel);
    }
}
