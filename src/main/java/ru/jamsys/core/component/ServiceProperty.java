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
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Lazy
public class ServiceProperty {

    final private Map<String, Set<PropertyUpdateDelegate>> subscriberKey = new ConcurrentHashMap<>();

    final private Map<String, Set<PropertyUpdateDelegate>> subscriberRegexp = new ConcurrentHashMap<>();

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
            map.forEach((key, value) -> {
                if (value == null) {
                    prop.remove(key);
                    if (subscriberKey.containsKey(key)) {
                        notify.addAll(subscriberKey.get(key));
                    }
                } else if (!prop.containsKey(key) || !prop.get(key).equals(value)) {
                    prop.put(key, value);
                    if (subscriberKey.containsKey(key)) {
                        notify.addAll(subscriberKey.get(key));
                    }
                }
            });
            notify.forEach(subscriber -> subscriber.onPropertyUpdate(new HashMap<>(map)));
        }
    }

    public void subscribeByKey(String absoluteKey, PropertyUpdateDelegate propertyUpdateDelegate, boolean require, String defValue) {
        String result = prop.get(absoluteKey);
        if (require && result == null) {
            throw new RuntimeException("Required key '" + absoluteKey + "' not found");
        }
        if (result == null) {
            result = prop.computeIfAbsent(absoluteKey, _ -> defValue);
        }
        subscriberKey.computeIfAbsent(absoluteKey, _ -> new HashSet<>()).add(propertyUpdateDelegate);
        propertyUpdateDelegate.onPropertyUpdate(new HashMapBuilder<String, String>().append(absoluteKey, result));
    }

    public void subscribeByRegexp(String pattern, PropertyUpdateDelegate propertyUpdateDelegate) {
        subscriberRegexp.computeIfAbsent(pattern, _ -> new HashSet<>()).add(propertyUpdateDelegate);
        propertyUpdateDelegate.onPropertyUpdate(findPropByRegexp(pattern));
    }

    private Map<String, String> findPropByRegexp(String pattern) {
        Map<String, String> result = new LinkedHashMap<>();
        UtilRisc.forEach(null, prop, (key, value) -> {
            if (Util.regexpFind(key, pattern) != null) {
                result.put(key, value);
            }
        });
        return result;
    }

    private Set<PropertyUpdateDelegate> getRegexpSubscriber(String key) {
        Set<PropertyUpdateDelegate> fits = new HashSet<>();
        UtilRisc.forEach(null, subscriberRegexp, (pattern, propertyUpdateDelegates) -> {
            if (Util.regexpFind(key, pattern) != null) {
                fits.addAll(propertyUpdateDelegates);
            }
        });
        return fits;
    }

    public void unsubscribe(String key, PropertyUpdateDelegate propertyUpdateDelegate) {
        unsubscribe(key, propertyUpdateDelegate, subscriberKey);
        unsubscribe(key, propertyUpdateDelegate, subscriberRegexp);
    }

    private void unsubscribe(String absoluteKey, PropertyUpdateDelegate propertyUpdateDelegate, Map<String, Set<PropertyUpdateDelegate>> map) {
        Set<PropertyUpdateDelegate> propertyUpdateDelegates = map.get(absoluteKey);
        if (propertyUpdateDelegates != null) {
            propertyUpdateDelegates.remove(propertyUpdateDelegate);
        }
    }

}
