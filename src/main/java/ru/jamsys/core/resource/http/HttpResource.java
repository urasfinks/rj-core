package ru.jamsys.core.resource.http;

import lombok.Getter;
import ru.jamsys.core.extension.UniversalPath;
import ru.jamsys.core.extension.expiration.AbstractExpirationResource;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.resource.http.client.HttpConnector;
import ru.jamsys.core.resource.http.client.HttpConnectorApache;
import ru.jamsys.core.resource.http.client.HttpConnectorDefault;
import ru.jamsys.core.resource.http.client.HttpResponse;

public class HttpResource extends AbstractExpirationResource {

    @SuppressWarnings("all")
    private final String ns;

    private final HttpResourceRepositoryProperty property = new HttpResourceRepositoryProperty();

    @Getter
    private final PropertyDispatcher<Object> propertyDispatcher;

    public HttpResource(String ns) {
        this.ns = ns;
        propertyDispatcher = new PropertyDispatcher<>(
                null,
                property,
                getCascadeKey(ns)
        );
    }

    public enum Type {
        DEFAULT,
        APACHE
    }

    public HttpConnector prepare() {
        HttpConnector httpConnector = switch (Type.valueOf(property.getType().toUpperCase())) {
            case DEFAULT -> new HttpConnectorDefault();
            case APACHE -> new HttpConnectorApache();
        };
        if (property.getUrl() != null) {
            httpConnector.setUrl(property.getUrl());
        }
        if (property.getHeader() != null) {
            UniversalPath universalPath = new UniversalPath("/?" + property.getHeader());
            universalPath.parseParameter().forEach(httpConnector::addRequestHeader);
        }
        return httpConnector;
    }

    public HttpResponse execute(HttpConnector arguments) {
        arguments.exec();
        return arguments.getHttpResponse();
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void runOperation() {
        propertyDispatcher.run();
    }

    @Override
    public void shutdownOperation() {
        propertyDispatcher.shutdown();
    }

    @Override
    public boolean checkFatalException(Throwable th) {
        return false;
    }

}
