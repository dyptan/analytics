package com.dyptan.connector;

import com.dyptan.configuration.ElasticConfiguration;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class SearchConnector {
    @Autowired
    private ElasticConfiguration config;
    private String host;
    private RestHighLevelClient client;

    public SearchConnector() {
    }

    @PostConstruct
    public void initClient() {
        this.host = config.getHost();
        this.client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(host, config.getPort(), "http")));
    }

    public ElasticConfiguration getConfig() {
        return this.config;
    }

    public String getHost() {
        return this.host;
    }

    public RestHighLevelClient getClient() {
        return this.client;
    }

    public void setConfig(ElasticConfiguration config) {
        this.config = config;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setClient(RestHighLevelClient client) {
        this.client = client;
    }

    public String toString() {
        return "SearchConnector(config=" + this.getConfig() + ", host=" + this.getHost() + ", client=" + this.getClient() + ")";
    }
}
