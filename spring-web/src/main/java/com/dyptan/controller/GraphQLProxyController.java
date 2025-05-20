package com.dyptan.controller;

import com.dyptan.service.ScoutGraphQLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/autoscout")
public class GraphQLProxyController {
    @Autowired
    private ScoutGraphQLService scoutGraphQLService;

    @PostMapping
    public ResponseEntity<String> proxyGraphQL(@RequestBody String body, @RequestHeader HttpHeaders headers) {
        // Here you can add authentication logic if needed
        String response = scoutGraphQLService.forwardQuery(body, headers);
        return ResponseEntity.ok(response);
    }
}
