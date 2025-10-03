package ru.jamsys.core.plugin.telegram;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.jamsys.core.App;
import ru.jamsys.core.component.RouteGenerator;
import ru.jamsys.core.extension.AbstractManagerElement;
import ru.jamsys.core.extension.RouteGeneratorRepository;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.property.PropertyDispatcher;

@Getter
public class TelegramBot extends AbstractManagerElement {

    private final String ns;

    private final String key;

    @JsonIgnore
    private final BotRepositoryProperty botRepositoryProperty = new BotRepositoryProperty();

    @JsonIgnore
    private final PropertyDispatcher<Object> propertyDispatcher;

    @JsonIgnore
    private RouteGeneratorRepository routerRepository;

    @JsonIgnore
    private TelegramBotsApi api;

    @JsonIgnore
    private TelegramMessageHandler telegramMessageHandler;

    @JsonIgnore
    private BotSession session = null;

    public TelegramBot(String ns, String key) {
        this.ns = ns;
        this.key = key;
        this.propertyDispatcher = new PropertyDispatcher<>(
                null,
                botRepositoryProperty,
                getCascadeKey(ns)
        );
    }

    @Override
    public void runOperation() {
        this.propertyDispatcher.run();
        String promiseGeneratorClass = botRepositoryProperty.getPromiseGeneratorClass();
        if (promiseGeneratorClass == null || promiseGeneratorClass.isEmpty()) {
            throw new ForwardException("promiseGeneratorClass is empty", this);
        }
        try {
            Class<?> aClass1 = Class.forName(botRepositoryProperty.getPromiseGeneratorClass());
            this.routerRepository = App.get(RouteGenerator.class).getRouterRepository(aClass1);
            this.api = new TelegramBotsApi(DefaultBotSession.class);
            this.telegramMessageHandler = new TelegramMessageHandler(this);
            this.session = api.registerBot(telegramMessageHandler);
        } catch (Throwable e) {
            throw new ForwardException(this, e);
        }
    }

    @Override
    public void shutdownOperation() {
        this.propertyDispatcher.shutdown();
        if (session != null) {
            // Зависает остановка
            //session.stop();
        }
        session = null;
    }

    @Override
    public void helper() {
        markActive();
    }

}
