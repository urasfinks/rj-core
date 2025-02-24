package ru.jamsys.core.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.property.Property;
import ru.jamsys.core.extension.property.item.PropertySubscription;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

// Хранилище Property
// Отвечает за создание всех Property, создавать экземпляры Property в других местах запрещено
// Отвечает за регистрацию подписок, для того, что бы распространить подписки на все Property

@Component
@Lazy
public class ServiceProperty {

    final private Map<String, Property> properties = new ConcurrentHashMap<>();
    final private ConcurrentLinkedDeque<String> sequenceKey = new ConcurrentLinkedDeque<>();

    //Нужен для момента, когда будет добавляться новое Property, что бы можно было к нему навешать старых слушателей
    final private Set<PropertySubscription> subscriptions = Util.getConcurrentHashSet();

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
            if (next instanceof EnumerablePropertySource) {
                for (String prop : ((EnumerablePropertySource<?>) next).getPropertyNames()) {
                    computeIfAbsent(
                            prop,
                            env.getProperty(prop),
                            property -> property.getSetTrace().getLast().setResource(next.getName())
                    );
                }
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
    public List<Property> get(String regexp) {
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

    public void set(String key, Object value) {
        computeIfAbsent(key, null).set(value);
    }

    public Property computeIfAbsent(String key, String value, Consumer<Property> onNew) {
        return this.properties.computeIfAbsent(key, key1 -> {
            Property property = new Property(key1, value);
            sequenceKey.add(key1);
            UtilRisc.forEach(null, subscriptions, property::addSubscription);
            // После того, как для нового Property добавили существующих подписчиков - оповестим подписчиков
            if (onNew != null) {
                onNew.accept(property);
            }
            property.emit(property.get());
            return property;
        });
    }

    public Property computeIfAbsent(String key, String value) {
        return computeIfAbsent(key, value, null);
    }

    @SuppressWarnings("all")
    public PropertySubscription addSubscription(PropertySubscription propertySubscription) {
        if (!subscriptions.contains(propertySubscription)) {
            subscriptions.add(propertySubscription);
            UtilRisc.forEach(null, sequenceKey, (propertyKey) -> {
                Property property = properties.get(propertyKey);
                if (property != null) {
                    property.addSubscription(propertySubscription);
                }
            });
        }
        return propertySubscription;
    }

    public void removeSubscription(PropertySubscription propertySubscription) {
        subscriptions.remove(propertySubscription);
        UtilRisc.forEach(null, this.properties, (_, property) -> {
            property.removeSubscription(propertySubscription);
        });
    }

    public String getJson() {
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
