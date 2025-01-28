package ru.jamsys.core.resource.http.client;


import org.springframework.http.HttpStatus;
import ru.jamsys.core.flat.util.UtilBase64;
import ru.jamsys.core.resource.virtual.file.system.File;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public interface HttpClient {

    HttpClient setSslContextType(String sslContextType);

    HttpClient setProxy(Proxy proxy);

    HttpClient setTimeoutMs(int connectTimeoutMs);

    HttpClient setMethod(HttpMethodEnum method);

    HttpClient setKeyStore(File keyStore, Object... props);

    HttpClient putRequestHeader(String name, String value);

    HttpClient setPostData(byte[] postData);

    HttpClient setUrl(String url);

    default HttpClient setProxy(Proxy.Type type, String hostname, int port) {
        return setProxy(new Proxy(type, new InetSocketAddress(hostname, port)));
    }

    default HttpClient setProxy(String hostname, int port) {
        return setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(hostname, port)));
    }

    default HttpClient setBasicAuth(String user, String pass, String charset) {
        return putRequestHeader("Authorization", "Basic " + UtilBase64.encode(user + ":" + pass, charset, false));
    }

    String getSslContextType();

    Proxy getProxy();

    int getTimeoutMs();

    long getTiming(); // Время исполнения запроса

    HttpMethodEnum getMethod();

    Map<String, List<String>> getHeaderResponse();

    byte[] getPostData();

    String getUrl();

    byte[] getResponse();

    int getStatus();

    Exception getException();

    void exec();

    String getResponseString(String charset) throws UnsupportedEncodingException;

    default HttpResponse getHttpResponse() {
        return getHttpResponse(StandardCharsets.UTF_8);
    }

    default HttpResponse getHttpResponse(Charset standardCharsets) {
        HttpResponse httpResponse = new HttpResponse();
        if (getException() != null) {
            httpResponse.addException(getException());
        }
        int status = getStatus();
        httpResponse.setStatusCode(status);
        if (status == -1) {
            httpResponse.addException("Запроса не было");
        } else {
            httpResponse.setStatusDesc(HttpStatus.valueOf(status));
        }
        if (httpResponse.getException() == null) {
            try {
                httpResponse.setBody(getResponseString(standardCharsets.toString()));
                Map<String, List<String>> headerResponse = getHeaderResponse();
                if (headerResponse != null) {
                    for (String key : headerResponse.keySet()) {
                        List<String> strings = headerResponse.get(key);
                        httpResponse.addHeader(key, String.join(";", strings));
                    }
                }
            } catch (Exception e) {
                httpResponse.addException(e);
            }
        }
        httpResponse.setTiming(getTiming());
        return httpResponse;
    }

}
