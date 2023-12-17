package com.dyptan;

import com.dyptan.gen.proto.AdvertisementMessage;
import com.dyptan.gen.proto.FilterMessage;
import com.dyptan.gen.proto.LoaderServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LoaderServiceImpl extends LoaderServiceGrpc.LoaderServiceImplBase {
    final static Logger logger = LoggerFactory.getLogger(LoaderServiceImpl.class.getName());

    @Override
    public void getAdvertisements(FilterMessage request, StreamObserver<AdvertisementMessage> responseObserver) {
        super.getAdvertisements(request, responseObserver);
    }
}
