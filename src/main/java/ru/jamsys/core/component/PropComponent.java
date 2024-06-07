package ru.jamsys.core.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
@Lazy
public class PropComponent {

    final ApplicationContext applicationContext;

    final Map<String, List<Consumer>> subscribe = new ConcurrentHashMap<>();

    Map<String, Object> prop = new HashMap<>();

    public PropComponent(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
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

    public <T> void getProp(String namespace, String key, Class<T> cls, Consumer<T> onUpdate) {
        getProp(namespace + "." + key, cls, onUpdate);
    }

    public <T> void getProp(String key, Class<T> cls, Consumer<T> onUpdate) {
        @SuppressWarnings("unchecked")
        T result = (T) prop.get(key);
        if (result == null) {
            throw new RuntimeException("Required key '" + key + "' not found");
        }
        subscribe.computeIfAbsent(key, _ -> new ArrayList<>()).add(onUpdate);
        onUpdate.accept(result);
    }

}
