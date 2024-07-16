package ru.jamsys.core.resource.google;

import lombok.Getter;
import ru.jamsys.core.extension.property.PropertyConnector;
import ru.jamsys.core.extension.annotation.PropertyName;

@SuppressWarnings({"UnusedDeclaration"})
@Getter
public class ReCaptchaProperty extends PropertyConnector {

    @PropertyName("reCaptcha.security.alias")
    private String securityAlias;

}
