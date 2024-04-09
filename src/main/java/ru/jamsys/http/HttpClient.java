package ru.jamsys.http;


import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import ru.jamsys.util.UtilBase64;
import ru.jamsys.virtual.file.system.File;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public interface HttpClient {

    @SuppressWarnings("unused")
    void setSslContextType(String sslContextType);

    @SuppressWarnings("unused")
    String getSslContextType();

    @SuppressWarnings("unused")
    void setProxy(Proxy proxy);

    @SuppressWarnings("unused")
    Proxy getProxy();

    @SuppressWarnings("unused")
    int getConnectTimeoutMillis();

    @SuppressWarnings("unused")
    void setConnectTimeoutMillis(int connectTimeoutMillis);

    @SuppressWarnings("unused")
    int getReadTimeoutMillis();

    @SuppressWarnings("unused")
    void setReadTimeoutMillis(int readTimeoutMillis);

    @SuppressWarnings("unused")
    boolean isDisableHostnameVerification();

    @SuppressWarnings("unused")
    void setDisableHostnameVerification(boolean disableHostnameVerification);

    @SuppressWarnings("unused")
    String getMethod();

    @SuppressWarnings("unused")
    void setMethod(String method);

    @SuppressWarnings("unused")
    Map<String, List<String>> getHeaderResponse();

    @SuppressWarnings("unused")
    void setKeyStore(File keyStore, Object... props) throws Exception;

    @SuppressWarnings("unused")
    byte[] getPostData();

    @SuppressWarnings("unused")
    void setPostData(byte[] postData);

    @SuppressWarnings("unused")
    void setUrl(String url);

    @SuppressWarnings("unused")
    String getUrl();

    @SuppressWarnings("unused")
    byte[] getResponse();

    @SuppressWarnings("unused")
    int getStatus();

    @SuppressWarnings("unused")
    Exception getException();

    @SuppressWarnings("unused")
    void setRequestHeader(String name, String value);

    @SuppressWarnings("unused")
    void exec();

    @SuppressWarnings("unused")
    String getResponseString(String charset) throws UnsupportedEncodingException;

    @SuppressWarnings("unused")
    default void setProxy(Proxy.Type type, String hostname, int port) {
        setProxy(new Proxy(type, new InetSocketAddress(hostname, port)));
    }

    @SuppressWarnings("unused")
    default void setProxy(String hostname, int port) {
        setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(hostname, port)));
    }

    @SuppressWarnings("unused")
    default void setBasicAuth(String user, String pass, String charset) {
        setRequestHeader("Authorization", "Basic " + UtilBase64.base64Encode(user + ":" + pass, charset, false));
    }

    default HttpResponseEnvelope getHttpResponseEnvelope(@Nullable HttpResponseEnvelope httpResponseEnvelope) {
        return getHttpResponseEnvelope(httpResponseEnvelope, StandardCharsets.UTF_8, false);
    }

    default HttpResponseEnvelope getHttpResponseEnvelope(@Nullable HttpResponseEnvelope httpResponseEnvelope, Charset standardCharsets, boolean forwardResponse) {
        HttpResponseEnvelope responseEnvelope = httpResponseEnvelope != null ? httpResponseEnvelope : new HttpResponseEnvelope();
        if (getException() != null) {
            responseEnvelope.addException(getException());
        }
        int status = getStatus();
        if (status == -1) {
            responseEnvelope.setHttpStatus(HttpStatus.EXPECTATION_FAILED);
        } else {
            responseEnvelope.setHttpStatus(HttpStatus.valueOf(status));
        }
        try {
            if (forwardResponse) {
                responseEnvelope.setRawBody(getResponseString(standardCharsets.toString()));
                Map<String, List<String>> headerResponse = getHeaderResponse();
                for (String key : headerResponse.keySet()) {
                    List<String> strings = headerResponse.get(key);
                    responseEnvelope.addHeader(key, String.join(";", strings));
                }
            } else {
                responseEnvelope.addData("httpResponseBody", getResponseString(standardCharsets.toString()));
                responseEnvelope.addData("httpResponseHeader", getHeaderResponse());
            }
        } catch (Exception e) {
            responseEnvelope.addException(e);
        }
        return responseEnvelope;
    }

}
