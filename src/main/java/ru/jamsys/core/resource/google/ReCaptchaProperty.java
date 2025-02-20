package ru.jamsys.core.resource.google;

import lombok.Getter;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;
import ru.jamsys.core.extension.annotation.PropertyName;

@SuppressWarnings({"UnusedDeclaration"})
@Getter
public class ReCaptchaProperty extends AnnotationPropertyExtractor {

    @PropertyName("re.captcha.security.alias")
    private String securityAlias;

}
