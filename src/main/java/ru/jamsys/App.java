package ru.jamsys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.jamsys.component.Core;
import ru.jamsys.component.Secret;
import ru.jamsys.component.StatisticReaderInfluxDb;


@SpringBootApplication
public class App {

    public static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        Secret.setPrivateKey("MIIBVAIBADANBgkqhkiG9w0BAQEFAASCAT4wggE6AgEAAkEArI/Eq8WX6ZAkXRHi4FVuMvebkRtucqOL+HT2Tadu5a5L4pfCytLDJtZiFQ1G8gdss8++Vt58tLhCmi4JxWZyhwIDAQABAkA2+q1mLxPqEgdL8eVvpThxm6tgjbVgaBQyCo3pCuYN3jTXPPLxvb56TYnqY+Q5OClRvDy+UifWQLbzkOcbDKvVAiEA/GxEXbaKWDAX+YjZxD+ejui+hMXhY32k/0e981N/KPsCIQCvAchP+h4ByFthNfV6ey9N10JoN0Not8IUS18hMQ4+5QIhAI06GeoAplh+1/sR+RzWp2S3jViFyfu7IWR+hCUukxefAiBmpqwBRVteflAjSAwyCJlpli7MhEXU4ZxEXSVyiZyqhQIgKUkSRCV7h6uFM8glDvEdlBPBLUA67Naj0e+pbYr126Y=".toCharArray());
        context = SpringApplication.run(App.class, args);
        context.getBean(Core.class).run(StatisticReaderInfluxDb.class);
        System.out.println("Hello World!");

        //context.getBean(Core.class).shutdown();
    }

}
