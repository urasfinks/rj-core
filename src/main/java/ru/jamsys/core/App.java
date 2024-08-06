package ru.jamsys.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import ru.jamsys.core.component.Core;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.flat.util.Util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

// TODO: Нужен RateLimit на Http запросы

@SpringBootApplication
public class App {

    private static final Map<Class<?>, Object> mapBean = new ConcurrentHashMap<>();

    public static ConfigurableApplicationContext context = null;

    public static SpringApplication application;

    public static Class<?> springSource = App.class;

    private static void init() {
        if (application == null) {
            application = new SpringApplication(springSource);
        }
    }

    public static void main(String[] args) {
        init();
        application.addListeners((ApplicationListener<ContextClosedEvent>) _ -> {
            Util.logConsole("App shutdown process...");

            AtomicBoolean isRun = new AtomicBoolean(true);

            Thread shutdownThread = new Thread(() -> {
                Thread.currentThread().setName("daemon");
                get(Core.class).shutdown();
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
    public static <T> T get(Class<T> cls) {
        return (T) mapBean.computeIfAbsent(cls, aClass -> {
            T t = App.context.getBean(cls);
            if (t == null) {
                Util.logConsole("App.get(" + cls.getName() + ") return null");
            }
            return t;
        });
    }

    public static void error(Throwable th) {
        if (context != null) {
            ExceptionHandler exceptionHandler = get(ExceptionHandler.class);
            if (exceptionHandler != null) {
                exceptionHandler.handler(th);
                return;
            }
        }
        th.printStackTrace();
    }

    public static AppRunBuilder getRunBuilder() {
        return new AppRunBuilder();
    }

    public static void run(String[] args) {
        init();
        if (context == null) {
            Util.logConsole("Run arguments:");
            Util.printArray(args);
            context = application.run(args);
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
