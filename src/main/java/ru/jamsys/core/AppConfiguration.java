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
import ru.jamsys.core.component.web.socket.WebSocket;
import ru.jamsys.core.extension.property.PropertyValue;
import ru.jamsys.core.extension.property.PropertyValueContainer;
import ru.jamsys.core.extension.property.item.PropertyBoolean;
import ru.jamsys.core.extension.property.item.PropertyInteger;
import ru.jamsys.core.extension.property.item.PropertyString;

import javax.annotation.PreDestroy;

@SuppressWarnings("unused")
@Configuration
@EnableWebSocket
public class AppConfiguration implements WebSocketConfigurer {

    private static final String USER_CONSTRAINT = "CONFIDENTIAL";

    private final PropertyValueContainer propertyValueContainer = new PropertyValueContainer();

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void registerWebSocketHandlers(@NotNull WebSocketHandlerRegistry registry) {
        propertyValueContainer.init(
                applicationContext,
                "run.args.web.socket.path",
                new PropertyString("/socketDefault/*"),
                (_, path) -> registry.addHandler(new WebSocket(), path)
        );
    }

    public boolean serverSslEnabled = false;

    @Bean
    public ServletWebServerFactory servletContainer() {
        propertyValueContainer.setApplicationContext(applicationContext);
        PropertyValue<Boolean> webHttp = propertyValueContainer.init(
                "run.args.web",
                new PropertyBoolean(null),
                null
        );

        if (webHttp.get()) {
            PropertyValue<Integer> httpPort = propertyValueContainer.init(
                    "run.args.web.http.port",
                    new PropertyInteger(80),
                    null
            );

            PropertyValue<Integer> httpsPort = propertyValueContainer.init(
                    "run.args.web.http.port",
                    new PropertyInteger(443),
                    null
            );

            PropertyValue<Boolean> ssl = propertyValueContainer.init(
                    "run.args.web.ssl",
                    new PropertyBoolean(false),
                    null
            );

            PropertyValue<Boolean> httpRedirectToHttps = propertyValueContainer.init(
                    applicationContext,
                    "run.args.web.http.redirect.to.https",
                    new PropertyBoolean(true),
                    null
            );

            TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
                @Override
                protected void postProcessContext(Context context) {
                    if (ssl.get()) {
                        SecurityConstraint securityConstraint = new SecurityConstraint();
                        securityConstraint.setUserConstraint(USER_CONSTRAINT);
                        SecurityCollection collection = new SecurityCollection();
                        collection.addPattern("/*");
                        securityConstraint.addCollection(collection);
                        context.addConstraint(securityConstraint);
                    }
                }
            };
            if (ssl.get() && httpRedirectToHttps.get()) {
                Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
                connector.setScheme("http");
                connector.setPort(httpPort.get());
                connector.setSecure(false);
                connector.setRedirectPort(httpsPort.get());
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
        factory.setMaxFileSize(DataSize.ofMegabytes(12));
        factory.setMaxRequestSize(DataSize.ofMegabytes(12));
        return factory.createMultipartConfig();
    }

    @PreDestroy
    public void onDestroy() {
        propertyValueContainer.shutdown();
    }

}
