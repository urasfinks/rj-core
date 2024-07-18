package ru.jamsys.core.resource.http;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.ResourceArguments;
import ru.jamsys.core.resource.http.client.HttpClient;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.util.function.Function;

@Component
@Scope("prototype")
public class HttpResource extends ExpirationMsMutableImpl implements Resource<HttpClient, HttpResponse> {

    @Override
    public void setArguments(ResourceArguments resourceArguments) {

    }

    @Override
    public HttpResponse execute(HttpClient arguments) {
        arguments.exec();
        return arguments.getHttpResponseEnvelope();
    }

    @Override
    public Function<Throwable, Boolean> getFatalException() {
        return _ -> false;
    }

    @Override
    public void run() {

    }

    @Override
    public void shutdown() {

    }

}
