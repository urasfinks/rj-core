package ru.jamsys.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.PropertySource;
import ru.jamsys.core.component.Core;

@PropertySource("global.properties")
@SpringBootApplication
public class App {

    public static ConfigurableApplicationContext context = null;

    public static void main(String[] args) {
        if (context == null) {
            context = SpringApplication.run(App.class, args);
            context.getBean(Core.class).run();
        }
    }

}
