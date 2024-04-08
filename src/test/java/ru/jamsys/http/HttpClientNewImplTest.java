package ru.jamsys.http;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.SpringApplication;
import ru.jamsys.App;
import ru.jamsys.component.Core;

class HttpClientNewImplTest {

    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        App.context = SpringApplication.run(App.class, args);
        App.context.getBean(Core.class).run();
    }
}