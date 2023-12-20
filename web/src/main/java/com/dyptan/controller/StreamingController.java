package com.dyptan.controller;

import com.dyptan.avro.Advertisement;
import com.dyptan.model.AdvertisementMongo;
import com.dyptan.service.AdvertisementService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;

import static com.dyptan.utils.JsonToMongoTranslator.convert;


@Controller
@EnableConfigurationProperties
@ConfigurationProperties
public class StreamingController {

    Logger log = LogManager.getLogger(StreamingController.class);
    private final ReactiveMongoTemplate mongoTemplate;
    @Autowired
    private AdvertisementService advertisementService;
    private Query query;
    public StreamingController(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping("/batch")
    public String batch() {
        return "batch";
    }

    @GetMapping("/stream")
    public String stream() {
        return "stream";
    }

    @GetMapping("/streamfilter")
    public String streamfilter() {
        return "streamfilter";
    }

    @PostMapping(value = "/stream", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String stream(@RequestBody String jsonQuery) {
        Document query = convert(jsonQuery);
        log.info("Mongo query: " + query);
        List<AdvertisementMongo> result = advertisementService.findAdvertisementsByQuery(query);
        log.info("Mongo result: " + result);
//        return ResponseEntity.ok(result.toString());
        return "stream";
    }

    @GetMapping(
            value = {"/mongostream"},
            produces = {"text/event-stream"}
    )
    @ResponseBody
    public Flux<Advertisement> filteredstream() {
        Flux<Advertisement> share = this.mongoTemplate.tail(this.query, Advertisement.class).share();
        return share;
    }


    WebClient webclient;

}