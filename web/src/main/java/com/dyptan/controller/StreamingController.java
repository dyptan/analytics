package com.dyptan.controller;

import com.dyptan.gen.proto.*;
import com.dyptan.model.Car;
import com.dyptan.model.Filter;
import net.devh.boot.grpc.client.inject.GrpcClient;
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
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;


@Controller
@EnableConfigurationProperties
@ConfigurationProperties
public class StreamingController {

    Logger log = LogManager.getLogger(StreamingController.class);

    private final ReactiveMongoTemplate mongoTemplate;

    @Value
    (value="${trainer.endpoint}")
    private String trainerEndpoint;

    private Query query = new Query();

    public StreamingController(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @GrpcClient("streamerGateway")
    private StreamerGatewayGrpc.StreamerGatewayBlockingStub streamerGatewayBlockingStub;

    @GetMapping("/stream")
    public String stream() {
        return "stream";
    }

    @GetMapping("/streamfilter")
    public String streamfilter() {
        return "streamfilter";
    }

    @PostMapping(value = "/stream", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String stream(@ModelAttribute Filter filter) {
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
    public Flux<Car> filteredstream() {
        return this.mongoTemplate.tail(this.query, Car.class).share();
    }


    @GetMapping("/trainmodel")
    public String model(final Model model) {
        MetadataReply metadataReply = streamerGatewayBlockingStub.modelMetadata(MetadataRequest.newBuilder().build());
        model.addAttribute("currentModelDeployed", metadataReply.getPath());
        return "trainmodel";
    }


    WebClient webclient;

    @PostMapping("/trainmodel")
    public String train(
            @RequestParam(name = "trainingDataSetPath") URL trainingDataSetPath,
            @RequestParam(name = "sourceLimit") int limit,
            @RequestParam(name = "MLiterations") int iterations,
            Model model) {
        webclient = WebClient.create(trainerEndpoint);
        String response = webclient.post().uri(uriBuilder -> uriBuilder
                .path("/train")
                .queryParam("path", trainingDataSetPath.toString())
                .queryParam("iterations", iterations)
                .queryParam("limit", limit)
                .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        model.addAttribute("modelTrained", response);

        return "trainmodel";
        }

        @PostMapping("/savemodel")
        public String save(
                @RequestParam(name = "newmodelname") String newModelName,
                Model model,
                HttpServletRequest request){
        log.info("Save model requested.");
            String response = webclient.post()
                    .uri(uriBuilder -> uriBuilder
                    .queryParam("name", newModelName)
                    .path("/save")
                    .build()
                    )
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();


            request.getSession().setAttribute("newModelName" , newModelName);
            model.addAttribute("modelSaved", response);

            return "trainmodel";
        }


        @PostMapping("/deploymodel")
        public String apply(
                Model model,
                HttpServletRequest request){
            log.info("Apply model received");
            String newModelName = (String) request.getSession().getAttribute("newModelName");
            StatusReply response = streamerGatewayBlockingStub.applyNewModel(ApplyRequest.newBuilder().setPath(newModelName).build());

            model.addAttribute("DeployStatus", response.getStatus());
            model.addAttribute("DeployMessage", response.getMessage());

            return "trainmodel";
        }


}