package ru.jamsys.core.component.promise.api;

import lombok.Getter;
import ru.jamsys.core.resource.http.Http2ClientImpl;
import ru.jamsys.core.resource.http.HttpClient;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Getter
public class HttpClientPromise extends AbstractPromiseApi<HttpClientPromise> {

    final private HttpClient httpClient = new Http2ClientImpl();

    @Override
    public Consumer<AtomicBoolean> getExecutor() {
        return this::execute;
    }

    private void execute(AtomicBoolean isThreadRun) {
        httpClient.exec();
        setResult(httpClient.getHttpResponseEnvelope(null));
    }

}
