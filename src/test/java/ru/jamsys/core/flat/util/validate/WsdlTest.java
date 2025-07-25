package ru.jamsys.core.flat.util.validate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.flat.util.UtilFileResource;

class WsdlTest {

    @Test
    public void test() throws Exception {
        try {
            Wsdl.validate(
                    UtilFileResource.get("schema/soap/true-soap.xml", UtilFileResource.Direction.RESOURCE_CORE),
                    UtilFileResource.get("schema/soap/test.wsdl", UtilFileResource.Direction.RESOURCE_CORE),
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
            Wsdl.validate(
                    UtilFileResource.get("schema/soap/false-soap.xml", UtilFileResource.Direction.RESOURCE_CORE),
                    UtilFileResource.get("schema/soap/test.wsdl", UtilFileResource.Direction.RESOURCE_CORE),
                    s -> UtilFileResource.get("schema/xsd/" + s, UtilFileResource.Direction.RESOURCE_CORE)
            );
            Assertions.fail();
        } catch (Exception e) {
            App.error(e);
        }
    }

}