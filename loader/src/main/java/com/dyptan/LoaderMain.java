package com.dyptan;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class LoaderMain {

    public static int port = 8082;
    final static Logger logger = LoggerFactory.getLogger(ClassLoader.getSystemClassLoader().getName());

    public static void main(String[] args) throws IOException, InterruptedException {
        logger.info("Starting RPC server.");
        Server server = ServerBuilder
                .forPort(port)
                .addService(new LoaderServiceImpl() {
                }).build();
        server.start();

        logger.info("RPC server started to listen on port " + port);
        server.awaitTermination();

    }
}
