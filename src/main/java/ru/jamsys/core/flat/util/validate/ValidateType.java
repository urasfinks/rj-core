package ru.jamsys.core.flat.util.validate;

import ru.jamsys.core.extension.functional.FunctionThrowing;

import java.io.InputStream;

public enum ValidateType implements Validate {
    JSON(new JsonSchema()),
    XSD(new Xsd()),
    WSDL(new Wsdl());

    private final Validate validate;

    ValidateType(Validate validate) {
        this.validate = validate;
    }

    @Override
    public void validate(
            InputStream data,
            InputStream schema,
            FunctionThrowing<String, InputStream> importSchemeResolver
    ) throws Exception {
        validate.validate(data, schema, importSchemeResolver);
    }

    @Override
    public void validate(
            String data,
            String schema,
            FunctionThrowing<String, InputStream> importSchemeResolver
    ) throws Exception {
        validate.validate(data, schema, importSchemeResolver);
    }

}
