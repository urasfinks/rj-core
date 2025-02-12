package ru.jamsys.core.resource.yandex.speech;

import lombok.Getter;
import ru.jamsys.core.extension.property.repository.RepositoryPropertiesField;
import ru.jamsys.core.extension.annotation.PropertyName;

@SuppressWarnings({"UnusedDeclaration"})
@Getter
public class YandexSpeechProperties extends RepositoryPropertiesField {

    @PropertyName("yandex.speech.kit.host")
    private String host;

    @PropertyName("yandex.speech.kit.port")
    private Integer port;

    @PropertyName("yandex.speech.kit.security.alias")
    private String alias;

}
