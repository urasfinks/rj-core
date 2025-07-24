package ru.jamsys.core.flat.util.validate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;

import java.io.InputStream;
import java.util.Objects;
import java.util.Set;

//https://bjdash.github.io/JSON-Schema-Builder/

public class JsonSchema {

    public static ObjectMapper objectMapper = new ObjectMapper();

    public static JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    public static void validate(InputStream json, InputStream schema) throws Exception {
        try (
                InputStream inJson = Objects.requireNonNull(json, "json data is null");
                InputStream inSchema = Objects.requireNonNull(schema, "json schema is null")
        ) {
            validate(new String(inJson.readAllBytes()), new String(inSchema.readAllBytes()));
        }
    }

    public static void validate(String json, String schema) throws Exception {
        if (json == null || json.isEmpty()) {
            throw new ForwardException(new HashMapBuilder<String, Object>()
                    .append("json", json)
                    .append("schema", schema)
                    .append("errors", Set.of("json data is empty"))
            );
        }
        if (schema == null || schema.isEmpty()) {
            throw new ForwardException(new HashMapBuilder<String, Object>()
                    .append("json", json)
                    .append("schema", schema)
                    .append("errors", Set.of("json schema is empty"))
            );
        }
        JsonNode jsonObject = objectMapper.readTree(json);

        Set<ValidationMessage> errors = schemaFactory.getSchema(schema).validate(jsonObject);
        if (!errors.isEmpty()) {
            throw new ForwardException(new HashMapBuilder<String, Object>()
                    .append("json", json)
                    .append("schema", schema)
                    .append("errors", errors)
            );
        }
    }

}
