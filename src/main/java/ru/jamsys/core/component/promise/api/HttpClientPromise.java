package ru.jamsys.core.component.promise.api;

import lombok.Getter;
import ru.jamsys.core.resource.http.Http2ClientImpl;
import ru.jamsys.core.resource.http.HttpClient;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Пример использования
 *
 * <p>
 * <pre> {@code
*        Promise wf = new PromiseImpl("test");
*        wf.api("request", new HttpClientPromise().setup((HttpClientPromise httpClientPromise) -> {
*            httpClientPromise.getHttpClient()
*                    .setConnectTimeoutMs(1000)
*                    .setReadTimeoutMs(1000)
*                    .setRequestHeader("x", "Y")
*                    .setBasicAuth("user", "password", "utf-8")
*                    .setPostData("Hello world".getBytes(StandardCharsets.UTF_8));
*        }).beforeExecute((HttpClientPromise httpClientPromise) -> {
*            httpClientPromise.getHttpClient().setUrl("http://yandex.ru");
*        })).run().await(10000);
*        System.out.println(wf.getLog());
 * }</pre>
 */

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
