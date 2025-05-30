package ru.jamsys.core.extension.property.repository;

import lombok.Getter;
import ru.jamsys.core.extension.annotation.PropertyDescription;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.annotation.PropertyNotNull;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.PropertyEnvelope;
import ru.jamsys.core.flat.util.UtilJson;

import java.lang.reflect.Field;

// Репозиторий Property с автоматическим извлечением свойств у класса родителя через аннотации
// @FieldNameConstants для примера, что бы удобно скопировать имена Fields, а так оно тут не работает, его надо
// для всех наследников аннотировать

@Getter
public class RepositoryPropertyAnnotationField<T> extends AbstractRepositoryProperty<T> {

    @Override
    public void init(PropertyDispatcher<T> propertyDispatcher) {
        if (getInit().compareAndSet(false, true)) {
            for (Field field : getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(PropertyKey.class)) {
                    try {
                        // Может такое быть, что value = "", это значит что мы смотрим прямо на корневое значение ns
                        field.setAccessible(true);
                        @SuppressWarnings("unchecked")
                        Class<T> cls = (Class<T>) field.getType();
                        @SuppressWarnings("unchecked")
                        T fieldValue = (T) field.get(this);
                        String repositoryPropertyKey = field.getAnnotation(PropertyKey.class).value();
                        PropertyEnvelope<T> tPropertyEnvelope = new PropertyEnvelope<>(
                                field,
                                cls,
                                field.getName(),
                                repositoryPropertyKey,
                                fieldValue,
                                field.isAnnotationPresent(PropertyDescription.class)
                                        ? field.getAnnotation(PropertyDescription.class).value()
                                        : null,
                                field.isAnnotationPresent(PropertyNotNull.class)
                        )
                                .setServiceProperty(propertyDispatcher.getServiceProperty())
                                .setPropertyKey(propertyDispatcher.getPropertyKey(repositoryPropertyKey))
                                .setRepositoryProperty(this)
                                .syncPropertyValue();
                        fill(tPropertyEnvelope);
                        getListPropertyEnvelopeRepository().add(tPropertyEnvelope);
                    } catch (Throwable th) {
                        throw new ForwardException(th);
                    }
                }
            }
        }
    }

    private void fill(PropertyEnvelope<T> propertyEnvelope) {
        try {
            propertyEnvelope.getField().set(this, propertyEnvelope.getValue());
        } catch (Throwable th) {
            throw new RuntimeException(UtilJson.toStringPretty(propertyEnvelope, "--"), th);
        }
    }

    // Приходит уведомление, что обновилось значение по ключу репозитория
    // Значения нельзя устанавливать самостоятельно в PropertyEnvelopeRepository
    // Тут мы просто пробрасываем значение до свойств класс
    // isAdditionalProperties - тут не уместно, так как свойства класса не динамичны
    @Override
    public void updateRepository(String repositoryPropertyKey, PropertyDispatcher<T> propertyDispatcher) {
        PropertyEnvelope<T> propertyEnvelope = getByRepositoryPropertyKey(repositoryPropertyKey);
        if (propertyEnvelope == null) {
            throw new RuntimeException("propertyEnvelopeRepository is null");
        }
        propertyEnvelope.syncPropertyValue();
        fill(propertyEnvelope);
    }

}
