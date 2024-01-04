package com.dyptan.component;
import com.dyptan.gen.proto.FilterMessage;
import com.dyptan.gen.proto.ProcessorServiceGrpc;
import com.dyptan.gen.proto.ConfirmationMessage;
//import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
public class GrpcClientComponent {

//    @GrpcClient("processorService")
    private ProcessorServiceGrpc.ProcessorServiceBlockingStub blockingStub;

    public void callRemoteService() {
        FilterMessage request = FilterMessage.newBuilder()
                // Set your request fields here
                .build();

        ConfirmationMessage response = blockingStub.archiveMessages(request);
        System.out.println("Response: " + response.getStatus());
    }
}
