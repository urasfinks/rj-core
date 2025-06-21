package ru.jamsys.core.resource.yandex.speech;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;
import ru.jamsys.core.extension.annotation.PropertyKey;

@SuppressWarnings({"UnusedDeclaration"})
@FieldNameConstants
@Getter
public class YandexSpeechRepositoryProperty extends RepositoryPropertyAnnotationField<Object> {

    @PropertyKey("host")
    private String host;

    @PropertyKey("port")
    private Integer port;

    @PropertyKey("security.alias")
    private String alias;

}
