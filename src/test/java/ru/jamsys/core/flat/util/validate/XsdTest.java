package ru.jamsys.core.flat.util.validate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.flat.util.UtilFileResource;

import java.io.IOException;

class XsdTest {

    @Test
    public void test() throws Exception {
        try {
            Xsd.validate(
                    UtilFileResource.get("schema/xsd/true-shiporder.xml", UtilFileResource.Direction.RESOURCE_CORE),
                    UtilFileResource.get("schema/xsd/test.xsd", UtilFileResource.Direction.RESOURCE_CORE),
                    _ -> {
                        try {
                            return UtilFileResource.get("schema/xsd/common.xsd", UtilFileResource.Direction.RESOURCE_CORE);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
            );
        } catch (Exception e) {
            App.error(e);
            Assertions.fail();
        }
    }

    @Test
    public void testFail() {
        try {
            Xsd.validate(
                    UtilFileResource.get("schema/xsd/false-shiporder.xml", UtilFileResource.Direction.RESOURCE_CORE),
                    UtilFileResource.get("schema/xsd/test.xsd", UtilFileResource.Direction.RESOURCE_CORE),
                    _ -> {
                        try {
                            return UtilFileResource.get("schema/xsd/common.xsd", UtilFileResource.Direction.RESOURCE_CORE);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
            );
            Assertions.fail();
        } catch (Exception e) {
            App.error(e);
        }
    }

}