package com.dyptan.utils;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.bson.Document;

import java.util.ArrayList;

public class JsonToMongoQueryTranslator {
        public static Query convert(String json) {
            try {
                Query query = new Query();

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

        private static void mapField(Query query, Document jsonDocument, String jsonField, String mongoField) {
            if (jsonDocument.containsKey(jsonField)) {
                Object fieldValue = jsonDocument.get(jsonField);

                if (fieldValue instanceof String) {
                    query.addCriteria(Criteria.where(mongoField).ne(fieldValue));
                } else if (fieldValue instanceof ArrayList) {
                    query.addCriteria(Criteria.where(mongoField).nin((ArrayList<?>) fieldValue));
                }
            }
        }

        private static void mapRangeField(Query query, Document jsonDocument, String jsonField, String mongoField) {
            if (jsonDocument.containsKey(jsonField)) {
                ArrayList<String> rangeValuesAsString = jsonDocument.get(jsonField, ArrayList.class);

                try {
                    int from = Integer.parseInt(rangeValuesAsString.get(0));
                    int to = Integer.parseInt(rangeValuesAsString.get(1));

                    query.addCriteria(Criteria.where(mongoField).gte(from).lte(to));
                } catch (NumberFormatException e) {
                    // Handle the case where parsing to int fails
                    System.err.println("Error parsing range values to integers: " + e.getMessage());
                }
            }
        }
    }
