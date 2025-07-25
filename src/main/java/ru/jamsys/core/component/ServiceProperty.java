package ru.jamsys.core.component;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.property.Property;
import ru.jamsys.core.extension.property.PropertySubscription;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

// Хранилище Property.
// Отвечает за создание всех Property, создавать экземпляры Property в других местах запрещено.
// Отвечает за регистрацию подписок, для того, что бы распространить подписки на все Property.

@Component
//@Lazy - надо что бы он загружался сразу же, так как используется в ServiceClassFinder -> PropertyDispatcher
public class ServiceProperty {

    final private Map<String, Property> properties = new ConcurrentHashMap<>();

    final private ConcurrentLinkedDeque<String> sequenceKey = new ConcurrentLinkedDeque<>();

    //Нужен для момента, когда будет добавляться новое Property, что бы можно было к нему навешать старых слушателей
    final private Set<PropertySubscription<?>> subscriptions = Util.getConcurrentHashSet();

    public ServiceProperty(ApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        MutablePropertySources propertySources = ((AbstractEnvironment) env).getPropertySources();
        for (org.springframework.core.env.PropertySource<?> next : propertySources) {
            String name = next.getName();
            if (
                    name.equals("configurationProperties")
                            || name.equals("systemProperties")
                            || name.equals("systemEnvironment")
                            || name.equals("random")
            ) {
                continue;
            }
            Map<String, String> description = new HashMap<>();
            if (next instanceof EnumerablePropertySource) {
                for (String prop : ((EnumerablePropertySource<?>) next).getPropertyNames()) {
                    if (prop.endsWith(".description")) {
                        description.put(prop.substring(0, prop.indexOf(".description")), env.getProperty(prop));
                        continue;
                    }
                    computeIfAbsent(
                            prop,
                            env.getProperty(prop),
                            property -> property.getTraceSetup().getLast().setResource(next.getName())
                    );
                }
                description.forEach((key, desc) -> {
                    Property property = properties.get(key);
                    if (property != null) {
                        property.setDescriptionIfNull(desc);
                    } else {
                        computeIfAbsent(
                                key +".description",
                                desc,
                                property1 -> property1.getTraceSetup().getLast().setResource(next.getName())
                        );
                    }
                });
            }
        }
        if (properties.containsKey("spring.application.name")) {
            String value = properties.get("spring.application.name").get();
            if (value != null) {
                App.applicationName = value;
            }
        }
    }

    // Получить все Property ключ которых подходит по шаблону
    public List<Property> getByRegexp(String regexp) {
        List<Property> result = new ArrayList<>();
        UtilRisc.forEach(null, sequenceKey, (propertyKey) -> {
            Property property = properties.get(propertyKey);
            if (property != null && property.isMatchPattern(regexp)) {
                result.add(property);
            }
        });
        return result;
    }

    public Property computeIfAbsent(String key, Object value) {
        return computeIfAbsent(
                key,
                value == null ? null : String.valueOf(value)
        );
    }

    @Getter
    public static class Equals {
        final boolean equals;
        final String oldValue;
        final String newValue;

        public Equals(boolean equals, String oldValue, String newValue) {
            this.equals = equals;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

    }

    public Equals equals(String key, String value) {
        String curValue = null;
        if (properties.containsKey(key)) {
            curValue = properties.get(key).get();
        }
        return new Equals(
                Objects.equals(value, curValue),
                value,
                curValue
        );
    }

    @SuppressWarnings("all")
    public Property getOrThrow(String key, Object context) throws Exception {
        if (!properties.containsKey(key)) {
            throw new ForwardException("Property: " + key + " does not exist", context);
        }
        return properties.get(key);
    }

    public boolean contains(String key) {
        return properties.containsKey(key);
    }

    public void set(String key, Object value) {
        computeIfAbsent(key, null).set(value);
    }

    public Property computeIfAbsent(String key, String value) {
        return computeIfAbsent(key, value, null);
    }

    public Property computeIfAbsent(String key, String value, Consumer<Property> onNew) {
        AtomicBoolean add = new AtomicBoolean(false);
        Property property = this.properties.computeIfAbsent(key, _ -> {
            Property newProperty = new Property(key, value);
            sequenceKey.add(key);
            UtilRisc.forEach(null, subscriptions, newProperty::addSubscription);
            add.set(true);
            return newProperty;
        });
        // Мы не можем в лямде вызывать property.emit() потому что в тот момент this.properties ещё не вставился
        // новый property, а внутри emit происходит другие computeIfAbsent и так начинается рекурсия
        if (add.get()) {
            if (onNew != null) {
                onNew.accept(property);
            }
            // После того, как для нового Property добавили существующих подписчиков - оповестим подписчиков
            property.emit(property.get());
        }
        return property;
    }

    // Подписка это просто размещение подписки к подходящим Property
    // никаких уведомлений и что-то подобное не будет
    @SuppressWarnings("all")
    public PropertySubscription<?> addSubscription(PropertySubscription<?> propertySubscription) {
        if (!subscriptions.contains(propertySubscription)) {
            subscriptions.add(propertySubscription);
            UtilRisc.forEach(null, sequenceKey, propertyKey -> {
                Property property = properties.get(propertyKey);
                if (property != null) {
                    property.addSubscription(propertySubscription);
                }
            });
        }
        return propertySubscription;
    }

    public void removeSubscription(PropertySubscription<?> propertySubscription) {
        subscriptions.remove(propertySubscription);
        UtilRisc.forEach(null, this.properties, (_, property) -> {
            property.removeSubscription(propertySubscription);
        });
    }

    @JsonValue
    public String getJsonValue() {
        List<Property> result = new ArrayList<>();
        UtilRisc.forEach(null, sequenceKey, (propertyKey) -> {
            Property property = properties.get(propertyKey);
            if (property != null) {
                result.add(property);
            }
        });
        return UtilJson.toStringPretty(result, "[]");
    }

}
