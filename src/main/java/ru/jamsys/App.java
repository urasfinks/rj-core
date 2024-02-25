package ru.jamsys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.jamsys.component.Core;
import ru.jamsys.component.StatisticReaderInfluxDb;


@SpringBootApplication
public class App {

    public static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        context = SpringApplication.run(App.class, args);
        context.getBean(Core.class).run(StatisticReaderInfluxDb.class);
        System.out.println("Hello World!");
        //context.getBean(Core.class).shutdown();
    }

}
