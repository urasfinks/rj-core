package ru.jamsys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.jamsys.component.Core;
import ru.jamsys.component.Security;


@SpringBootApplication
public class App {

    public static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        context = SpringApplication.run(App.class, args);
        App.context.getBean(Security.class).setPrivateKey("""
                MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAlbD08iy0vuVlGLQ8mqSjCIx7BVjo
                6vH7HUquJS2UHfCutwe9lbxE7rt3BJYO0EUp4t87nM9s2YwLJkzhrkeajwIDAQABAkARaME0ISrs
                QLWfR+b8fUVQyzXLi2mbWYVBBNx4CnL9gEBcOGksMQo4f1QEfSxhO/Fhhon5uikrqw4u4cEtjZ8h
                AiEA5J8CvZWS8opCyJF+DdcnLPzo6ZC0+6M9tLuMt1BJX8kCIQCnniPabh45a1abYNmAuWzliiNY
                BE54zvwfIopsn7rDlwIgd+W0iDyjPjOoZot28kc9smhItgVABSBNQjWBzLl8YZECICizo+0klD5J
                LEyqpeY2IJVUh+SVlyCK0noU/xwFZWqdAiB0AN/XR4MQw4zwEEilSCyw3TZua/Lgkwg5Dhe4V6aO
                5Q==
                """.toCharArray());

        context.getBean(Core.class).run(null);
        System.out.println("Hello World!");
        context.getBean(Core.class).shutdown();
    }

}
