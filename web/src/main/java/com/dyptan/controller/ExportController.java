package com.dyptan.controller;

import com.dyptan.service.GrpcClientService;
import com.dyptan.utils.AvroSchemaConverter;
import com.ria.avro.Advertisement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;

import static com.dyptan.utils.SizeEstimator.countBytesOf;
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
    private Query query = new Query();

    @GetMapping("/export")
    public String exportpage() {
        return "export";
    }

//    @GetMapping("/prexport")
//    public String prexportpage() {
//        return "prexport";
//    }

    @GetMapping("/schema")
    @ResponseBody
    public String getSchema() {
        String json = AvroSchemaConverter.schemaToJson(Advertisement.getClassSchema());
        return json;
    }

    @PostMapping(value = "/preexport", consumes = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String prepareExport(@RequestBody String bson, Model model) throws IOException {
        BasicQuery query = new BasicQuery(bson);
//        ObjectMapper mapper = new ObjectMapper();

//        Document document = Document.parse(bson);
//        Criteria criteria = Criteria.fromDocument(doc);

        var count = mongoTemplate.count(query, Advertisement.class).block();
        var one = mongoTemplate.findOne(query, Advertisement.class).block();
        log.info("Mongo query count: " + count);
        log.info("" + one.toString());
        long estimatedSize = count * countBytesOf(one);
        log.info("estimates size: " + estimatedSize);
//        String outputJson = String.format("{" +
//                "'Count of matching documents': %d," +
//                "'Estimated serialised size in MiB': %d" +
//                "}", count, estimatedSize / 1024 / 1024);

        String outputJson = String.format("{" +
                "\"Count of matching documents\": %d," +
                "\"Estimated size in MiB\": %d" +
                "}", count, estimatedSize / 1024 / 1024);
        log.info(outputJson);
        return outputJson;
    }

}