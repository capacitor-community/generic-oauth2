package com.getcapacitor.community.genericoauth2;

import android.content.Context;
import android.util.Log;
import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

public class MTLSHelper {
    private static final String TAG = "MTLSHelper";
    private static SSLContext sslContext;

    public static void configureMTLS(Context context, String rawPkcs, String pkcsPassword) {
        if (rawPkcs == null || rawPkcs.isEmpty()) {
            Log.d(TAG, "No certificate data provided, skipping mTLS configuration");
            return;
        }

        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            byte[] pkcsData = android.util.Base64.decode(rawPkcs, android.util.Base64.DEFAULT);
            keyStore.load(new ByteArrayInputStream(pkcsData), pkcsPassword.toCharArray());

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, pkcsPassword.toCharArray());

            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

            Log.d(TAG, "mTLS client certificate configured successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to configure mTLS", e);
            throw new RuntimeException("mTLS configuration failed", e);
        }
    }

    public static SSLContext getSSLContext() {
        return sslContext;
    }

    public static void resetSSLContext() {
        try {
            SSLContext defaultContext = SSLContext.getInstance("TLS");
            defaultContext.init(null, null, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(defaultContext.getSocketFactory());
            sslContext = null;
            Log.d(TAG, "SSL context reset to default");
        } catch (Exception e) {
            Log.e(TAG, "Failed to reset SSL context", e);
        }
    }
}