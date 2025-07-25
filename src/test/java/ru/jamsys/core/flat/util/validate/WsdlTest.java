package ru.jamsys.core.flat.util.validate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.flat.util.UtilFileResource;

import java.io.IOException;

class WsdlTest {

    @Test
    public void test() throws Exception {
        try {
            Wsdl.validate(
                    UtilFileResource.get("schema/xsd/true-soap.xml", UtilFileResource.Direction.RESOURCE_CORE),
                    UtilFileResource.get("schema/xsd/test.wsdl", UtilFileResource.Direction.RESOURCE_CORE),
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
        }
    }

    @Test
    public void testFail() {
        try {
            Wsdl.validate(
                    UtilFileResource.get("schema/xsd/false-soap.xml", UtilFileResource.Direction.RESOURCE_CORE),
                    UtilFileResource.get("schema/xsd/test.wsdl", UtilFileResource.Direction.RESOURCE_CORE),
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