package ru.jamsys.core.promise;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;

@SuppressWarnings({"all"})
@FieldNameConstants
@Getter
public class PromiseGeneratorExternalRequestRepositoryProperty extends RepositoryPropertyAnnotationField<Object> {

    @PropertyKey("auth")
    private Boolean auth = false;

    @PropertyKey("users")
    private String users = "";

    @PropertyKey("validation.type") // [JSON|XSD|WSDL]
    private String validationType = "";

    // Файлы схем должны лежать в /web/scheme/${classNamePromiseGenerator}/${имя схемы}
    @PropertyKey("validation.scheme") // file.xsd
    private String validationScheme = "";

    // validation.import не используется для json валидации
    // Автоматика import из папки classNamePromiseGenerator
//    @PropertyKey("validation.import") // file2.xsd, file3.xsd
//    private String validationImport = "";

}
