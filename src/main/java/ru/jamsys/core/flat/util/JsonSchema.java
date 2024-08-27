package ru.jamsys.core.flat.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import ru.jamsys.core.extension.exception.JsonSchemaException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;

//https://bjdash.github.io/JSON-Schema-Builder/

public class JsonSchema {

    public static ObjectMapper objectMapper = new ObjectMapper();

    public static JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    public static boolean validate(String json, String schema) throws Exception {
        return validate(json, schema, StandardCharsets.UTF_8);
    }

    public static boolean validate(String json, String schema, Charset charset) throws Exception {
        InputStream jsonStream = new ByteArrayInputStream(json.getBytes(charset));
        InputStream schemaStream = new ByteArrayInputStream(schema.getBytes(charset));
        return validate(jsonStream, schemaStream);
    }

    public static boolean validate(InputStream jsonStream, InputStream schemaStream) throws Exception {
        if (jsonStream == null) {
            throw new RuntimeException("json data is null");
        }
        if (schemaStream == null) {
            throw new RuntimeException("json schema is null");
        }
        JsonNode jsonObject = objectMapper.readTree(jsonStream);

        Set<ValidationMessage> validate = schemaFactory.getSchema(schemaStream).validate(jsonObject);
        if (!validate.isEmpty()) {
            throw new JsonSchemaException(validate);
        }
        return true;
    }

}
