package com.dyptan;

import com.dyptan.gen.proto.ExportAck;
import com.dyptan.gen.proto.ExportRequest;
import com.dyptan.gen.proto.ExportServiceGrpc;
import io.grpc.stub.StreamObserver;

public class ExportServiceImpl extends ExportServiceGrpc.ExportServiceImplBase {

    @Override
    public void exportData(ExportRequest request, StreamObserver<ExportAck> responseObserver) {
        ExportAck confirmationMessage = ExportAck.newBuilder().setStatus("Success").build();
        responseObserver.onNext(confirmationMessage);
        responseObserver.onCompleted();
    }
}
