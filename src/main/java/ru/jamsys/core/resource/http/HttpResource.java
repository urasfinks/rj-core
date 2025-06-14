package ru.jamsys.core.resource.http;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.expiration.AbstractExpirationResource;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.flat.util.UtilUri;
import ru.jamsys.core.resource.http.client.AbstractHttpConnector;
import ru.jamsys.core.resource.http.client.HttpConnectorApache;
import ru.jamsys.core.resource.http.client.HttpConnectorDefault;
import ru.jamsys.core.resource.http.client.HttpMethodEnum;

@Getter
public class HttpResource extends AbstractExpirationResource {

    private final String ns;

    private final HttpResourceRepositoryProperty property = new HttpResourceRepositoryProperty();

    private final PropertyDispatcher<Object> propertyDispatcher;

    public enum Type {
        DEFAULT,
        APACHE
    }

    public HttpResource(String ns) {
        this.ns = ns;
        propertyDispatcher = new PropertyDispatcher<>(
                null,
                property,
                getCascadeKey(ns)
        );
    }

    @JsonValue
    public Object getJsonValue() {
        return new HashMapBuilder<String, Object>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("class", getClass())
                .append("ns", ns)
                .append("propertyDispatcherNs", propertyDispatcher.getNs())
                ;
    }

    public AbstractHttpConnector prepare() {
        AbstractHttpConnector httpConnector = switch (Type.valueOf(property.getType().toUpperCase())) {
            case DEFAULT -> new HttpConnectorDefault();
            case APACHE -> new HttpConnectorApache();
        };
        if (property.getUrl() != null) {
            httpConnector.setUrl(property.getUrl());
        }
        if (property.getHeader() != null) {
            UtilUri.parseParameters(
                    "?" + property.getHeader(),
                    strings -> String.join(",", strings)
            ).forEach(httpConnector::addRequestHeader);
        }
        if (property.getConnectTimeoutMs() != null) {
            httpConnector.setConnectTimeoutMs(property.getConnectTimeoutMs());
        }
        if (property.getReadTimeoutMs() != null) {
            httpConnector.setReadTimeoutMs(property.getReadTimeoutMs());
        }
        httpConnector.setMethod(HttpMethodEnum.valueOf(property.getMethod()));
        return httpConnector;
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
