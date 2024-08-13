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
import org.springframework.context.annotation.PropertySource;
import org.springframework.util.unit.DataSize;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.ServletWebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import ru.jamsys.core.component.ServiceClassFinder;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.web.socket.WebSocket;
import ru.jamsys.core.extension.property.PropertiesContainer;

import javax.annotation.PreDestroy;

@SuppressWarnings("unused")
@Configuration
@EnableWebSocket
@PropertySource("global.properties")
public class AppConfiguration implements WebSocketConfigurer {

    private PropertiesContainer container = null;

    private ApplicationContext applicationContext;

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        container = applicationContext.getBean(ServiceProperty.class).getFactory().getContainer();
    }

    private ServiceProperty serviceProperty;

    @Autowired
    public void setServiceProperty(ServiceProperty serviceProperty){
        this.serviceProperty = serviceProperty;
    }

    @Override
    public void registerWebSocketHandlers(@NotNull WebSocketHandlerRegistry registry) {
        if (serviceProperty.get(Boolean.class, "run.args.web.socket", false)) {
            container.watch(
                    String.class,
                    "run.args.web.socket.path",
                    "/socket/*",
                    true,
                    s -> registry.addHandler(applicationContext.getBean(WebSocket.class), s)
            );
            ((ServletWebSocketHandlerRegistry) registry).setOrder(-1);
        } else {
            applicationContext.getBean(ServiceClassFinder.class).removeAvailableClass(
                    WebSocket.class,
                    "run.args.web.socket = false"
            );
        }
    }

    @Bean
    public ServletWebServerFactory servletContainer() {
        // Функционал runtime я считаю не должен влиять на такие вещи как ssl приклада
        // По этому логика реализована линейной
        // Не могу предположить, что есть необходимость на ходу менять правила игры работать по ssl и редиректам
        // Как минимум это странно, как максимум - я без понятия как это сделать)))
        if (serviceProperty.get(Boolean.class, "run.args.web", false)) {

            int httpPort = serviceProperty.get(Integer.class, "run.args.web.http.port", 80);
            int httpsPort = serviceProperty.get(Integer.class, "run.args.web.https.port", 443);
            boolean ssl = serviceProperty.get(Boolean.class, "run.args.web.ssl", false);
            boolean redirect = serviceProperty.get(Boolean.class, "run.args.web.http.redirect.https", true);

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
        container.watch(
                Integer.class,
                "run.args.web.multipart.mb.max",
                12,
                true,
                (v) -> factory.setMaxFileSize(DataSize.ofMegabytes(v))
        );
        container.watch(
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
