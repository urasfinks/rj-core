package ru.jamsys.core.flat.util.validate;

import ru.jamsys.core.extension.functional.FunctionThrowing;

import java.io.InputStream;

public interface Validate {

    void validate(
            InputStream data,
            InputStream schema,
            FunctionThrowing<String, InputStream> importSchemeResolver
    ) throws Exception;

    void validate(
            String data,
            String schema,
            FunctionThrowing<String, InputStream> importSchemeResolver
    ) throws Exception;

}
