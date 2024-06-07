package ru.jamsys.core.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
@Lazy
public class PropertiesComponent {

    final ApplicationContext applicationContext;

    final Map<String, List<Consumer>> subscribe = new ConcurrentHashMap<>();

    public PropertiesComponent(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public <T> void getProperties(String namespace, String key, Class<T> cls, Consumer<T> onUpdate) {
        getProperties(namespace + "." + key, cls, onUpdate);
    }

    public <T> void getProperties(String key, Class<T> cls, Consumer<T> onUpdate) {
        Environment environment = applicationContext.getEnvironment();
        T result = environment.getProperty(key, cls);
        if (result == null) {
            throw new RuntimeException("Required key '" + key + "' not found");
        }
        subscribe.computeIfAbsent(key, _ -> new ArrayList<>()).add(onUpdate);
        onUpdate.accept(result);
    }

}
