package ru.jamsys.core.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Component;
import ru.jamsys.core.extension.HashMapBuilder;
import ru.jamsys.core.extension.PropertyConnector;
import ru.jamsys.core.extension.Subscriber;
import ru.jamsys.core.extension.SubscriberPropertyNotifier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Lazy
public class PropertyComponent {

    final private Map<String, Set<Subscriber>> subscribe = new ConcurrentHashMap<>();

    final private Map<String, String> prop = new HashMap<>();

    public PropertyComponent(ApplicationContext applicationContext) {
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

    public void update(Map<String, String> map) {
        Set<Subscriber> notify = new HashSet<>();
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
            notify.forEach(subscriber -> subscriber.onUpdate(new HashMap<>(map)));
        }
    }

    public void update(String key, String value) {

        if (value == null) {
            prop.remove(key);
            if (subscribe.containsKey(key)) {
                HashMapBuilder<String, String> append = new HashMapBuilder<String, String>().append(key, null);
                subscribe.get(key).forEach(subscriber -> subscriber.onUpdate(append));
            }
        } else if (!prop.containsKey(key) || !prop.get(key).equals(value)) {
            prop.put(key, value);
            if (subscribe.containsKey(key)) {
                HashMapBuilder<String, String> append = new HashMapBuilder<String, String>().append(key, value);
                subscribe.get(key).forEach(subscriber -> subscriber.onUpdate(append));
            }
        }
    }

    public void subscribe(String key, Subscriber subscriber, boolean require, String defValue) {
        String result = prop.get(key);
        if (require && result == null) {
            throw new RuntimeException("Required key '" + key + "' not found");
        }
        if (result == null) {
            result = prop.computeIfAbsent(key, _ -> defValue);
        }
        subscribe.computeIfAbsent(key, _ -> new HashSet<>()).add(subscriber);
        subscriber.onUpdate(new HashMapBuilder<String, String>().append(key, result));
    }

    public void unsubscribe(String key, Subscriber subscriber) {
        subscribe.get(key).remove(subscriber);
    }

    public Subscriber getSubscriber(SubscriberPropertyNotifier subscriberPropertyNotifier, PropertyConnector propertyConnector) {
        return getSubscriber(subscriberPropertyNotifier, propertyConnector, null);
    }

    public Subscriber getSubscriber(SubscriberPropertyNotifier subscriberPropertyNotifier, PropertyConnector propertyConnector, String ns) {
        return new Subscriber(subscriberPropertyNotifier, this, propertyConnector, ns);
    }

}
