package ru.jamsys.core.extension.property.repository;

import lombok.Getter;
import ru.jamsys.core.extension.annotation.PropertyDescription;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.annotation.PropertyNotNull;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.flat.util.UtilJson;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

// Класс помогает изъять свойства у класса родителя через аннотации
// @FieldNameConstants
// Для примера, что бы удобно скопировать было, а так оно тут не работает, его надо для всех наследников
@Getter
public class AnnotationPropertyExtractor<T> implements PropertyRepository<T> {

    private final List<PropertyEnvelopeRepository<T>> listPropertyEnvelopeRepository = new ArrayList<>();

    private final AtomicBoolean init = new AtomicBoolean(false);

    @Override
    public void init(PropertyDispatcher<T> propertyDispatcher) {
        if (init.compareAndSet(false, true)) {
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
                        PropertyEnvelopeRepository<T> tPropertyEnvelopeRepository = new PropertyEnvelopeRepository<>(
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
                                .setPropertyRepository(this)
                                .syncPropertyValue();
                        fill(tPropertyEnvelopeRepository);
                        listPropertyEnvelopeRepository.add(tPropertyEnvelopeRepository);
                    } catch (Throwable th) {
                        throw new ForwardException(th);
                    }
                }
            }
        }
    }

    @Override
    public void append(String repositoryPropertyKey, PropertyDispatcher<T> propertyDispatcher) {

    }

    private void fill(PropertyEnvelopeRepository<T> propertyEnvelopeRepository) {
        try {
            propertyEnvelopeRepository.getField().set(this, propertyEnvelopeRepository.getValue());
        } catch (Throwable th) {
            throw new RuntimeException(UtilJson.toStringPretty(propertyEnvelopeRepository, "--"), th);
        }
    }

    // Приходит уведомление, что обновилось значение по ключу репозитория
    // Значения нельзя устанавливать самостоятельно в PropertyEnvelopeRepository
    // Тут мы просто пробрасываем значение до свойств класс
    // isAdditionalProperties - тут не уместно, так как свойства класса не динамичны
    @Override
    public void updateRepository(String repositoryPropertyKey, PropertyDispatcher<T> propertyDispatcher) {
        PropertyEnvelopeRepository<T> propertyEnvelopeRepository = getByRepositoryPropertyKey(repositoryPropertyKey);
        if (propertyEnvelopeRepository == null) {
            throw new RuntimeException("propertyEnvelopeRepository is null");
        }
        propertyEnvelopeRepository.syncPropertyValue();
        fill(propertyEnvelopeRepository);
    }

}
