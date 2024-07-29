package com.dyptan.controller;

import com.ria.avro.Advertisement;
import com.dyptan.service.GrpcClientService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
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
        return "streamFilter";
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