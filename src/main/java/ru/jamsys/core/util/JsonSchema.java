package ru.jamsys.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;

//https://bjdash.github.io/JSON-Schema-Builder/

public class JsonSchema {

    ObjectMapper objectMapper = new ObjectMapper();
    JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    @SuppressWarnings("unused")
    public Result validate(String json, String schema) {
        return validate(json, schema, StandardCharsets.UTF_8);
    }

    public Result validate(String json, String schema, Charset charset) {
        InputStream jsonStream = new ByteArrayInputStream(json.getBytes(charset));
        InputStream schemaStream = new ByteArrayInputStream(schema.getBytes(charset));
        return validate(jsonStream, schemaStream);
    }

    public Result validate(InputStream jsonStream, InputStream schemaStream) {
        Result result = new Result();
        if (jsonStream == null) {
            result.exception = new RuntimeException("json data is null");
            return result;
        }
        if (schemaStream == null) {
            result.exception = new RuntimeException("json schema is null");
            return result;
        }
        try {
            JsonNode jsonObject = objectMapper.readTree(jsonStream);
            com.networknt.schema.JsonSchema schemaObject = schemaFactory.getSchema(schemaStream);
            result.validationResult = schemaObject.validate(jsonObject);
        } catch (Exception e) {
            result.exception = e;
        }
        return result;
    }

    public static class Result {
        Set<ValidationMessage> validationResult = null;
        Exception exception = null;

        public boolean isValidate() {
            return exception == null && validationResult != null && validationResult.isEmpty();
        }

        public String getError() {
            if (isValidate()) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            if (validationResult != null) {
                validationResult.forEach(vm -> sb.append(vm.getMessage()).append("\n"));
            }
            if (exception != null) {
                sb.append(exception);
            }
            return sb.toString().trim();
        }
    }

}
