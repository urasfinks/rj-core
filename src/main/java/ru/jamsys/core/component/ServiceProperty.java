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
import ru.jamsys.core.extension.property.PropertyConnector;
import ru.jamsys.core.extension.property.PropertyUpdateNotifier;
import ru.jamsys.core.extension.property.NameSpaceAgent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Lazy
public class ServiceProperty {

    final private Map<String, Set<NameSpaceAgent>> subscribe = new ConcurrentHashMap<>();

    @Getter
    final private Map<String, String> prop = new HashMap<>();

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
                    this.prop.put(prop, env.getProperty(prop));
                }
            }
        }
    }

    public void setProperty(Map<String, String> map) {
        Set<NameSpaceAgent> notify = new HashSet<>();
        if (!map.isEmpty()) {
            for (String key : map.keySet()) {
                String value = map.get(key);
                if (value == null) {
                    prop.remove(key);
                    notify.addAll(subscribe.get(key));
                } else if (!prop.get(key).equals(value)) {
                    prop.put(key, value);
                    notify.addAll(subscribe.get(key));
                }
            }
            notify.forEach(subscriber -> subscriber.onPropertyUpdate(new HashMap<>(map)));
        }
    }

    public void setProperty(String key, String value) {
        if (value == null) {
            prop.remove(key);
            if (subscribe.containsKey(key)) {
                HashMapBuilder<String, String> append = new HashMapBuilder<String, String>().append(key, null);
                subscribe.get(key).forEach(subscriber -> subscriber.onPropertyUpdate(append));
            }
        } else if (!prop.containsKey(key) || !prop.get(key).equals(value)) {
            prop.put(key, value);
            if (subscribe.containsKey(key)) {
                HashMapBuilder<String, String> append = new HashMapBuilder<String, String>().append(key, value);
                subscribe.get(key).forEach(subscriber -> subscriber.onPropertyUpdate(append));
            }
        }
    }

    public void subscribe(String key, NameSpaceAgent nameSpaceAgent, boolean require, String defValue) {
        String result = prop.get(key);
        if (require && result == null) {
            throw new RuntimeException("Required key '" + key + "' not found");
        }
        if (result == null) {
            result = prop.computeIfAbsent(key, _ -> defValue);
        }
        subscribe.computeIfAbsent(key, _ -> new HashSet<>()).add(nameSpaceAgent);
        nameSpaceAgent.onPropertyUpdate(new HashMapBuilder<String, String>().append(key, result));
    }

    public void unsubscribe(String key, NameSpaceAgent nameSpaceAgent) {
        subscribe.get(key).remove(nameSpaceAgent);
    }

    public NameSpaceAgent getSubscriber(
            PropertyUpdateNotifier propertyUpdateNotifier,
            PropertyConnector propertyConnector
    ) {
        return getSubscriber(propertyUpdateNotifier, propertyConnector, null, true);
    }

    public NameSpaceAgent getSubscriber(
            PropertyUpdateNotifier propertyUpdateNotifier,
            PropertyConnector propertyConnector,
            String ns
    ) {
        return new NameSpaceAgent(propertyUpdateNotifier, this, propertyConnector, ns, true);
    }

    public NameSpaceAgent getSubscriber(
            PropertyUpdateNotifier propertyUpdateNotifier,
            PropertyConnector propertyConnector,
            String ns,
            boolean require
    ) {
        return new NameSpaceAgent(propertyUpdateNotifier, this, propertyConnector, ns, require);
    }

}
