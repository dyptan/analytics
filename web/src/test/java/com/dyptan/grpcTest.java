package com.dyptan;

import com.dyptan.gen.proto.ConfirmationMessage;
import com.dyptan.gen.proto.FilterMessage;
import com.dyptan.gen.proto.ProcessorServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class grpcTest {

    ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
            .usePlaintext()
            .build();
    ProcessorServiceGrpc.ProcessorServiceBlockingStub service = ProcessorServiceGrpc.newBlockingStub(channel);
    @Ignore
    @Test
    public void greet_shouldReturnGreeting() {
        ConfirmationMessage response = service.archiveMessages(FilterMessage.newBuilder().build());

        assertEquals("Success1", response.getStatus());
    }

}
