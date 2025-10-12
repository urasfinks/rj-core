package ru.jamsys.core;

import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@SuppressWarnings("unused")
@Component
@Order(1)
public class ApplicationContextInject {

    public ApplicationContextInject(ApplicationContext applicationContext) {
        // Мы не можем это сделать в AppConfiguration implements WebSocketConfigurer, WebMvcConfigurer
        // потому что если выключить run.args.web = false конфигуратор перестанет запускаться,
        // и initialContext будет null. Нам надо иметь статичный initialContext для получения бинов при старте spring,
        // а постоянно тащить ApplicationContext через спагетти вызовов вообще не хочется. Поэтому придуман такой хак
        App.initialContext = applicationContext;
    }

}
