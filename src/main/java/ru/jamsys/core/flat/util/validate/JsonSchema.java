package ru.jamsys.core.flat.util.validate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.functional.FunctionThrowing;

import java.io.InputStream;
import java.util.Objects;
import java.util.Set;

//https://bjdash.github.io/JSON-Schema-Builder/

public class JsonSchema implements Validate {

    public static ObjectMapper objectMapper = new ObjectMapper();

    public static JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    public void validate(
            InputStream json,
            InputStream schema,
            FunctionThrowing<String, InputStream> importSchemeResolver
    ) throws Exception {
        try (
                InputStream inJson = Objects.requireNonNull(json, "json data is null");
                InputStream inSchema = Objects.requireNonNull(schema, "json schema is null")
        ) {
            validate(new String(inJson.readAllBytes()), new String(inSchema.readAllBytes()), importSchemeResolver);
        }
    }

    public void validate(
            String json,
            String schema,
            FunctionThrowing<String, InputStream> importSchemeResolver
    ) throws Exception {
        if (json == null || json.isEmpty()) {
            throw new ForwardException("json data is empty", new HashMapBuilder<String, Object>()
                    .append("json", json)
                    .append("schema", schema)
                    .append("errors", Set.of("json data is empty"))
            );
        }
        if (schema == null || schema.isEmpty()) {
            throw new ForwardException("json schema is empty", new HashMapBuilder<String, Object>()
                    .append("json", json)
                    .append("schema", schema)
                    .append("errors", Set.of("json schema is empty"))
            );
        }
        JsonNode jsonObject = objectMapper.readTree(json);

        Set<ValidationMessage> errors = schemaFactory.getSchema(schema).validate(jsonObject);
        if (!errors.isEmpty()) {
            String errorMessage = errors.stream()
                    .map(ValidationMessage::getMessage)
                    .reduce("", (a, b) -> a + "\n" + b);
            throw new ForwardException(errorMessage.trim(), new HashMapBuilder<String, Object>()
                    .append("json", json)
                    .append("schema", schema)
                    .append("errors", errors)
            );
        }
    }

}
