package ru.jamsys.core.resource.google;

import lombok.Getter;
import ru.jamsys.core.extension.property.PropertiesRepository;
import ru.jamsys.core.extension.annotation.PropertyName;

@SuppressWarnings({"UnusedDeclaration"})
@Getter
public class ReCaptchaProperties extends PropertiesRepository {

    @PropertyName("reCaptcha.security.alias")
    private String securityAlias;

}
