package ru.jamsys.core.resource.notification.yandex.speech;

import lombok.Getter;
import ru.jamsys.core.extension.property.PropertiesRepository;
import ru.jamsys.core.extension.annotation.PropertyName;

@SuppressWarnings({"UnusedDeclaration"})
@Getter
public class YandexSpeechNotificationProperties extends PropertiesRepository {

    @PropertyName("yandex.speech.kit.host")
    String host = null;

    @PropertyName("yandex.speech.kit.port")
    String port = null;

    @PropertyName("yandex.speech.kit.security.alias")
    String alias = null;

}