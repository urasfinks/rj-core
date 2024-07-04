package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.JsonSchema;
import ru.jamsys.core.flat.util.UtilFileResource;


import java.io.IOException;

// IO time: 139ms
// COMPUTE time: 141ms

class JsonSchemaTest {

    @Test
    void validate() throws IOException {
        JsonSchema jsonSchema = new JsonSchema();
        JsonSchema.Result validate = jsonSchema.validate(UtilFileResource.get("test/JsonSchema/1.json"), UtilFileResource.get("test/JsonSchema/1-schema.json"));
        Assertions.assertEquals("$.code: string found, integer expected", validate.getError(), "#1");
    }
}