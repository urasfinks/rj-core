package ru.jamsys.core.resource.http.client;


import org.springframework.http.HttpStatus;
import ru.jamsys.core.flat.util.UtilBase64;
import ru.jamsys.core.resource.virtual.file.system.File;
import ru.jamsys.core.resource.virtual.file.system.FileKeyStoreSSLContext;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public interface HttpConnector {

    HttpConnector setSslProtocol(String sslProtocol);

    HttpConnector setProxy(String host, int port);

    HttpConnector setProxy(String host, int port, String user, String password);

    HttpConnector setConnectTimeoutMs(int connectTimeoutMs);

    HttpConnector setReadTimeoutMs(int connectTimeoutMs);

    HttpConnector setMethod(HttpMethodEnum method);

    HttpConnector setKeyStore(FileKeyStoreSSLContext fileKeyStoreSSLContext);

    HttpConnector setRequestHeader(String name, String value);

    HttpConnector setPostData(byte[] postData);

    HttpConnector setUrl(String url);

    default HttpConnector setBasicAuth(String user, String pass, String charset) {
        return setRequestHeader("Authorization", "Basic " + UtilBase64.encode(user + ":" + pass, charset, false));
    }

    String getSslProtocol();

    int getConnectTimeoutMs();

    long getTiming(); // Время исполнения запроса

    HttpMethodEnum getMethod();

    Map<String, List<String>> getHeaderResponse();

    byte[] getPostData();

    String getUrl();

    byte[] getResponseByte();

    int getStatus();

    Exception getException();

    void exec();

    String getResponseString(String charset) throws UnsupportedEncodingException;

    default HttpResponse getResponseObject() {
        return getResponseObject(StandardCharsets.UTF_8);
    }

    default HttpResponse getResponseObject(Charset standardCharsets) {
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
