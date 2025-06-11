package ru.jamsys.core.resource.http.client;

import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.util.UtilBase64;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

// Не работает с авторизацией прокси в случае обращения к https ресурсу
public class HttpConnectorDefault extends AbstractHttpConnector {

    @Override
    public ru.jamsys.core.resource.http.client.HttpResponse exec() {
        int status = -2;
        byte[] responseByte = null;
        Map<String, List<String>> headerResponse = new LinkedHashMap<>();
        Exception exception = null;

        long startTime = System.currentTimeMillis();
        try {
            if (method == null) {
                throw new ForwardException("Method is null", this);
            }
            HttpClient.Builder clientBuilder = HttpClient.newBuilder();
            if (getFileKeyStoreSSLContext() != null) {
                if (disableHostnameVerification) {
                    Properties props = System.getProperties();
                    props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
                }
                clientBuilder.sslContext(getFileKeyStoreSSLContext().get(getSslProtocol()));
            }

            if (getProxyHost() != null) {
                clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(getProxyHost(), getProxyPort())));
            }

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(new URI(this.getUrl()));
            if (getProxyUser() != null) {
                requestBuilder.setHeader(
                        "Proxy-Authorization",
                        "Basic " + UtilBase64.encode(
                                getProxyUser()
                                        + ":"
                                        + new String(App.get(SecurityComponent.class).get(getProxyPasswordAlias())),
                                false)
                );
            }
            for (Map.Entry<String, String> x : getHeadersRequest().entrySet()) {
                requestBuilder.setHeader(x.getKey(), x.getValue());
            }
            requestBuilder.method(
                    method.name(),
                    getBodyRaw() != null
                            ? HttpRequest.BodyPublishers.ofByteArray(getBodyRaw())
                            : HttpRequest.BodyPublishers.noBody()
            );
            requestBuilder.timeout(Duration.ofMillis(getConnectTimeoutMs() + getReadTimeoutMs()));
            try (HttpClient httpClient = clientBuilder.build()) {
                status = -1;
                HttpResponse<byte[]> responses = httpClient.send(
                        requestBuilder.build(),
                        HttpResponse.BodyHandlers.ofByteArray()
                );
                status = responses.statusCode();
                responseByte = responses.body();
                headerResponse = responses.headers().map();
            }
        } catch (Exception e) {
            exception = e;
            App.error(e);
        }
        return ru.jamsys.core.resource.http.client.HttpResponse.instanceOf(
                status,
                headerResponse,
                responseByte,
                exception,
                System.currentTimeMillis() - startTime
        );
    }

}
