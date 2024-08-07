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
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.EncodedResourceResolver;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.ServletWebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.web.socket.WebSocket;
import ru.jamsys.core.extension.property.PropertiesContainer;

import javax.annotation.PreDestroy;

@SuppressWarnings("unused")
@Configuration
@EnableWebSocket
@PropertySource("global.properties")
public class AppConfiguration implements WebSocketConfigurer, WebMvcConfigurer {

    private PropertiesContainer container = null;

    private ApplicationContext applicationContext;

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        container = applicationContext.getBean(ServiceProperty.class).getFactory().getContainer();
    }

    @Override
    public void registerWebSocketHandlers(@NotNull WebSocketHandlerRegistry registry) {
        if (container.watch(Boolean.class, "run.args.web", true, true, null).get()) {
            container.watch(
                    String.class,
                    "run.args.web.socket.path",
                    "/socket/*",
                    true,
                    s -> registry.addHandler(applicationContext.getBean(WebSocket.class), s)
            );
            ((ServletWebSocketHandlerRegistry) registry).setOrder(-1);
        }
    }

    @Bean
    public ServletWebServerFactory servletContainer() {
        // Функционал runtime я считаю не должен влиять на такие вещи как ssl приклада
        // По этому логика реализована линейной
        // Не могу предположить, что есть необходимость на ходу менять правила игры работать по ssl и редиректам
        // Как минимум это странно, как максимум - я без понятия как это сделать)))
        if (container.watch(Boolean.class, "run.args.web", true, true, null).get()) {

            Integer httpPort = container.watch(Integer.class, "run.args.web.http.port", 80, true, null).get();
            Integer httpsPort = container.watch(Integer.class, "run.args.web.https.port", 443, true, null).get();
            Boolean ssl = container.watch(Boolean.class, "run.args.web.ssl", false, true, null).get();
            Boolean redirect = container.watch(Boolean.class, "run.args.web.http.redirect.https", true, true, null).get();

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

    @Override
    public void addResourceHandlers(@NotNull ResourceHandlerRegistry registry) {
        if (container.watch(Boolean.class, "run.args.web", true, true, null).get()) {
            String location = container.watch(String.class, "run.args.web.resource.location", "file:web/", true, null).get();
            registry
                    .addResourceHandler("/**.*", "/*/*.*")
                    .addResourceLocations(location)
                    .setCachePeriod(-1)
                    .resourceChain(false)
                    .addResolver(new EncodedResourceResolver());
            registry.setOrder(-2);
        }
    }

}
