package com.dyptan.controller;

import com.dyptan.avro.Advertisement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import static com.dyptan.utils.JsonToMongoQueryTranslator.convert;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


@Controller
@EnableConfigurationProperties
@ConfigurationProperties
public class StreamingController {

    Logger log = LogManager.getLogger(StreamingController.class);
    @Autowired
    private ReactiveMongoTemplate mongoTemplate;
    private Query query = new Query();

    @GetMapping("/stream")
    public String stream() {
        return "stream";
    }

    @PostMapping(value = "/stream", consumes = APPLICATION_JSON_VALUE)
    public String stream(@RequestBody String jsonQuery) {
        query = convert(jsonQuery);
        log.info("Mongo query: " + query);
        return "stream";
    }

    @GetMapping("/streamfilter")
    public String streamfilter() {
        return "streamfilter";
    }

    @GetMapping(
            value = {"/mongostream"},
            produces = {"text/event-stream"}
    )
    @ResponseBody
    public Flux<String> filteredstream() {
        return mongoTemplate.tail(this.query, Advertisement.class).map(e -> e.toString());
    }

}