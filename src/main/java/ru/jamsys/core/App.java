package ru.jamsys.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.ContextClosedEvent;
import ru.jamsys.core.component.Core;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.flat.util.Util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@PropertySource("global.properties")
@SpringBootApplication
public class App {

    private static final Map<Class<?>, Object> mapBean = new ConcurrentHashMap<>();

    public static ConfigurableApplicationContext context = null;

    public static SpringApplication application = new SpringApplication(App.class);

    public static void main(String[] args) {
        application.addListeners((ApplicationListener<ContextClosedEvent>) _ -> {
            Util.logConsole("App shutdown process...");

            AtomicBoolean isRun = new AtomicBoolean(true);

            Thread shutdownThread = new Thread(() -> {
                Thread.currentThread().setName("Shutdown");
                context.getBean(Core.class).shutdown();
                isRun.set(false);
            });
            //Запускаем демоническим, что бы если будут зависания в shutdown - мы могли это проигнорировать
            shutdownThread.setDaemon(true);
            shutdownThread.start();

            if (Util.await(isRun, 5000, "App stop with error timeout shutdown")) {
                Util.logConsole("App stop. I wish you good luck, see you soon!");
            }
        });
        run(args);
    }

    @SuppressWarnings("all")
    public static  <T> T get(Class<T> cls) {
        return (T) mapBean.computeIfAbsent(cls, aClass -> App.context.getBean(aClass));
    }

    public static void error(Throwable th) {
        get(ExceptionHandler.class).handler(th);
    }

    public static void run(String[] args) {
        if (context == null) {
            context = application.run(args);
            for (String data : args) {
                if (data.contains("=")) {
                    String key = data.substring(0, data.indexOf("="));
                    String value = data.substring(data.indexOf("=") + 1);
                    context.getBean(ServiceProperty.class).setProperty(key, value);
                    Util.logConsole("Setup ServiceProperty: " + key + "=" + value);
                }
            }
            get(Core.class).run();
        }
    }

    public static void shutdown() {
        if (context != null) {
            get(Core.class).shutdown();
            context.close();
            context = null;
            mapBean.clear();
        }
    }

}
