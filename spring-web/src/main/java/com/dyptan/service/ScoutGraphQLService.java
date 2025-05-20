package com.dyptan.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ScoutGraphQLService {
    @Value("${SCOUT_GRAPHQL_URL:${scout.graphql.url:http://localhost:8086/graphql}}")
    private String scoutGraphqlUrl;

    public String fetchAllCars() {
        String query = "{ cars { id make model year } }";
        String requestBody = String.format("{\"query\":\"%s\"}", query.replace("\"", "\\\""));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.postForObject(scoutGraphqlUrl, entity, String.class);
    }

    public String forwardQuery(String body, HttpHeaders incomingHeaders) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Optionally forward auth headers
        if (incomingHeaders.containsKey("Authorization")) {
            headers.set("Authorization", incomingHeaders.getFirst("Authorization"));
        }
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.postForObject(scoutGraphqlUrl, entity, String.class);
    }
}
