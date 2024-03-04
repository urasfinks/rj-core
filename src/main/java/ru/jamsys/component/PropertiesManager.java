package ru.jamsys.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class PropertiesManager {
    ApplicationContext applicationContext;

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public <T> T getProperties(String key, Class<T> cls) {
        Environment environment = applicationContext.getEnvironment();
        T result = environment.getProperty(key, cls);
        if (result == null) {
            applicationContext.getBean(ExceptionHandler.class).handler(new RuntimeException("Required key '" + key + "' not found"));
        }
        return result;
    }

}
