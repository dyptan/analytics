package com.dyptan.controller;

import com.dyptan.avro.Advertisement;
import com.dyptan.gen.proto.ProcessorServiceGrpc;
import com.dyptan.service.GrpcClientService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.dialect.BooleanDecoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.*;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.HashMap;

import static com.dyptan.utils.JsonToMongoQueryTranslator.convert;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Controller
@EnableConfigurationProperties
@ConfigurationProperties
public class StreamingController {

    Logger log = LogManager.getLogger(StreamingController.class);
    @Autowired
    private ReactiveMongoTemplate mongoTemplate;
    @Autowired
    GrpcClientService client;
    @Value("${fetcher.api.url}")
    String url;
    private Query query = new Query();

    @GetMapping("/stream")
    public String stream() {
        return "streamTable";
    }

    @PostMapping(value = "/stream", consumes = APPLICATION_JSON_VALUE)
    public String stream(@RequestBody String jsonQuery) {
        query = convert(jsonQuery);
        log.info("Mongo query: " + query);
        return "streamTable";
    }

    @GetMapping("/streamfilter")
    public String streamfilter() {
        return "streamFrame";
    }

    @GetMapping("/fetcher")
    @ResponseBody
    public String streamStart() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> bodyValues = new LinkedMultiValueMap<>();
        bodyValues.add("command", "start");
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(bodyValues, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            String responseBody = response.getBody();
            log.info("Processor: " + responseBody);
        } else {
            log.error("Processor start Error: " + response.getStatusCode());
        }
        return response.getBody();
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