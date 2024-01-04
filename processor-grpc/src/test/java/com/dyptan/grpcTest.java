package com.dyptan;

import com.dyptan.gen.proto.ConfirmationMessage;
import com.dyptan.gen.proto.FilterMessage;
import com.dyptan.gen.proto.ProcessorServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

//@SpringBootTest(properties = {
//        "grpc.client.inProcess.address=processor:50051"
//})
//@DirtiesContext
//@RunWith(SpringRunner.class)
public class grpcTest {

    ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
            .usePlaintext()
            .build();

//    @GrpcClient("inProcess")
    ProcessorServiceGrpc.ProcessorServiceBlockingStub service = ProcessorServiceGrpc.newBlockingStub(channel);
    @Test
    public void greet_shouldReturnGreeting() {
        ConfirmationMessage response = service.archiveMessages(FilterMessage.newBuilder().build());

        // Assert
        assertEquals("Hello, World!", response.getStatus());
    }

}
