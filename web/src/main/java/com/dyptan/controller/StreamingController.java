package com.dyptan.controller;

import com.dyptan.gen.proto.AdvertisementMessage;
import com.dyptan.gen.proto.FilterMessage;
import com.dyptan.gen.proto.LoaderServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.Iterator;


@Controller
@EnableConfigurationProperties
@ConfigurationProperties
public class StreamingController {

    Logger log = LogManager.getLogger(StreamingController.class);

    public static void main(String[] args) {
        // Create a channel to connect to the gRPC server
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 8080)  // Replace with your server's address and port
                .usePlaintext()  // Use plaintext (non-secure) communication for simplicity
                .build();

        // Create a gRPC client using the channel
        LoaderServiceGrpc.LoaderServiceBlockingStub blockingStub = LoaderServiceGrpc.newBlockingStub(channel);

        // Create a request
        FilterMessage request = FilterMessage.newBuilder()
                // Set request parameters
                .build();

        // Make an RPC call and get the response
        Iterator<AdvertisementMessage> response = blockingStub.getAdvertisements(request);

        // Process the response
        System.out.println("Response received: " + response);

        // Shutdown the channel when done
        channel.shutdown();
    }
    private final ReactiveMongoTemplate mongoTemplate;

    @Value
    (value="${trainer.endpoint}")
    private String trainerEndpoint;

    private Query query = new Query();

    public StreamingController(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping("/stream")
    public String stream() {
        return "stream";
    }

    @GetMapping("/streamfilter")
    public String streamfilter() {
        return "streamfilter";
    }

    @PostMapping(value = "/stream", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String stream(@ModelAttribute FilterMessage filter) {
        this.query = new Query();
        this.query.addCriteria(Criteria.where("category").not().in(
                // splits the string on a delimiter defined as:
                // zero or more whitespace, a literal comma, zero or more whitespace
                Arrays.asList(filter.getBrands().split("\\s*,\\s*")
                )
                )
        );

        this.query.addCriteria(Criteria.where("model").not().in(
                // splits the string on a delimiter defined as:
                // zero or more whitespace, a literal comma, zero or more whitespace
                Arrays.asList(filter.getModels().split("\\s*,\\s*")
                )
                )
        );

        this.query.addCriteria(
                Criteria.where("year").gte(filter.getYearFrom()).lte(filter.getYearTo())

        );

        this.query.addCriteria(
                Criteria.where("price_usd")
                        .gte(filter.getPriceFrom())
                        .lte(filter.getPriceTo())
        );

        log.info("Mongo query: " + this.query.toString());

        return "stream";
    }

    @GetMapping(
            value = {"/mongostream"},
            produces = {"text/event-stream"}
    )
    public Flux<AdvertisementMessage> filteredstream() {
        return this.mongoTemplate.tail(this.query, AdvertisementMessage.class).share();
    }


    WebClient webclient;

}