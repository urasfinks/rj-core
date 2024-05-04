package ru.jamsys.core.component;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.util.JsonSchema;
import ru.jamsys.core.util.UtilFileResource;


import java.io.IOException;

class JsonSchemaTest {

    @Test
    void validate() throws IOException {
        JsonSchema jsonSchema = new JsonSchema();
        JsonSchema.Result validate = jsonSchema.validate(UtilFileResource.get("test/JsonSchema/1.json"), UtilFileResource.get("test/JsonSchema/1-schema.json"));
        Assertions.assertEquals("$.code: string found, integer expected", validate.getError(), "#1");
    }
}