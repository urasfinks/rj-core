package ru.jamsys.core.resource.http.client;


import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.UtilBase64;
import ru.jamsys.core.resource.virtual.file.system.FileKeyStoreSSLContext;

import java.net.Authenticator;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Accessors(chain = true)
public abstract class AbstractHttpConnector {

    private String proxyHost;

    private int proxyPort;

    private String proxyUser;

    private String proxyPasswordAlias;

    private String sslProtocol = "TLS";

    private Authenticator authenticator = null;

    private int connectTimeoutMs = 5_000;

    private int readTimeoutMs = 5_000;

    public HttpMethodEnum method = null;

    private final Map<String, String> headersRequest = new HashMap<>();

    private FileKeyStoreSSLContext fileKeyStoreSSLContext = null;

    private byte[] bodyRaw = null;

    private String url = null;

    boolean disableHostnameVerification = false;

    @JsonValue
    public HashMapBuilder<String, Object> getJsonValue() {
        return new HashMapBuilder<String, Object>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("class", getClass())
                .append("proxyHost", proxyHost)
                .append("proxyPort", proxyPort)
                .append("proxyUser", proxyUser)
                .append("proxyPasswordAlias", proxyPasswordAlias)
                .append("connectTimeoutMs", connectTimeoutMs)
                .append("readTimeoutMs", readTimeoutMs)
                .append("url", url)
                .append("method", method)
                .append("header", headersRequest)
                .append("raw", new String(bodyRaw))
                ;
    }

    @SuppressWarnings("all")
    public AbstractHttpConnector setProxy(String host, int port) {
        this.proxyHost = host;
        this.proxyPort = port;
        return this;
    }

    @SuppressWarnings("unused")
    public AbstractHttpConnector setProxy(String host, int port, String user, String password) {
        setProxy(host, port);
        this.proxyUser = user;
        this.proxyPasswordAlias = password;
        return this;
    }

    public AbstractHttpConnector addRequestHeader(String key, String value) {
        headersRequest.put(key, value);
        return this;
    }

    @SuppressWarnings("unused")
    public AbstractHttpConnector setBasicAuthorization(String user, String pass, String charset) {
        return addRequestHeader(
                "Authorization",
                "Basic " + UtilBase64.encode(user + ":" + pass, charset, false)
        );
    }

    public abstract HttpResponse exec();

}
