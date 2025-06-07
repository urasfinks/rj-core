package ru.jamsys.core.resource.http.client;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
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
import ru.jamsys.core.resource.virtual.file.system.FileKeyStoreSSLContext;

import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Apache HttpClient был создан, потому что нативный клиент не умеет подключаться к прокси с авторизацией
// когда надо подключаться к https ресурсу, он теряет заголовок Proxy-Authorization
// в случает с http ресурсом всё ОК
// Apache клиент - работает с этим отлично, но меня пугает собственный пул ресурсов
// Предполагаю работать с Apache клиентом только в исключительных случаях

@Data
@Accessors(chain = true)
public class HttpConnectorApache implements HttpConnector {

    @Getter
    @Setter
    private long timing;

    @Getter
    @Setter
    private String proxyHost;

    @Getter
    @Setter
    private int proxyPort;

    @Getter
    @Setter
    private String proxyUser;

    @Setter
    private String proxyPassword;

    @Getter
    @Setter
    private String sslProtocol = "TLS";

    @Getter
    @Setter
    private Authenticator authenticator = null;

    @Getter
    @Setter
    private int connectTimeoutMs = 5_000;

    @Getter
    @Setter
    private int readTimeoutMs = 5_000;

    @Getter
    @Setter
    private int poolWaitMs = 5_000;

    @Getter
    @Setter
    public HttpMethodEnum method = null;

    private final Map<String, String> headersRequest = new HashMap<>();

    @Getter
    private Map<String, List<String>> headerResponse = null;

    private FileKeyStoreSSLContext fileKeyStoreSSLContext = null;

    @Getter
    @Setter
    @ToString.Exclude
    private byte[] postData = null;

    @Getter
    @Setter
    private String url = null;

    @Getter
    @ToString.Exclude
    private byte[] responseByte = null;

    @Getter
    private int status = -1;

    @Getter
    private Exception exception = null;

    @Getter
    @Setter
    boolean disableHostnameVerification = false;

    @Override
    public HttpConnectorApache addRequestHeader(String key, String value) {
        headersRequest.put(key, value);
        return this;
    }

    @Override
    public void exec() {
        long startTime = System.currentTimeMillis();
        try {
            HttpClientBuilder httpClientBuilder = HttpClients
                    .custom()
                    .setDefaultRequestConfig(RequestConfig.custom()
                            .setConnectTimeout(connectTimeoutMs)    // Таймаут подключения (5 секунд)
                            .setSocketTimeout(readTimeoutMs)     // Таймаут ожидания данных (5 секунд)
                            .setConnectionRequestTimeout(poolWaitMs) // Таймаут ожидания из пула соединений (5 секунд)
                            .build()
                    );
            if (fileKeyStoreSSLContext != null) {

                SSLConnectionSocketFactory socketFactory = disableHostnameVerification
                        ? new SSLConnectionSocketFactory(
                        fileKeyStoreSSLContext.get(sslProtocol),
                        NoopHostnameVerifier.INSTANCE
                )
                        : new SSLConnectionSocketFactory(
                        fileKeyStoreSSLContext.get(sslProtocol)
                );
                httpClientBuilder.setSSLSocketFactory(socketFactory);
            }
            if (proxyHost != null) {
                httpClientBuilder.setProxy(new HttpHost(proxyHost, proxyPort));
                if (proxyUser != null) {
                    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(
                            new AuthScope(proxyHost, proxyPort), // Указываем область авторизации
                            new UsernamePasswordCredentials(proxyUser, proxyPassword) // Учетные данные
                    );
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                }
            }

            try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
                if (method == null) {
                    method = postData != null ? HttpMethodEnum.POST : HttpMethodEnum.GET;
                }
                HttpRequestBase request = switch (method) {
                    case GET -> new HttpGet(url);
                    case DELETE -> new HttpDelete(url);
                    case POST -> {
                        HttpPost httpPost = new HttpPost(url);
                        httpPost.setEntity(new ByteArrayEntity(postData));
                        yield httpPost;
                    }
                    case PUT -> {
                        HttpPut httpPut = new HttpPut(url);
                        httpPut.setEntity(new ByteArrayEntity(postData));
                        yield httpPut;
                    }
                };
                for (Map.Entry<String, String> x : headersRequest.entrySet()) {
                    request.setHeader(x.getKey(), x.getValue());
                }

                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    status = response.getStatusLine().getStatusCode();
                    responseByte = response.getEntity().getContent().readAllBytes();
                    headerResponse = extractHeaders(response.getAllHeaders());
                }
            }
        } catch (Exception e) {
            exception = e;
            App.error(e);
        }
        setTiming(System.currentTimeMillis() - startTime);
    }

    private static Map<String, List<String>> extractHeaders(Header[] headers) {
        Map<String, List<String>> headersMap = new HashMap<>();
        for (Header header : headers) {
            headersMap.computeIfAbsent(header.getName(), _ -> new ArrayList<>()).add(header.getValue());
        }
        return headersMap;
    }

    @Override
    public String getResponseString(String charset) throws UnsupportedEncodingException {
        if (responseByte == null) {
            return "";
        }
        return new String(responseByte, charset);
    }

    @Override
    public HttpConnector setProxy(String host, int port) {
        this.proxyHost = host;
        this.proxyPort = port;
        return this;
    }

    @Override
    public HttpConnector setProxy(String host, int port, String user, String password) {
        this.proxyHost = host;
        this.proxyPort = port;
        this.proxyUser = user;
        this.proxyPassword = password;
        return this;
    }

    @Override
    public HttpConnectorApache setKeyStore(FileKeyStoreSSLContext fileKeyStoreSSLContext) {
        this.fileKeyStoreSSLContext = fileKeyStoreSSLContext;
        return this;
    }

    @SuppressWarnings("unused")
    @ToString.Include()
    public String getResponseString() {
        return responseByte != null ? new String(responseByte, StandardCharsets.UTF_8) : "null";
    }

    @SuppressWarnings("unused")
    @ToString.Include()
    public String getPostDataString() {
        return postData != null ? new String(postData, StandardCharsets.UTF_8) : "null";
    }

}
