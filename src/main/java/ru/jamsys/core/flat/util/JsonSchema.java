package ru.jamsys.core.flat.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import ru.jamsys.core.extension.exception.JsonSchemaException;

import java.io.InputStream;
import java.util.Set;

//https://bjdash.github.io/JSON-Schema-Builder/

public class JsonSchema {

    public static ObjectMapper objectMapper = new ObjectMapper();

    public static JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    public static void validate(String json, String schema, String information) throws Exception {
        if (json == null || json.isEmpty()) {
            throw new RuntimeException("json data is empty");
        }
        if (schema == null || schema.isEmpty()) {
            throw new RuntimeException("json schema is empty");
        }
        JsonNode jsonObject = objectMapper.readTree(json);

        Set<ValidationMessage> validate = schemaFactory.getSchema(schema).validate(jsonObject);
        if (!validate.isEmpty()) {
            throw new JsonSchemaException(validate, information, json, schema);
        }
    }

    public static void validate(InputStream json, InputStream schema, String information) throws Exception {
        if (json == null) {
            throw new RuntimeException("json data is null");
        }
        if (schema == null) {
            throw new RuntimeException("json schema is null");
        }
        validate(new String(json.readAllBytes()), new String(schema.readAllBytes()), information);
    }

}
