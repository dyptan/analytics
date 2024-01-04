package com.dyptan;

import com.dyptan.gen.proto.ConfirmationMessage;
import com.dyptan.gen.proto.FilterMessage;
import com.dyptan.gen.proto.ProcessorServiceGrpc;
import io.grpc.stub.StreamObserver;

public class ProcessorServiceImpl extends ProcessorServiceGrpc.ProcessorServiceImplBase {

    @Override
    public void archiveMessages(FilterMessage request, StreamObserver<ConfirmationMessage> responseObserver) {
        // Implement your logic here to process the FilterMessage and send a ConfirmationMessage
        ConfirmationMessage confirmationMessage = ConfirmationMessage.newBuilder().setStatus("Success").build();
        responseObserver.onNext(confirmationMessage);
        responseObserver.onCompleted();
    }
}
