package ru.jamsys.core.extension;

import lombok.Getter;
import lombok.Setter;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

@Getter
@Setter
public class CustomTrustManager {

    private X509TrustManager trustManager = new X509TrustManager() {
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[0];
        }
    };

    private X509TrustManager[] listTrustManager = {trustManager};

    private HostnameVerifier hostnameVerifier = (_, _) -> true;

}