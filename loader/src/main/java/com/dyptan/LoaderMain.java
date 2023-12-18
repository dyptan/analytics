package com.dyptan;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

public class LoaderMain {
    public static int port = 8082;
    final static Logger logger = LoggerFactory.getLogger("Loader");

    public static void main(String[] args) throws IOException, InterruptedException {
    try (MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017"))
    {
        MongoDatabase database = mongoClient.getDatabase("ria");
        MongoCollection<Document>  collection = database.getCollection("autos");
        Server server = ServerBuilder
                .forPort(port)
                .addService(new LoaderServiceImpl(collection) {
                }).build();
        server.start();
        logger.info("RPC server started to listen on port " + port);
        server.awaitTermination();
    }

    }
}
