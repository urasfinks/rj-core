package ru.jamsys.core;

import jakarta.servlet.MultipartConfigElement;
import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.web.socket.WebSocket;
import ru.jamsys.core.extension.property.PropertiesContainer;

import javax.annotation.PreDestroy;

@SuppressWarnings("unused")
@Configuration
@EnableWebSocket
public class AppConfiguration implements WebSocketConfigurer {

    private PropertiesContainer container = null;

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        container = applicationContext.getBean(ServiceProperty.class).getFactory().getContainer();
    }

    @Override
    public void registerWebSocketHandlers(@NotNull WebSocketHandlerRegistry registry) {
        if (container.getProperty(Boolean.class, "run.args.web", true, true, null).get()) {
            container.getProperty(
                    String.class,
                    "run.args.web.socket.path",
                    "/socketDefault/*",
                    true,
                    s -> registry.addHandler(new WebSocket(), s)
            );
        }
    }

    @Bean
    public ServletWebServerFactory servletContainer() {
        // Функционал runtime я считаю не должен влиять на такие вещи как ssl приклада
        // По этому логика реализована линейной
        // Не могу предположить, что вы решите на ходу менять правила игры работать по ssl и редиректам
        // Как минимум это странно, как максимум - я без понятия как это сделать)))
        if (container.getProperty(Boolean.class, "run.args.web", true, true, null).get()) {

            Integer httpPort = container.getProperty(Integer.class, "run.args.web.http.port", 80, true, null).get();
            Integer httpsPort = container.getProperty(Integer.class, "run.args.web.https.port", 443, true, null).get();
            Boolean ssl = container.getProperty(Boolean.class, "run.args.web.ssl", false, true, null).get();
            Boolean redirect = container.getProperty(Boolean.class, "run.args.web.http.redirect.https", true, true, null).get();

            TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
                @Override
                protected void postProcessContext(Context context) {
                    if (ssl) {
                        SecurityConstraint securityConstraint = new SecurityConstraint();
                        securityConstraint.setUserConstraint("CONFIDENTIAL");
                        SecurityCollection collection = new SecurityCollection();
                        collection.addPattern("/*");
                        securityConstraint.addCollection(collection);
                        context.addConstraint(securityConstraint);
                    }
                }
            };
            if (ssl && redirect) {
                Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
                connector.setScheme("http");
                connector.setPort(httpPort);
                connector.setSecure(false);
                connector.setRedirectPort(httpsPort);
                connector.setAsyncTimeout(1000);
                tomcat.addAdditionalTomcatConnectors(connector);
            }
            return tomcat;
        }
        return null;
    }

    @Bean
    MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        container.getProperty(
                Integer.class,
                "run.args.web.multipart.mb.max",
                12,
                true,
                (v) -> factory.setMaxFileSize(DataSize.ofMegabytes(v))
        );
        container.getProperty(
                Integer.class,
                "run.args.web.request.mb.max",
                12,
                true,
                (v) -> factory.setMaxRequestSize(DataSize.ofMegabytes(v))
        );
        return factory.createMultipartConfig();
    }

    @PreDestroy
    public void onDestroy() {
        container.shutdown();
    }

}
