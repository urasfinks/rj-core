package ru.jamsys.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.ContextClosedEvent;
import ru.jamsys.core.component.Core;
import ru.jamsys.core.flat.util.Util;

import java.util.concurrent.atomic.AtomicBoolean;

@PropertySource("global.properties")
@SpringBootApplication
public class App {

    public static ConfigurableApplicationContext context = null;

    public static SpringApplication application = new SpringApplication(App.class);

    public static void main(String[] args) {
        application.addListeners((ApplicationListener<ContextClosedEvent>) event -> {
            Util.logConsole("App shutdown process...");

            AtomicBoolean shutdownFinish = new AtomicBoolean(false);

            Thread shutdownThread = new Thread(() -> {
                Thread.currentThread().setName("Shutdown");
                context.getBean(Core.class).shutdown();
                shutdownFinish.set(true);
            });
            //Запускаем демоническим, что бы если будут зависания в shutdown - мы могли это проигнорировать
            shutdownThread.setDaemon(true);
            shutdownThread.start();

            long start = System.currentTimeMillis();
            long expiredTime = start + 5000;
            while (!shutdownFinish.get() && expiredTime >= System.currentTimeMillis()) {
                Thread.onSpinWait();
            }
            if (expiredTime >= System.currentTimeMillis()) {
                Util.logConsole("App stop. I wish you good luck, see you soon!");
            } else {
                Util.logConsole("App stop with error timeout shutdown");
            }
        });
        run(args);
    }

    public static void run(String[] args) {
        if (context == null) {
            context = application.run(args);
            context.getBean(Core.class).run();
        }
    }

    public static void shutdown() {
        if (context != null) {
            context.getBean(Core.class).shutdown();
            context.close();
            context = null;
        }
    }

}
