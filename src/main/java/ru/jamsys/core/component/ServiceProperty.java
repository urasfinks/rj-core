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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Lazy
public class ServiceProperty {

    final private Map<String, Set<PropertyUpdateDelegate>> subscribe = new ConcurrentHashMap<>();

    @Getter
    final private ServicePropertyFactory factory;

    @Getter
    final private Map<String, String> prop = new HashMap<>();

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
                    this.prop.put(prop, env.getProperty(prop));
                }
            }
        }
    }

    public void setProperty(String absoluteKey, String value) {
        setProperty(new HashMapBuilder<String, String>().append(absoluteKey, value));
    }

    public void setProperty(Map<String, String> map) {
        Set<PropertyUpdateDelegate> notify = new HashSet<>();
        if (!map.isEmpty()) {
            for (String key : map.keySet()) {
                String value = map.get(key);
                if (value == null) {
                    prop.remove(key);
                    if (subscribe.containsKey(key)) {
                        notify.addAll(subscribe.get(key));
                    }
                } else if (!prop.containsKey(key) || !prop.get(key).equals(value)) {
                    prop.put(key, value);
                    if (subscribe.containsKey(key)) {
                        notify.addAll(subscribe.get(key));
                    }
                }
            }
            notify.forEach(subscriber -> subscriber.onPropertyUpdate(new HashMap<>(map)));
        }
    }

    public void subscribe(String absoluteKey, PropertyUpdateDelegate propertyUpdateDelegate, boolean require, String defValue) {
        String result = prop.get(absoluteKey);
        if (require && result == null) {
            throw new RuntimeException("Required key '" + absoluteKey + "' not found");
        }
        if (result == null) {
            result = prop.computeIfAbsent(absoluteKey, _ -> defValue);
        }
        subscribe.computeIfAbsent(absoluteKey, _ -> new HashSet<>()).add(propertyUpdateDelegate);
        propertyUpdateDelegate.onPropertyUpdate(new HashMapBuilder<String, String>().append(absoluteKey, result));
    }

    public void unsubscribe(String absoluteKey, PropertyUpdateDelegate propertyUpdateDelegate) {
        subscribe.get(absoluteKey).remove(propertyUpdateDelegate);
    }

}
