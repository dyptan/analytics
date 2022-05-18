package com.dyptan.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties
@ConfigurationProperties
public class ElasticConfiguration {
        @Value(value="${elasticsearch.type}")
        private String type;
        @Value(value="${elasticsearch.index}")
        private String index;
        @Value(value="${elasticsearch.service.port}")
        private Integer port;
        @Value(value="${elasticsearch.service.host}")
        private String host;


        public ElasticConfiguration() {
        }

        public String getHost() {
                return this.host;
        }

        public String getIndex() {
                return this.index;
        }

        public Integer getPort() {
                return this.port;
        }

        public String getType() {
                return this.type;
        }

        public void setHost(String host) {
                this.host=host;
        }

        public void setIndex(String index) {
                this.index = index;
        }

        public void setPort(int port) {
                this.port=port;
        }

        public void setType(String type) {
                this.type = type;
        }

        public String toString() {
                return "ElasticConfiguration(host=" + this.getHost() + ", index=" + this.getIndex() + ", port=" + this.getPort() + ", type=" + this.getType() + ")";
        }
}
