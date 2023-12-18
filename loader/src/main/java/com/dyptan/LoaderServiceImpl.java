package com.dyptan;

import com.dyptan.gen.proto.AdvertisementMessage;
import com.dyptan.gen.proto.FilterMessage;
import com.dyptan.gen.proto.LoaderServiceGrpc;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import io.grpc.stub.StreamObserver;
import org.bson.Document;


public class LoaderServiceImpl extends LoaderServiceGrpc.LoaderServiceImplBase {
    MongoCollection<Document> collection;
    public LoaderServiceImpl(MongoCollection<Document> collection) {
        this.collection = collection;
    }
    @Override
    public void getAdvertisements(FilterMessage filterMessage, StreamObserver<AdvertisementMessage> responseObserver) {

        Document simplequery = new Document("markNameEng", filterMessage.getBrands())
                .append("modelNameEng", filterMessage.getModels())
                .append("autoData.year",
                        new Document("$gte", filterMessage.getYearFrom())
                                .append("$lte", filterMessage.getYearTo()))
                .append("autoData.raceInt", new Document("$lte", filterMessage.getRaceTo()));


            FindIterable<Document> advertisements = collection.find(simplequery);
            for (Document ad : advertisements) {
                System.out.println(ad);
                AdvertisementMessage advertisementMessage = AdvertisementMapper.mapToAdvertisementMessage(ad);
                responseObserver.onNext(advertisementMessage);
            }


    }
}
