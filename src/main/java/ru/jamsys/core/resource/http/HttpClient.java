package ru.jamsys.core.resource.http;


import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import ru.jamsys.core.resource.virtual.file.system.File;
import ru.jamsys.core.flat.util.UtilBase64;

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

    HttpClient setConnectTimeoutMs(int connectTimeoutMs);

    HttpClient setReadTimeoutMs(int readTimeoutMs);

    HttpClient setDisableHostnameVerification(boolean disableHostnameVerification);

    HttpClient setMethod(String method);

    HttpClient setKeyStore(File keyStore, Object... props) throws Exception;

    HttpClient setRequestHeader(String name, String value);

    HttpClient setPostData(byte[] postData);

    HttpClient setUrl(String url);

    default HttpClient setProxy(Proxy.Type type, String hostname, int port) {
        return setProxy(new Proxy(type, new InetSocketAddress(hostname, port)));
    }

    default HttpClient setProxy(String hostname, int port) {
        return setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(hostname, port)));
    }

    default HttpClient setBasicAuth(String user, String pass, String charset) {
        return setRequestHeader("Authorization", "Basic " + UtilBase64.base64Encode(user + ":" + pass, charset, false));
    }

    String getSslContextType();

    Proxy getProxy();

    int getConnectTimeoutMs();

    int getReadTimeoutMs();

    boolean isDisableHostnameVerification();

    String getMethod();

    Map<String, List<String>> getHeaderResponse();

    byte[] getPostData();

    String getUrl();

    byte[] getResponse();

    int getStatus();

    Exception getException();

    void exec();

    String getResponseString(String charset) throws UnsupportedEncodingException;

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
