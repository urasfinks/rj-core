package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.JsonSchema;
import ru.jamsys.core.flat.util.UtilFileResource;

// IO time: 139ms
// COMPUTE time: 141ms

class JsonSchemaTest {

    @Test
    void validate() throws Throwable {
        try {
            JsonSchema.validate(UtilFileResource.get("schema/test/2.json"), UtilFileResource.get("schema/test/1-schema.json"), null);
        } catch (Throwable th) {
            th.printStackTrace();
        }

        try {
            JsonSchema.validate(UtilFileResource.get("schema/test/1.json"), UtilFileResource.get("schema/test/1-schema.json"), null);
            Assertions.fail();
        } catch (Throwable th) {
            Assertions.assertEquals("$.code: string found, integer expected", th.getMessage(), "#1");
        }

        // Просто повтор, что статика работает
        try {
            JsonSchema.validate(UtilFileResource.get("schema/test/2.json"), UtilFileResource.get("schema/test/1-schema.json"), null);
        } catch (Throwable th) {
            th.printStackTrace();
        }

        try {
            JsonSchema.validate(UtilFileResource.get("schema/test/1.json"), UtilFileResource.get("schema/test/1-schema.json"), "hello");
            Assertions.fail();
        } catch (Throwable th) {
            Assertions.assertEquals("information: hello; cause: $.code: string found, integer expected", th.getMessage(), "#1");
        }

    }
}