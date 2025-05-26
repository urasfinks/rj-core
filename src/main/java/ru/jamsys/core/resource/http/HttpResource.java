package ru.jamsys.core.resource.http;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.http.client.HttpConnector;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.extension.expiration.mutable.ExpirationMsMutableImplAbstractLifeCycle;

@Component
@Scope("prototype")
public class HttpResource extends ExpirationMsMutableImplAbstractLifeCycle implements Resource<HttpConnector, HttpResponse> {

    @Override
    public void init(String ns) {

    }

    @Override
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

}
