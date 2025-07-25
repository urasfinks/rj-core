package ru.jamsys.core.flat.util.validate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.flat.util.UtilFileResource;

// IO time: 139ms
// COMPUTE time: 141ms

class JsonSchemaTest {

    @Test
    void test1() {
        try {
            ValidateType.JSON.validate(
                    UtilFileResource.get("schema/test/2.json"),
                    UtilFileResource.get("schema/test/1-schema.json"),
                    null
            );
        } catch (Throwable th) {
            th.printStackTrace();
            Assertions.fail();
        }
    }

    @Test
    void test2() {
        try {
            ValidateType.JSON.validate(
                    UtilFileResource.get("schema/test/1.json"),
                    UtilFileResource.get("schema/test/1-schema.json"),
                    null
            );
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
            ValidateType.JSON.validate(
                    UtilFileResource.get("schema/test/2.json"),
                    UtilFileResource.get("schema/test/1-schema.json"),
                    null
            );
        } catch (Throwable th) {
            th.printStackTrace();
            Assertions.fail();
        }
    }

    @Test
    void test4() {
        try {
            ValidateType.JSON.validate(
                    UtilFileResource.get("schema/test/1.json"),
                    UtilFileResource.get("schema/test/1-schema.json"),
                    null
            );
            Assertions.fail();
        } catch (Throwable th) {
            App.error(th);
            //Assertions.assertEquals("$.code: string found, integer expected", th.getMessage(), "#1");
        }
    }

}