package ru.jamsys.core.resource.http.client;

import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.resource.virtual.file.system.File;
import ru.jamsys.core.resource.virtual.file.system.view.FileViewKeyStoreSslContext;

import java.io.UnsupportedEncodingException;
import java.net.Proxy;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class HttpClientImpl implements HttpClient {

    @Getter
    private String SslContextType = "TLS";

    //TODO: надо использовать в реализации
    @Getter
    private Proxy proxy = null;

    @Getter
    private int timeoutMs = 10000;

    @Getter
    public boolean disableHostnameVerification = false;

    @Getter
    public HttpMethodEnum method = null;

    private final Map<String, String> headersRequest = new HashMap<>();

    @Getter
    private Map<String, List<String>> headerResponse = null;

    private FileViewKeyStoreSslContext sslContext = null;

    @Getter
    @ToString.Exclude
    private byte[] postData = null;

    @Getter
    private String url = null;

    @Getter
    @ToString.Exclude
    private byte[] response = null;

    @Getter
    private int status = -1;

    @Getter
    private Exception exception = null;

    @Override
    public HttpClient setUrl(String url) {
        this.url = url;
        return this;
    }

    @Override
    public HttpClient setRequestHeader(String name, String value) {
        headersRequest.put(name, value);
        return this;
    }

    @Override
    public HttpClient setPostData(byte[] postData) {
        this.postData = postData;
        return this;
    }

    @Override
    public HttpClient setMethod(HttpMethodEnum method) {
        this.method = method;
        return this;
    }

    @Override
    public HttpClient setDisableHostnameVerification(boolean disableHostnameVerification) {
        //TODO: надо использовать в реализации
        this.disableHostnameVerification = disableHostnameVerification;
        return this;
    }

    @Override
    public HttpClient setTimeoutMs(int connectTimeoutMs) {
        this.timeoutMs = connectTimeoutMs;
        return this;
    }

    @Override
    public HttpClient setProxy(Proxy proxy) {
        this.proxy = proxy;
        return this;
    }

    @Override
    public HttpClient setSslContextType(String sslContextType) {
        this.SslContextType = sslContextType;
        return this;
    }

    @Override
    public void exec() {
        try {
            java.net.http.HttpClient.Builder clientBuilder = java.net.http.HttpClient.newBuilder();
            if (sslContext != null) {
                clientBuilder.sslContext(sslContext.getSslContext(SslContextType));
            }

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(new URI(this.url));
            for (Map.Entry<String, String> x : headersRequest.entrySet()) {
                requestBuilder.setHeader(x.getKey(), x.getValue());
            }
            if (method == null) {
                method = postData != null ? HttpMethodEnum.POST : HttpMethodEnum.GET;
            }
            switch (method) {
                case GET -> requestBuilder.GET();
                case POST -> requestBuilder.method(method.name(), HttpRequest.BodyPublishers.ofByteArray(postData));
                case PUT -> requestBuilder.PUT(HttpRequest.BodyPublishers.ofByteArray(postData));
                case DELETE -> requestBuilder.DELETE();
            }
            requestBuilder.timeout(Duration.ofMillis(timeoutMs));
            try (java.net.http.HttpClient hc = clientBuilder.build()) {
                HttpResponse<byte[]> responses = hc.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
                status = responses.statusCode(); // Return the status code, if it is 200, it means the sending is successful
                response = responses.body();
                this.headerResponse = responses.headers().map();
            }

        } catch (Exception e) {
            exception = e;
            App.context.getBean(ExceptionHandler.class).handler(e);
        }
    }

    @Override
    public String getResponseString(String charset) throws UnsupportedEncodingException {
        return new String(response, charset);
    }

    @Override
    public HttpClient setKeyStore(File keyStore, Object... props) {
        sslContext = keyStore.getView(FileViewKeyStoreSslContext.class, props);
        return this;
    }

    @SuppressWarnings("unused")
    @ToString.Include()
    public String getResponseString() {
        return response != null ? new String(response, StandardCharsets.UTF_8) : "null";
    }

    @SuppressWarnings("unused")
    @ToString.Include()
    public String getPostDataString() {
        return postData != null ? new String(postData, StandardCharsets.UTF_8) : "null";
    }

}
