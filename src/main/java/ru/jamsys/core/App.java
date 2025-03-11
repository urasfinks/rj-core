package ru.jamsys.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import ru.jamsys.core.component.Core;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.extension.CascadeName;
import ru.jamsys.core.flat.util.Util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

// TODO: Нужен RateLimit на Http запросы

@SpringBootApplication
public class App implements CascadeName {

    public static App cascadeName = new App();

    public static Map<String, Class<?>> uniqueClassName = new ConcurrentHashMap<>();

    public static String applicationName = App.class.getName(); // Может поменяться при загрузке компонента ServiceProperty

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
            Util.logConsole(App.class, "App shutdown process...");

            AtomicBoolean run = new AtomicBoolean(true);

            Thread shutdownThread = new Thread(() -> {
                Thread.currentThread().setName("daemon");
                get(Core.class).shutdown();
                run.set(false);
            });
            //Запускаем демоническим, что бы если будут зависания в shutdown - мы могли это проигнорировать
            shutdownThread.setDaemon(true);
            shutdownThread.start();

            if (Util.await(run, 5000, 0, () -> Util.logConsole(
                    App.class,
                    "App stop with error timeout shutdown; last running: " + Core.lastOperation,
                    true
            ))) {
                Util.logConsole(App.class, "App stop. I wish you good luck, see you soon!");
            }
        });
        run(args);
    }

    @SuppressWarnings("all")
    public static <T> T get(Class<T> cls) {
        return (T) mapBean.computeIfAbsent(cls, aClass -> {
            T t = App.context.getBean(cls);
            if (t == null) {
                throw new RuntimeException("App.get(" + cls.getName() + ") return null");
            }
            return t;
        });
    }

    @SuppressWarnings("all")
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
            Util.logConsole(App.class, "Run arguments:");
            Util.printArray(App.class, args);
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

    public static String getUniqueClassName(Class<?> cls) {
        if (cls == null) {
            return "null";
        }
        Class<?> regClass = uniqueClassName.computeIfAbsent(cls.getSimpleName(), _ -> cls);
        if (regClass.equals(cls)) {
            return cls.getSimpleName();
        } else {
            uniqueClassName.putIfAbsent(cls.getName(), cls);
            return cls.getName();
        }
    }

    @Override
    public String getKey() {
        return null;
    }

    @Override
    public CascadeName getParentCascadeName() {
        return null;
    }

    @Override
    public String getCascadeName() {
        return App.class.getSimpleName();
    }

}
