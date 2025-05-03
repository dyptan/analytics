package com.dyptan.utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ria.avro.Advertisement;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;

import java.util.List;
import java.util.stream.Collectors;

public class AvroSchemaConverter {
    public static String schemaToJson(Schema schema) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        schema.getFields().forEach(f -> recHelper(f, mapper, rootNode));
        return rootNode.toString();
    }

    private static ObjectNode recHelper(Field field, ObjectMapper mapper, ObjectNode output) {
        switch (field.schema().getType()) {
            case RECORD:
                ObjectNode node = mapper.createObjectNode();
                field.schema().getFields().stream()
                        .map(f -> recHelper(f, mapper, node))
                        .collect(Collectors.toList());
                output.set(field.name(), node);
                break;
            case UNION:
                output.put(field.name(), field.schema().getTypes().get(1).getType().getName());
                break;
            default:
                output.put(field.name(), field.schema().getType().getName());
                break;
        }
        return output;
    }

    public static void main(String[] args) {
        String jsonSchema = schemaToJson(Advertisement.getClassSchema());
        System.out.println(jsonSchema);
    }
}
