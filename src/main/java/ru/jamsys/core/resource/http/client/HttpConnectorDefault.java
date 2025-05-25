package ru.jamsys.core.resource.http.client;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import ru.jamsys.core.App;
import ru.jamsys.core.flat.util.UtilBase64;
import ru.jamsys.core.resource.virtual.file.system.FileKeyStoreSSLContext;

import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

// Не работает с авторизацией прокси в случае обращения к https ресурсу

@Data
@Accessors(chain = true)
public class HttpConnectorDefault implements HttpConnector {

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
    public HttpConnectorDefault setRequestHeader(String name, String value) {
        headersRequest.put(name, value);
        return this;
    }

    @Override
    public void exec() {
        long startTime = System.currentTimeMillis();
        try {
            HttpClient.Builder clientBuilder = HttpClient.newBuilder();

            if (fileKeyStoreSSLContext != null) {
                if (disableHostnameVerification) {
                    Properties props = System.getProperties();
                    props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
                }
                clientBuilder.sslContext(fileKeyStoreSSLContext.get(sslProtocol));
            }

            if (proxyHost != null) {
                clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(proxyHost, proxyPort)));
            }

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(new URI(this.url));
            if (proxyUser != null) {
                requestBuilder.setHeader(
                        "Proxy-Authorization",
                        "Basic " + UtilBase64.encode(proxyUser + ":" + proxyPassword, false)
                );
            }
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
            requestBuilder.timeout(Duration.ofMillis(connectTimeoutMs + readTimeoutMs));
            try (HttpClient httpClient = clientBuilder.build()) {
                HttpResponse<byte[]> responses = httpClient.send(
                        requestBuilder.build(),
                        HttpResponse.BodyHandlers.ofByteArray()
                );
                status = responses.statusCode();
                responseByte = responses.body();
                this.headerResponse = responses.headers().map();
            }

        } catch (Exception e) {
            exception = e;
            App.error(e);
        }
        setTiming(System.currentTimeMillis() - startTime);
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
    public HttpConnectorDefault setKeyStore(FileKeyStoreSSLContext fileKeyStoreSSLContext) {
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
