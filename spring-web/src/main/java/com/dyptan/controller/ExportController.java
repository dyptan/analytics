package com.dyptan.controller;

import com.dyptan.model.ExportRequest;
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
    private ReactiveMongoTemplate mongoTemplate;

    @GetMapping("/export")
    public String exportpage() {
        return "export";
    }

    @ResponseBody
    @GetMapping("/schema")
    public String getSchema() {
        String json = AvroSchemaConverter.schemaToJson(Advertisement.getClassSchema());
        return json;
    }

    @ResponseBody
    @PostMapping(value = "/preview", consumes = APPLICATION_JSON_VALUE)
    public String preview(@RequestBody ExportRequest exportRequest) throws IOException {
        log.info("Received preview request: " + exportRequest);
        JsonNode jsonQuery = exportRequest.getFilter().getQuery();
        JsonNode jsonProjection = exportRequest.getFilter().getProjection();

        if (jsonQuery == null) {
            log.warn("Query is null in the request");
            return "Error: Query cannot be null";
        }

        if (jsonProjection == null) {
            log.warn("Projection is null in the request");
            return "Error: Projection cannot be null";
        }

        log.info("Parsed query: " + jsonQuery.toPrettyString());
        log.info("Parsed projection: " + jsonProjection.toPrettyString());

        Document query = Document.parse(jsonQuery.toString());
        Document projection = Document.parse(jsonProjection.toString());
        BasicQuery filter = new BasicQuery(query, projection);

        log.info("Executing MongoDB query with filter: " + filter);

        var count = mongoTemplate.count(filter, exportRequest.getCollectionName()).block();
        log.info("Count of matching documents: " + count);

        var one = mongoTemplate.findOne(filter, Advertisement.class, exportRequest.getCollectionName()).blockOptional();

        String sample = one.map(Advertisement::toString).orElse("No matching documents");
        log.info("Sample document: " + sample);

        long estimatedSize = count * sample.getBytes().length;
        log.info("Estimated size of one document in bytes: " + sample.getBytes().length);
        log.info("Estimated total size in MiB: " + estimatedSize / 1024 / 1024);

        Map<String, Object> map = new TreeMap<>();
        map.put("Count of matching documents", count);
        map.put("Example data", sample);
        map.put("Estimated size of one document in bytes", sample.getBytes().length);
        map.put("Estimated total size in MiB", estimatedSize / 1024 / 1024);

        ObjectMapper mapper = new ObjectMapper();
        String outputJson = mapper.writeValueAsString(map);
        log.info(outputJson);
        return outputJson;
    }
}