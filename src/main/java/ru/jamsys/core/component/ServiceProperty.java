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
import ru.jamsys.core.extension.property.PropertiesNsAgent;
import ru.jamsys.core.extension.property.ServicePropertyFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Lazy
public class ServiceProperty {

    final private Map<String, Set<PropertiesNsAgent>> subscribe = new ConcurrentHashMap<>();

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

    public void setProperty(Map<String, String> map) {
        Set<PropertiesNsAgent> notify = new HashSet<>();
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

    public void setProperty(String absoluteKey, String value) {
        if (value == null) {
            prop.remove(absoluteKey);
            if (subscribe.containsKey(absoluteKey)) {
                HashMapBuilder<String, String> append = new HashMapBuilder<String, String>().append(absoluteKey, null);
                subscribe.get(absoluteKey).forEach(subscriber -> subscriber.onPropertyUpdate(append));
            }
        } else if (!prop.containsKey(absoluteKey) || !prop.get(absoluteKey).equals(value)) {
            prop.put(absoluteKey, value);
            if (subscribe.containsKey(absoluteKey)) {
                HashMapBuilder<String, String> append = new HashMapBuilder<String, String>().append(absoluteKey, value);
                subscribe.get(absoluteKey).forEach(subscriber -> subscriber.onPropertyUpdate(append));
            }
        }
    }

    public void subscribe(String absoluteKey, PropertiesNsAgent propertiesNsAgent, boolean require, String defValue) {
        String result = prop.get(absoluteKey);
        if (require && result == null) {
            throw new RuntimeException("Required key '" + absoluteKey + "' not found");
        }
        if (result == null) {
            result = prop.computeIfAbsent(absoluteKey, _ -> defValue);
        }
        subscribe.computeIfAbsent(absoluteKey, _ -> new HashSet<>()).add(propertiesNsAgent);
        propertiesNsAgent.onPropertyUpdate(new HashMapBuilder<String, String>().append(absoluteKey, result));
    }

    public void unsubscribe(String absoluteKey, PropertiesNsAgent propertiesNsAgent) {
        subscribe.get(absoluteKey).remove(propertiesNsAgent);
    }

}
