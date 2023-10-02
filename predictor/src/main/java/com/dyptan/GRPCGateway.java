package com.dyptan;

import com.dyptan.gen.proto.*;
import io.grpc.stub.StreamObserver;
import org.apache.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Paths;

public class GRPCGateway extends StreamerGatewayGrpc.StreamerGatewayImplBase {
    final static Logger logger = Logger.getLogger(GRPCGateway.class.getName());
    public static StreamTransformer transformer;
    public static Thread detatchedTransformer;

    @Override
    public void applyNewModel(ApplyRequest request, StreamObserver<StatusReply> responseObserver) {
        logger.info("Stopping Streamer with Id: "+transformer.id);
        detatchedTransformer.interrupt();
        logger.info("Streamer with Id: "+transformer.id+" applied new model.");
        StatusReply status;
        if (init(request.getPath())){
            status = StatusReply.newBuilder().setStatus(200).setMessage("New model applied: "+request.getPath()).build();
        } else {
            status = StatusReply.newBuilder().setStatus(404).setMessage("Model does not exist: "+request.getPath()).build();
        }
        responseObserver.onNext(status);
        responseObserver.onCompleted();
    }

    @Override
    public void modelMetadata(MetadataRequest request, StreamObserver<MetadataReply> responseObserver) {
        MetadataReply metadataReply = MetadataReply
                .newBuilder()
                .setOwner("ivan")
                .setPath(transformer.MODEL_NAME)
                .build();
        responseObserver.onNext(metadataReply);
        responseObserver.onCompleted();
    }

    public static boolean init(String newModelName) {
        if (Files.notExists(Paths.get(transformer.MODEL_PATH+newModelName))) {
             logger.error("Model does not exist: "+transformer.MODEL_PATH+newModelName);
             return false;
        }

        transformer = new StreamTransformer(1);
        transformer.applyNewModel(newModelName);

        detatchedTransformer = new Thread(transformer);
        detatchedTransformer.start();

        logger.info("New model deployed: "+transformer.MODEL_PATH+transformer.MODEL_NAME);
        return true;
    }

    //The driver method for Streamer
    public static void init(){
        transformer = new StreamTransformer(1);
        detatchedTransformer = new Thread(transformer);
        detatchedTransformer.start();
    }
}
