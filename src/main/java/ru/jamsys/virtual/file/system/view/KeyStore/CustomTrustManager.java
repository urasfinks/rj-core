package ru.jamsys.virtual.file.system.view.KeyStore;

import lombok.Data;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

@Data
public class CustomTrustManager {

    private X509TrustManager trustManager = new X509TrustManager() {
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    };

    private X509TrustManager[] listTrustManager = {trustManager};

    private HostnameVerifier hostnameVerifier = (hostname, session) -> true;

}