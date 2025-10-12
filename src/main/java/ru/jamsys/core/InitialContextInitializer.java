package ru.jamsys.core;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

// Данный класс добавлен в META-INF/spring.factories как ранний хук, без бинов
@SuppressWarnings("unused")
public class InitialContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(@NotNull ConfigurableApplicationContext applicationContext) {
        // Мы не можем это сделать в AppConfiguration implements WebSocketConfigurer, WebMvcConfigurer
        // потому что если выключить run.args.web = false конфигуратор перестанет запускаться,
        // и initialContext будет null. Нам надо иметь статичный initialContext для получения бинов при старте spring,
        // а постоянно тащить ApplicationContext через спагетти вызовов вообще не хочется. Поэтому придуман такой хак
        App.initialContext = applicationContext;
    }

}
