package ru.jamsys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.jamsys.template.Template;

@SpringBootApplication
public class App {

    public static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        context = SpringApplication.run(App.class, args);
        //System.out.println("Hello World!");
        //Template.parse("H ${w\\{o\\}}");
        System.out.println(Template.getParsedTemplate("$${opa}"));
    }

}
