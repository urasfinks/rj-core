package ru.jamsys.core.resource.google;

import lombok.Getter;
import ru.jamsys.core.extension.property.repository.RepositoryPropertiesField;
import ru.jamsys.core.extension.annotation.PropertyName;

@SuppressWarnings({"UnusedDeclaration"})
@Getter
public class ReCaptchaProperties extends RepositoryPropertiesField {

    @PropertyName("re.captcha.security.alias")
    private String securityAlias;

}
