package ru.jamsys.core.plugin.http.resource.google;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.annotation.PropertyNotNull;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;
import ru.jamsys.core.extension.annotation.PropertyKey;

@SuppressWarnings({"UnusedDeclaration", "all"})
@Getter
@FieldNameConstants
public class ReCaptchaRepositoryProperty extends RepositoryPropertyAnnotationField<String> {

    @PropertyNotNull
    @PropertyKey("security.alias")
    private String securityAlias;

    @PropertyNotNull
    @PropertyKey("captcha.value")
    private String captchaValue;

}
