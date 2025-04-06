package ru.jamsys.core.resource.http;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.ResourceArguments;
import ru.jamsys.core.resource.http.client.HttpConnector;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImplAbstractLifeCycle;

import java.util.function.Function;

@Component
@Scope("prototype")
public class HttpResource extends ExpirationMsMutableImplAbstractLifeCycle implements Resource<HttpConnector, HttpResponse> {

    @Override
    public void setArguments(ResourceArguments resourceArguments) {

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
    public Function<Throwable, Boolean> getFatalException() {
        return _ -> false;
    }

    @Override
    public void runOperation() {

    }

    @Override
    public void shutdownOperation() {

    }

}
