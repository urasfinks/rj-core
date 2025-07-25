package ru.jamsys.core.flat.util.validate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.flat.util.UtilFileResource;

class XsdTest {

    @Test
    public void test() throws Exception {
        try {
            Xsd.validate(
                    UtilFileResource.get("schema/xsd/true-xsd.xml", UtilFileResource.Direction.RESOURCE_CORE),
                    UtilFileResource.get("schema/xsd/test.xsd", UtilFileResource.Direction.RESOURCE_CORE),
                    s -> UtilFileResource.get("schema/xsd/" + s, UtilFileResource.Direction.RESOURCE_CORE)
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
                    UtilFileResource.get("schema/xsd/false-xsd.xml", UtilFileResource.Direction.RESOURCE_CORE),
                    UtilFileResource.get("schema/xsd/test.xsd", UtilFileResource.Direction.RESOURCE_CORE),
                    s -> UtilFileResource.get("schema/xsd/" + s, UtilFileResource.Direction.RESOURCE_CORE)
            );
            Assertions.fail();
        } catch (Exception e) {
            App.error(e);
        }
    }

}