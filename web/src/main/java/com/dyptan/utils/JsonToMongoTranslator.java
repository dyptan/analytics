package com.dyptan.utils;

import org.bson.Document;

import java.util.ArrayList;

public class JsonToMongoTranslator {
    public static Document convert(String json) {
        try {
            // Parse JSON
            Document query = new Document();

            // Convert JSON to Document
            Document jsonDocument = Document.parse(json);

            // Map fields to MongoDB query
            mapField(query, jsonDocument, "excludedVendors", "markNameEng");
            mapField(query, jsonDocument, "excludedModels", "modelNameEng");
            mapRangeField(query, jsonDocument, "yearRange", "autoData.year");
            mapRangeField(query, jsonDocument, "priceRange", "USD");
            mapRangeField(query, jsonDocument, "raceRange", "autoData.raceInt");

            return query;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void mapField(Document query, Document jsonDocument, String jsonField, String mongoField) {
        if (jsonDocument.containsKey(jsonField)) {
            Object fieldValue = jsonDocument.get(jsonField);

            if (fieldValue instanceof String) {
                query.put(mongoField, new Document("$ne", fieldValue));
            } else if (fieldValue instanceof ArrayList) {
                query.put(mongoField, new Document("$nin", fieldValue));
            }
        }
    }

    private static void mapRangeField(Document query, Document jsonDocument, String jsonField, String mongoField) {
        if (jsonDocument.containsKey(jsonField)) {
            ArrayList<String> rangeValuesAsString = jsonDocument.get(jsonField, ArrayList.class);

            try {
                int from = Integer.parseInt(rangeValuesAsString.get(0));
                int to = Integer.parseInt(rangeValuesAsString.get(1));

                query.put(mongoField, new Document("$gte", from).append("$lte", to));
            } catch (NumberFormatException e) {
                // Handle the case where parsing to int fails
                System.err.println("Error parsing range values to integers: " + e.getMessage());
            }
        }
    }
}


