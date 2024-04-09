package ru.jamsys.http;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.virtual.file.system.view.FileViewKeyStoreSslSocketFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class HttpClientImpl implements HttpClient {

    @Getter
    @Setter
    private String SslContextType = "TLS";

    @Getter
    @Setter
    private Proxy proxy = null;

    @Getter
    @Setter
    private int connectTimeoutMillis = 10000;

    @Getter
    @Setter
    private int readTimeoutMillis = 10000;

    @Getter
    @Setter
    public boolean disableHostnameVerification = true;

    @Getter
    @Setter
    public String method = null;
    private final Map<String, String> headersRequest = new HashMap<>();

    @Getter
    private Map<String, List<String>> headerResponse = null;

    private FileViewKeyStoreSslSocketFactory socketFactory;

    @Getter
    @Setter
    @ToString.Exclude
    private byte[] postData = null;

    @Getter
    @Setter
    private String url = null;

    @Getter
    @ToString.Exclude
    private byte[] response = null;

    @Getter
    private int status = -1;

    @Getter
    private Exception exception = null;

    public void setRequestHeader(String name, String value) {
        headersRequest.put(name, value);
    }

    @SuppressWarnings("unused")
    public void exec() {
        try {
            URL url = new URL(this.url);
            HttpURLConnection httpURLConnection = (HttpURLConnection) (proxy == null ? url.openConnection() : url.openConnection(proxy));

            if (httpURLConnection instanceof HttpsURLConnection) {
                configureSsl((HttpsURLConnection) httpURLConnection);
            }

            httpURLConnection.setDoOutput(true);
            httpURLConnection.setConnectTimeout(connectTimeoutMillis);
            httpURLConnection.setReadTimeout(readTimeoutMillis);
            httpURLConnection.setUseCaches(false);

            for (Map.Entry<String, String> x : headersRequest.entrySet()) {
                httpURLConnection.setRequestProperty(x.getKey(), x.getValue());
            }
            if (method == null) {
                method = postData != null ? "POST" : "GET";
            }
            httpURLConnection.setRequestMethod(method);
            if (postData != null) {
                httpURLConnection.setDoOutput(true);
            }

            httpURLConnection.connect();
            if (postData != null) {
                OutputStream out = httpURLConnection.getOutputStream();
                out.write(postData);
                out.flush();
                out.close();
            }
            InputStream inputStream = getResult(httpURLConnection);
            status = httpURLConnection.getResponseCode();
            response = read(inputStream);
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (Exception e) {
            exception = e;
            e.printStackTrace();
        }
    }

    private InputStream getResult(HttpURLConnection httpURLConnection) {
        try {
            this.headerResponse = httpURLConnection.getHeaderFields();
            return httpURLConnection.getInputStream();
        } catch (Exception e) {
            try {
                return httpURLConnection.getErrorStream();
            } catch (Exception e2) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void configureSsl(HttpsURLConnection httpsURLConnection) {
        SSLSocketFactory sslSocketFactory;
        if (socketFactory != null) {
            sslSocketFactory = socketFactory.getSslSocketFactory(SslContextType);
            httpsURLConnection.setSSLSocketFactory(sslSocketFactory);
            if (disableHostnameVerification) {
                httpsURLConnection.setHostnameVerifier(socketFactory.getTrustManager().getHostnameVerifier());
            }
        }
    }

    private static byte[] read(InputStream inputStream) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (inputStream != null) {
            byte[] buf = new byte[1024];
            while (true) {
                int data = inputStream.read(buf);
                if (data <= 0) {
                    break;
                }
                out.write(buf, 0, data);
            }
        }
        return out.toByteArray();
    }

    @SuppressWarnings("unused")
    public String getResponseString(String charset) throws UnsupportedEncodingException {
        return new String(response, charset);
    }

    @Override
    public void setKeyStore(ru.jamsys.virtual.file.system.File keyStore, Object... props) throws Exception {
        socketFactory = keyStore.getView(FileViewKeyStoreSslSocketFactory.class, props);
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