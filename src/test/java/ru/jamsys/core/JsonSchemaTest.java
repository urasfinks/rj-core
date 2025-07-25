package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.validate.JsonSchema;

// IO time: 139ms
// COMPUTE time: 141ms

class JsonSchemaTest {

    @Test
    void test1() {
        try {
            JsonSchema.validate(UtilFileResource.get("schema/test/2.json"), UtilFileResource.get("schema/test/1-schema.json"));
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    @Test
    void test2() {
        try {
            JsonSchema.validate(UtilFileResource.get("schema/test/1.json"), UtilFileResource.get("schema/test/1-schema.json"));
            Assertions.fail();
        } catch (Throwable th) {
            App.error(th);
            //Assertions.assertEquals("$.code: string found, integer expected", th.getMessage(), "#1");
        }
    }

    @Test
    void test3() {
        // Просто повтор, что статика работает
        try {
            JsonSchema.validate(UtilFileResource.get("schema/test/2.json"), UtilFileResource.get("schema/test/1-schema.json"));
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    @Test
    void test4() {
        try {
            JsonSchema.validate(UtilFileResource.get("schema/test/1.json"), UtilFileResource.get("schema/test/1-schema.json"));
            Assertions.fail();
        } catch (Throwable th) {
            App.error(th);
            //Assertions.assertEquals("$.code: string found, integer expected", th.getMessage(), "#1");
        }
    }

}