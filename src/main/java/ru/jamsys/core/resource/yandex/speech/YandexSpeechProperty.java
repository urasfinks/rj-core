package ru.jamsys.core.resource.yandex.speech;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;
import ru.jamsys.core.extension.annotation.PropertyKey;

@SuppressWarnings({"UnusedDeclaration"})
@FieldNameConstants
@Getter
public class YandexSpeechProperty extends AnnotationPropertyExtractor<Object> {

    @PropertyKey("yandex.speech.kit.host")
    private String host;

    @PropertyKey("yandex.speech.kit.port")
    private Integer port;

    @PropertyKey("yandex.speech.kit.security.alias")
    private String alias;

}
