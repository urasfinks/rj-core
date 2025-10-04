package ru.jamsys.core.plugin.telegram;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.annotation.PropertyDescription;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.annotation.PropertyNotNull;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;

@SuppressWarnings({"UnusedDeclaration", "all"})
@FieldNameConstants
@Getter
public class BotRepositoryProperty extends RepositoryPropertyAnnotationField<Object> {

    @PropertyKey("name")
    @PropertyDescription("Имя telegram бота")
    @PropertyNotNull
    private String name;

    @PropertyKey("security.alias")
    @PropertyDescription("SecurityAlias для токена")
    @PropertyNotNull
    private String securityAlias;

    @PropertyKey("cache.file.upload.timeout.ms")
    @PropertyDescription("Время жизни id файла для переиспользования. Используется при массовых рассылках")
    @PropertyNotNull
    private Integer cacheFileUploadTimeoutMs = 600_000;

    @PropertyKey("promise.generator.class")
    @PropertyDescription("Маркер интерфейс для PromiseGenerator")
    private String promiseGeneratorClass;

    // Пример: /ask_question/?question=
    @PropertyKey("default.url")
    @PropertyDescription("Url по умолчанию, если клиент пишет без команды")
    private String defaultUrl;

}
