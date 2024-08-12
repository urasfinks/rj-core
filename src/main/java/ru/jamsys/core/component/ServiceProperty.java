package ru.jamsys.core.component;

import lombok.Getter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Component;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.property.PropertyUpdateDelegate;
import ru.jamsys.core.extension.property.ServicePropertyFactory;
import ru.jamsys.core.extension.property.item.PropertySource;
import ru.jamsys.core.extension.property.item.PropertySubscriber;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Lazy
public class ServiceProperty {

    @Getter
    final private ServicePropertyFactory factory;

    final private Map<String, PropertySource> prop = new ConcurrentHashMap<>();

    public String unitTestGetProp(String propKey) {
        return prop.get(propKey).getValue();
    }

    //Нужен для момента, когда будет добавляться новое Property, что бы можно было к нему навешать старых слушателей
    final private Set<PropertySubscriber> subscribers = Util.getConcurrentHashSet();

    public boolean containsSubscriber(PropertySubscriber propertySubscriber) {
        return subscribers.contains(propertySubscriber);
    }

    // Получить статическую пробу
    public String getProbe(String key) {
        return prop.get(key).getValue();
    }

    public ServiceProperty(ApplicationContext applicationContext) {
        factory = new ServicePropertyFactory(this);
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
                    createIfNotExist(prop, env.getProperty(prop));
                }
            }
        }
    }

    // Добавляем новое или изменяем значение существующего Property
    public void setProperty(String key, String value) {
        setProperty(new HashMapBuilder<String, String>().append(key, value));
    }

    // Добавляем новое или изменяем значение существующего Property
    public void setProperty(Map<String, String> map) {
        Map<PropertySubscriber, Map<String, String>> notify = new HashMap<>();
        if (!map.isEmpty()) {
            map.forEach((key, value) -> {
                PropertySource propertySource = createIfNotExist(key, value);
                if (propertySource.isUpdateValue()) {
                    UtilRisc.forEach(null, propertySource.getSubscribers(), propertySubscriber -> {
                        notify.computeIfAbsent(propertySubscriber, _ -> new HashMap<>()).put(key, value);
                    });
                }
                if (value == null) {
                    this.prop.remove(key);
                }
            });
            notify.forEach(PropertySubscriber::onPropertyUpdate);
        }
    }

    private PropertySource createIfNotExist(String key, String value) {
        PropertySource result = this.prop.computeIfAbsent(key, k -> {
            PropertySource propertySource = new PropertySource(k);
            UtilRisc.forEach(null, subscribers, propertySource::check);
            return propertySource;
        });
        result.setValue(value);
        return result;
    }

    public PropertySubscriber subscribe(PropertySubscriber propertySubscriber) {
        if (!subscribers.contains(propertySubscriber)) {
            subscribers.add(propertySubscriber);
            attacheAndNotifySubscriber(propertySubscriber);
        }
        return propertySubscriber;
    }

    public PropertySubscriber subscribe(String key, PropertyUpdateDelegate propertyUpdateDelegate, boolean require, String defValue) {

        PropertySubscriber propertySubscriber = new PropertySubscriber();
        propertySubscriber.setKey(key);
        propertySubscriber.setPropertyUpdateDelegate(propertyUpdateDelegate);
        subscribers.add(propertySubscriber);

        PropertySource result = prop.get(key);
        if (require && result == null) {
            throw new RuntimeException("Required key '" + key + "' not found");
        }
        if (result == null) {
            createIfNotExist(key, defValue);
        }

        attacheAndNotifySubscriber(propertySubscriber);
        return propertySubscriber;
    }

    public PropertySubscriber subscribe(String regexp, PropertyUpdateDelegate propertyUpdateDelegate) {
        PropertySubscriber propertySubscriber = new PropertySubscriber();
        propertySubscriber.setRegexp(regexp);
        propertySubscriber.setPropertyUpdateDelegate(propertyUpdateDelegate);
        subscribers.add(propertySubscriber);

        attacheAndNotifySubscriber(propertySubscriber);
        return propertySubscriber;
    }

    private void attacheAndNotifySubscriber(PropertySubscriber propertySubscriber) {
        Map<String, String> map = new LinkedHashMap<>();
        UtilRisc.forEach(null, this.prop, (key, propertySource) -> {
            propertySource.check(propertySubscriber);
            if (propertySource.getSubscribers().contains(propertySubscriber)) {
                map.put(key, propertySource.getValue());
            }
        });
        propertySubscriber.onPropertyUpdate(map);
    }

    public void unsubscribe(PropertySubscriber propertySubscriber) {
        subscribers.remove(propertySubscriber);
        UtilRisc.forEach(null, this.prop, (_, propertySource) -> {
            propertySource.remove(propertySubscriber);
        });
    }

}
