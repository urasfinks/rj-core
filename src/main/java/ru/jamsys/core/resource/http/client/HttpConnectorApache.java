package ru.jamsys.core.resource.http.client;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.extension.exception.ForwardException;

import java.net.Authenticator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Apache HttpClient был создан, потому что нативный клиент не умеет подключаться к прокси с авторизацией,
// когда надо подключаться к https ресурсу, он теряет заголовок Proxy-Authorization.
// Apache клиент - работает с этим отлично, но меня пугает собственный пул ресурсов.
// Предполагаю работать с Apache клиентом только в исключительных случаях.

@Getter
@Setter
@Accessors(chain = true)
public class HttpConnectorApache extends AbstractHttpConnector {

    private Authenticator authenticator = null;

    private int poolWaitMs = 5_000;

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
            HttpClientBuilder httpClientBuilder = HttpClients
                    .custom()
                    .setDefaultRequestConfig(RequestConfig.custom()
                            .setConnectTimeout(getConnectTimeoutMs())    // Таймаут подключения (5 секунд)
                            .setSocketTimeout(getReadTimeoutMs())     // Таймаут ожидания данных (5 секунд)
                            .setConnectionRequestTimeout(poolWaitMs) // Таймаут ожидания из пула соединений (5 секунд)
                            .build()
                    );
            if (getFileKeyStoreSSLContext() != null) {
                SSLConnectionSocketFactory socketFactory = disableHostnameVerification
                        ? new SSLConnectionSocketFactory(
                        getFileKeyStoreSSLContext().get(getSslProtocol()),
                        NoopHostnameVerifier.INSTANCE
                )
                        : new SSLConnectionSocketFactory(
                        getFileKeyStoreSSLContext().get(getSslProtocol())
                );
                httpClientBuilder.setSSLSocketFactory(socketFactory);
            }
            if (getProxyHost() != null) {
                httpClientBuilder.setProxy(new HttpHost(getProxyHost(), getProxyPort()));
                if (getProxyUser() != null) {
                    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(
                            new AuthScope(getProxyHost(), getProxyPort()), // Указываем область авторизации
                            new UsernamePasswordCredentials(
                                    getProxyUser(),
                                    new String(App.get(SecurityComponent.class).get(getProxyPasswordAlias()))
                            ) // Учетные данные
                    );
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                }
            }

            try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
                if (method == null) {
                    method = getBodyRaw() != null ? HttpMethodEnum.POST : HttpMethodEnum.GET;
                }
                HttpRequestBase request = switch (method) {
                    case GET -> new HttpGet(getUrl());
                    case DELETE -> new HttpDelete(getUrl());
                    case POST -> {
                        HttpPost httpPost = new HttpPost(getUrl());
                        httpPost.setEntity(new ByteArrayEntity(getBodyRaw()));
                        yield httpPost;
                    }
                    case PUT -> {
                        HttpPut httpPut = new HttpPut(getUrl());
                        httpPut.setEntity(new ByteArrayEntity(getBodyRaw()));
                        yield httpPut;
                    }
                    case HEAD -> new HttpHead(getUrl());
                    case OPTIONS -> new HttpOptions(getUrl());
                    case PATCH -> new HttpPatch(getUrl());
                    case TRACE -> new HttpTrace(getUrl());
                    case CONNECT -> throw new ForwardException("Connect not support in Apache Http Client", this);
                };
                for (Map.Entry<String, String> x : getHeadersRequest().entrySet()) {
                    request.setHeader(x.getKey(), x.getValue());
                }
                status = -1;
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    status = response.getStatusLine().getStatusCode();
                    responseByte = response.getEntity().getContent().readAllBytes();
                    for (Header header : response.getAllHeaders()) {
                        headerResponse.computeIfAbsent(header.getName(), _ -> new ArrayList<>()).add(header.getValue());
                    }
                }
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
