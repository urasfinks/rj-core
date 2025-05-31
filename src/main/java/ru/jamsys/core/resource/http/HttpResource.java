package ru.jamsys.core.resource.http;

import ru.jamsys.core.extension.expiration.AbstractExpirationResource;
import ru.jamsys.core.extension.log.DataHeader;
import ru.jamsys.core.resource.http.client.HttpConnector;
import ru.jamsys.core.resource.http.client.HttpResponse;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class HttpResource extends AbstractExpirationResource {

    @SuppressWarnings("all")
    private final String ns;

    public HttpResource(String ns) {
        this.ns = ns;
    }

    public HttpResponse execute(HttpConnector arguments) {
        arguments.exec();
        return arguments.getResponseObject();
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void runOperation() {

    }

    @Override
    public void shutdownOperation() {

    }

    @Override
    public boolean checkFatalException(Throwable th) {
        return false;
    }

    @Override
    public List<DataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        return List.of();
    }

}
