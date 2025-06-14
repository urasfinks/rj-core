package ru.jamsys.core.extension.property.repository;

import lombok.Getter;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.annotation.PropertyDescription;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.annotation.PropertyNotNull;
import ru.jamsys.core.extension.annotation.PropertyValueRegexp;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.PropertyEnvelope;

import java.lang.reflect.Field;

// Репозиторий Property с автоматическим извлечением свойств у класса родителя через аннотации
// @FieldNameConstants для примера, что бы удобно скопировать имена Fields, а так оно тут не работает, его надо
// для всех наследников аннотировать

@Getter
public class RepositoryPropertyAnnotationField<T> extends AbstractRepositoryProperty<T> {

    @Override
    public void init(String ns, boolean sync) {
        if (getInit().compareAndSet(false, true)) {
            for (Field field : getClass().getDeclaredFields()) {
                if (field.getType().isPrimitive()) {
                    throw new RuntimeException("Filed " + field.getName() + " is primitive");
                }
                if (field.isAnnotationPresent(PropertyKey.class)) {
                    try {
                        field.setAccessible(true);
                        @SuppressWarnings("unchecked")
                        Class<T> cls = (Class<T>) field.getType();
                        @SuppressWarnings("unchecked")
                        T fieldValue = (T) field.get(this);
                        String repositoryPropertyKey = field.getAnnotation(PropertyKey.class).value();
                        if (repositoryPropertyKey.isEmpty()) {
                            throw new ForwardException(
                                    "repositoryPropertyKey is empty",
                                    new HashMapBuilder<String, Object>()
                                            .append("ns", ns)
                                            .append("field", field)
                            );
                        }
                        PropertyEnvelope<T> apply = new PropertyEnvelope<>(
                                this,
                                field,
                                cls,
                                field.getName(),
                                repositoryPropertyKey,
                                CascadeKey.complexLinear(ns, repositoryPropertyKey),
                                fieldValue,
                                field.isAnnotationPresent(PropertyDescription.class)
                                        ? field.getAnnotation(PropertyDescription.class).value()
                                        : null,
                                field.isAnnotationPresent(PropertyValueRegexp.class)
                                        ? field.getAnnotation(PropertyValueRegexp.class).value()
                                        : null,
                                field.isAnnotationPresent(PropertyNotNull.class),
                                false
                        );
                        if (sync) {
                            apply
                                    .syncPropertyValue()
                                    .apply();
                        }
                        getListPropertyEnvelopeRepository().add(apply);
                    } catch (Throwable th) {
                        throw new ForwardException(new HashMapBuilder<String, Object>()
                                .append("ns", ns)
                                .append("fieldName", field.getName()),
                                th
                        );
                    }
                }
            }
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
        propertyEnvelope
                .syncPropertyValue()
                .apply();
    }

}
