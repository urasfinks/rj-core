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

import javax.annotation.PreDestroy;

@SuppressWarnings("unused")
@Configuration
@EnableWebSocket
public class AppConfiguration implements WebSocketConfigurer {

    private final PropertyValueContainer prop = new PropertyValueContainer();

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void registerWebSocketHandlers(@NotNull WebSocketHandlerRegistry registry) {
        prop.init(
                String.class,
                "run.args.web.socket.path",
                "/socketDefault/*",
                (_, path) -> registry.addHandler(new WebSocket(), path)
        );
    }

    public boolean serverSslEnabled = false;

    @Bean
    public ServletWebServerFactory servletContainer() {
        prop.setApplicationContext(applicationContext);
        PropertyValue<Boolean> webHttp = prop.init(Boolean.class, "run.args.web", null);

        if (webHttp.get()) {
            PropertyValue<Integer> httpPort = prop.init(Integer.class, "run.args.web.http.port", 80);
            PropertyValue<Integer> httpsPort = prop.init(Integer.class, "run.args.web.https.port", 443);
            PropertyValue<Boolean> ssl = prop.init(Boolean.class,"run.args.web.ssl",false);
            PropertyValue<Boolean> redirect = prop.init(Boolean.class,"run.args.web.http.redirect.to.https",true);

            TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
                @Override
                protected void postProcessContext(Context context) {
                    if (ssl.get()) {
                        SecurityConstraint securityConstraint = new SecurityConstraint();
                        securityConstraint.setUserConstraint("CONFIDENTIAL");
                        SecurityCollection collection = new SecurityCollection();
                        collection.addPattern("/*");
                        securityConstraint.addCollection(collection);
                        context.addConstraint(securityConstraint);
                    }
                }
            };
            if (ssl.get() && redirect.get()) {
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
        prop.shutdown();
    }

}
