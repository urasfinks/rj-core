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

import java.util.Arrays;
import java.util.List;

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
                ns
        );
    }

    public boolean isAvailableGroup(long idChat) {
        String availableGroup = getBotRepositoryProperty().getAvailableGroup();
        if (availableGroup == null) {
            return false;
        }
        List<Long> ids = Arrays.stream(availableGroup.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .toList();

        return ids.contains(idChat);
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
        // session.stop(); - зависает, не могу использовать
        session = null;
    }

    @Override
    public void helper() {
        markActive();
    }

}
