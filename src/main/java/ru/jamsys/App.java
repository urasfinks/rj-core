package ru.jamsys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.PropertySource;
import ru.jamsys.component.Core;
import ru.jamsys.component.Security;
import ru.jamsys.task.handler.LoadStatisticToInfluxDb;


@PropertySource("global.properties")
@SpringBootApplication
public class App {

    public static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        context = SpringApplication.run(App.class, args);
        Security.init("""
MIIBVAIBADANBgkqhkiG9w0BAQEFAASCAT4wggE6AgEAAkEAi0b4fAQEzM9iVXZ93E81xYTr6t2n
0yEmHUVJpTe4RYT6OIO8I+5hZDYDhlInG4gh2jFlsSQXwFXKUsWEyxotIQIDAQABAkAW/KUpTr5+
ESJRKafXMymUSn5neqLmyTtRrGxdvOcIbgu5mrUqmYLpw47fBOH2kK2hMd/u8Zum+R53843vKrYZ
AiEA+2f6oQeE0dsRwtVQ8+5PHpsxNAm7T/VzPULX4o2RAP0CIQCN0npXwHWwo9zqX8KET2tBF6dC
l6VO02Ybw5aKNmuX9QIhAI4hk80UDiACZQEsTi8KDIr2HBQaaF5lGriIoLqBZHgVAiBkPRztwwEr
/VWabl58x+ll04MLxUU4xqAIBaD0RWyctQIgMt6GFvwEx0V06A+W1LUeVfWDsJPm9pd+UBE0ft+q
r+A=
""".toCharArray());
        context.getBean(Core.class).applicationInit(LoadStatisticToInfluxDb.class);
        context.getBean(Core.class).run();
    }

}
