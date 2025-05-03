package ru.jamsys.core.resource.google;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;
import ru.jamsys.core.extension.annotation.PropertyKey;

@SuppressWarnings({"UnusedDeclaration"})
@Getter
@FieldNameConstants
public class ReCaptchaRepositoryProperty extends RepositoryPropertyAnnotationField<String> {

    @PropertyKey("re.captcha.security.alias")
    private String securityAlias;

}
