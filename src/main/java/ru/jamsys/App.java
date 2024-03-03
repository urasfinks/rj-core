package ru.jamsys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.jamsys.component.Core;
import ru.jamsys.component.ExceptionHandler;

import java.net.InetSocketAddress;
import java.net.Socket;


@SpringBootApplication
public class App {

    public static ConfigurableApplicationContext context;
    public static String ip;

    public static void main(String[] args) {
        context = SpringApplication.run(App.class, args);
        ip = getIp();
        context.getBean(Core.class).run(null);
    }

    private static String getIp() {
        String result = null;
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress("google.com", 80));
            result = socket.getLocalAddress().toString();
            socket.close();
        } catch (Exception e) {
            context.getBean(ExceptionHandler.class).handler(e);
        }
        return result;
    }

}
