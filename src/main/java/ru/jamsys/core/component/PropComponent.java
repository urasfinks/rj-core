package ru.jamsys.core.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
@Lazy
public class PropComponent {

    final ApplicationContext applicationContext;

    final Map<String, List<Consumer>> subscribe = new ConcurrentHashMap<>();

    Map<String, String> prop = new HashMap<>();

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

    public void getProp(String namespace, String key, Consumer<String> onUpdate) {
        getProp(namespace + "." + key, onUpdate);
    }

    public void getProp(String key, Consumer<String> onUpdate) {
        getProp(key, onUpdate, true, null);
    }

    public void getProp(String key, Consumer<String> onUpdate, boolean require, String defValue) {
        String result = prop.get(key);
        if (require && result == null) {
            throw new RuntimeException("Required key '" + key + "' not found");
        }
        if (result == null) {
            result = prop.computeIfAbsent(key, _ -> defValue);
        }
        subscribe.computeIfAbsent(key, _ -> new ArrayList<>()).add(onUpdate);
        onUpdate.accept(result);
    }

}
