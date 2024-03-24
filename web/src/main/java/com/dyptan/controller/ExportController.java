package com.dyptan.controller;

import com.dyptan.component.GrpcClientComponent;
import com.dyptan.model.ExportRequest;
import com.dyptan.service.GrpcClientService;
import com.dyptan.utils.AvroSchemaConverter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ria.avro.Advertisement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Controller
@EnableConfigurationProperties
@ConfigurationProperties
public class ExportController {

    Logger log = LogManager.getLogger(ExportController.class);
    @Autowired
    GrpcClientService client;
    @Autowired
    private ReactiveMongoTemplate mongoTemplate;
    //    @Value("${exporter.api.url}")
//    String exporterUrl;
    @Autowired
    GrpcClientComponent exportService;

    @GetMapping("/export")
    public String exportpage() {
        return "export";
    }

    @GetMapping("/schema")
    @ResponseBody
    public String getSchema() {
        String json = AvroSchemaConverter.schemaToJson(Advertisement.getClassSchema());
        return json;
    }

    @PostMapping(value = "/export", consumes = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String doExport(@RequestBody ExportRequest exportRequest) throws IOException {
        JsonNode jsonQuery = exportRequest.getQuery();
        JsonNode jsonProjection = exportRequest.getProjection();
        log.info("select: " + jsonQuery.toPrettyString());
        Document query = Document.parse(jsonQuery.toString());
        Document projection = Document.parse(jsonProjection.toString());

        exportService.exportData(query.toBsonDocument());
        return jsonQuery.toPrettyString();
    }

    @PostMapping(value = "/preexport", consumes = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String prepareExport(@RequestBody ExportRequest exportRequest) throws IOException {
        JsonNode jsonQuery = exportRequest.getQuery();
        JsonNode jsonProjection = exportRequest.getProjection();
        log.info("select: " + jsonQuery.toPrettyString());
        Document query = Document.parse(jsonQuery.toString());
        Document projection = Document.parse(jsonProjection.toString());
        BasicQuery filter = new BasicQuery(query, projection);

        var count = mongoTemplate.count(filter, Advertisement.class).block();
        var one = mongoTemplate.findOne(filter, Advertisement.class).blockOptional();

        String sample = one.map(Advertisement::toString).orElse("No matching documents");
        long estimatedSize = count * sample.getBytes().length;

        Map<String, Object> map = new TreeMap<>();
        map.put("Example data", sample);
        map.put("Count of matching documents", count);
        map.put("Estimated size of one document in bytes", estimatedSize);
        map.put("Estimated total size in MiB", estimatedSize / 1024 / 1024);

        ObjectMapper mapper = new ObjectMapper();
        String outputJson = mapper.writeValueAsString(map);
        log.info(outputJson);
        return outputJson;
    }

}