package com.dyptan.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Table;

@Embeddable
@Table(name = "ExportRequest")
public class ExportRequest {
    private Filter filter;
    private String collectionName;

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public static class Filter {
        private JsonNode query;
        private JsonNode projection;

        public JsonNode getQuery() {
            return query;
        }

        public void setQuery(JsonNode query) {
            this.query = query;
        }

        public JsonNode getProjection() {
            return projection;
        }

        public void setProjection(JsonNode projection) {
            this.projection = projection;
        }
    }

    public ExportRequest() {}
}
