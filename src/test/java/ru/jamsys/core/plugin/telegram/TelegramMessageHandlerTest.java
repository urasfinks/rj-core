package ru.jamsys.core.plugin.telegram;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.jamsys.core.App;
import ru.jamsys.core.component.RouteGenerator;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.expiration.ExpirationMap;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.plugin.telegram.sender.TelegramSenderEmbedded;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.*;

class TelegramMessageHandlerTest {

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runSpring();
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @Test
    public void test() throws JsonProcessingException {
        Update object = UtilJson.toObject("""
                {
                  "update_id" : 793394279,
                  "message" : {
                    "message_id" : 69,
                    "from" : {
                      "id" : 290029195,
                      "first_name" : "Юра Мухин",
                      "is_bot" : false,
                      "username" : "urasfinks",
                      "language_code" : "ru",
                      "is_premium" : true
                    },
                    "date" : 1759436948,
                    "chat" : {
                      "id" : 290029195,
                      "type" : "private",
                      "first_name" : "Юра Мухин",
                      "username" : "urasfinks"
                    },
                    "text" : "/few",
                    "entities" : [
                      {
                        "type" : "bot_command",
                        "offset" : 0,
                        "length" : 4
                      }
                    ]
                  }
                }""", Update.class);

        // 1) Создаём partial-mock класса под тестом БЕЗ вызова конструктора
        TelegramMessageHandler handler = Mockito.mock(
                TelegramMessageHandler.class,
                Mockito.withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS)
        );

        // 2) Готовим моки зависимостей
        TelegramBot telegramBot = mock(TelegramBot.class, RETURNS_DEEP_STUBS);

        // getBotRepositoryProperty().getName()
        when(telegramBot.getBotRepositoryProperty().getName()).thenReturn("test_bot");


        when(telegramBot.getRouterRepository()).thenReturn(App.get(RouteGenerator.class).getRouterRepository(App.class));
        //when(generator.generate()).thenReturn(promise);

        // 3) sender config с deep-stub, чтобы .get().send(...)
//        @SuppressWarnings("unchecked")
//        ManagerConfiguration<TelegramSenderEmbedded> senderCfg =
//                mock(ManagerConfiguration.class, RETURNS_DEEP_STUBS);
//        when(senderCfg.get()).thenReturn(null);
        ManagerConfiguration<TelegramSenderEmbedded> real =
                ManagerConfiguration.getInstance("ns", "key", TelegramSenderEmbedded.class, null);

        ManagerConfiguration<TelegramSenderEmbedded> spy = spy(real);

        TelegramSenderEmbedded fake = mock(TelegramSenderEmbedded.class);
        doReturn(fake).when(spy).get();


        // 4) stepHandler — можно замокать полностью, нам важен только .get().remove(...)
        ManagerConfiguration<ExpirationMap<Long, String>> stepHandler = ManagerConfiguration.getInstance(
                telegramBot.getBotRepositoryProperty().getName(),
                telegramBot.getBotRepositoryProperty().getName(),
                ExpirationMap.class,
                integerXTestExpirationMap -> integerXTestExpirationMap
                        .setupTimeoutElementExpirationMs(600_000)
        );


        // 5) Вкалываем моки в приватные final поля (конструктор мы НЕ вызывали)
        ReflectionTestUtils.setField(handler, "telegramBot", telegramBot);
        ReflectionTestUtils.setField(handler, "telegramSenderEmbeddedManagerConfiguration", spy);
        ReflectionTestUtils.setField(handler, "stepHandler", stepHandler);

        handler.onUpdateReceived(object);

    }
}