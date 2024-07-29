package com.dyptan.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Table;

@Embeddable
@Table(name = "Roles")
public class ExportRequest {
    JsonNode query;

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

    JsonNode projection;

    public ExportRequest(){}
}
