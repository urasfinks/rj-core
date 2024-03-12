package ru.jamsys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.PropertySource;
import ru.jamsys.component.Core;
import ru.jamsys.component.Security;


@PropertySource("global.properties")
@SpringBootApplication
public class App {

    public static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        context = SpringApplication.run(App.class, args);
        Security.init("""
                MIIBVAIBADANBgkqhkiG9w0BAQEFAASCAT4wggE6AgEAAkEAhs81ubcqaFlryKkFG6wFXS+wN71s
                p8KtcR/xSXQL4JfQnGx+L5GGE9rvYWpu6/mgZIjObi7JxosPB+c1mBdIAQIDAQABAkAZHpm8eAqt
                JIJuwr3arOhX95een0uUi/Q9GM5hWUAQUnmUl3drRszL1hfzVBQ6cHhreRk5bh11QOODdc/qT3y1
                AiEA9iwrf0PRy5mEwspyGBRF/EuGGLsN+91MG/aVfhrSOa0CIQCMMO7S/1hbNkalDsEhgYsMXZzs
                SbDV+WwhamrZaSr6JQIhAPT4dXyav65tIgk5rpo5qn9rpJD9q+fEi5WUJ6WrCSKtAiAFcsVlV90k
                NljKg2dIGRPBWEYH/Nkth7MHHW6nomm0LQIgAi8EFCJ54oT1PHL9/M/uTdFVDMVOrYK4CIg/UmTn
                XAo=
                """.toCharArray());
        context.getBean(Core.class).run();
    }

}
