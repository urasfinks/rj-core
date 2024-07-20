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
import ru.jamsys.core.extension.property.item.PropertyBoolean;
import ru.jamsys.core.extension.property.item.PropertyString;

@SuppressWarnings("unused")
@Configuration
@EnableWebSocket
public class AppConfiguration implements WebSocketConfigurer {

    private static final int HTTP_PORT = 80;
    private static final int HTTPS_PORT = 443;
    private static final String HTTP = "http";
    private static final String USER_CONSTRAINT = "CONFIDENTIAL";

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void registerWebSocketHandlers(@NotNull WebSocketHandlerRegistry registry) {
        new PropertyValue<>(
                applicationContext,
                "run.args.web.socket.path",
                new PropertyString("/socketDefault/*"),
                (_, path) -> registry.addHandler(new WebSocket(), path)
        );
    }

    public boolean serverSslEnabled = false;

    @Bean
    public ServletWebServerFactory servletContainer() {
        PropertyValue<Boolean> webHttp = new PropertyValue<>(
                applicationContext,
                "run.args.web.http",
                new PropertyBoolean(null),
                null
        );
        if (webHttp.get()) {
            TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
                @Override
                protected void postProcessContext(Context context) {
                    if (serverSslEnabled) {
                        SecurityConstraint securityConstraint = new SecurityConstraint();
                        securityConstraint.setUserConstraint(USER_CONSTRAINT);
                        SecurityCollection collection = new SecurityCollection();
                        collection.addPattern("/*");
                        securityConstraint.addCollection(collection);
                        context.addConstraint(securityConstraint);
                    }
                }
            };
            if (serverSslEnabled) {
                Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
                connector.setScheme(HTTP);
                connector.setPort(HTTP_PORT);
                connector.setSecure(false);
                connector.setRedirectPort(HTTPS_PORT);
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

}
