package com.aula.virtual.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import okhttp3.CipherSuite;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.TlsVersion;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_SERVER_URL = "server_url";

    public static final String EMULATOR_URL = "http://10.0.2.2:8000/";
    public static final String BASE_URL = "https://movies-men-bits-css.trycloudflare.com/";

    private static Retrofit retrofit = null;
    private static String activeUrl = null;

    public static String getActiveUrl(Context context) {
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String savedUrl = prefs.getString(KEY_SERVER_URL, null);

            // Auto-sync Cloudflare Tunnel URL: if savedUrl is null or an ephemeral trycloudflare.com URL,
            // always keep it synchronized with the latest BASE_URL updated by main.py
            if (savedUrl == null || savedUrl.trim().isEmpty() || savedUrl.contains("trycloudflare.com")) {
                if (savedUrl == null || !savedUrl.trim().equalsIgnoreCase(BASE_URL.trim())) {
                    prefs.edit().putString(KEY_SERVER_URL, BASE_URL).apply();
                }
                activeUrl = BASE_URL;
            } else {
                activeUrl = savedUrl.trim();
            }
        } else if (activeUrl == null) {
            activeUrl = BASE_URL;
        }

        if (!activeUrl.endsWith("/")) {
            activeUrl += "/";
        }
        return activeUrl;
    }

    public static String getActiveUrl() {
        return getActiveUrl(null);
    }

    public static void setCustomUrl(Context context, String customUrl) {
        if (customUrl != null && !customUrl.trim().isEmpty()) {
            String url = customUrl.trim();
            if (!url.endsWith("/")) url += "/";
            activeUrl = url;
            if (context != null) {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit().putString(KEY_SERVER_URL, activeUrl).apply();
            }
            retrofit = null;
        }
    }

    public static void setCustomUrl(String customUrl) {
        setCustomUrl(null, customUrl);
    }

    public static void resetClient() {
        retrofit = null;
    }

    public static ApiService getApiService() {
        return getApiService(null);
    }

    public static ApiService getApiService(Context context) {
        String currentUrl = getActiveUrl(context);
        if (retrofit == null || !retrofit.baseUrl().toString().equalsIgnoreCase(currentUrl)) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            Interceptor headerInterceptor = chain -> {
                Request original = chain.request();
                Request.Builder requestBuilder = original.newBuilder()
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile; AulaVirtual)")
                        .header("Accept", "application/json")
                        .header("Connection", "keep-alive")
                        .header("ngrok-skip-browser-warning", "true")
                        .header("Bypass-Tunnel-Remainder", "true");

                if (context != null) {
                    SharedPreferences prefs = context.getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE);
                    String sessionToken = prefs.getString("session_token", null);
                    if (sessionToken != null && !sessionToken.trim().isEmpty()) {
                        requestBuilder.header("Authorization", "Bearer " + sessionToken);
                        requestBuilder.header("X-Session-Token", sessionToken);
                    }
                }

                requestBuilder.method(original.method(), original.body());
                return chain.proceed(requestBuilder.build());
            };

            Interceptor unauthorizedInterceptor = chain -> {
                Request request = chain.request();
                okhttp3.Response response = chain.proceed(request);
                if (response.code() == 401 && context != null) {
                    String reason = "Tu sesión ha expirado o se ha iniciado sesión en otro dispositivo.";
                    context.getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .remove("session_token")
                            .putString("force_logout_reason", reason)
                            .apply();

                    android.content.Intent intent = new android.content.Intent(context, com.aula.virtual.MainActivity.class);
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra("force_logout_reason", reason);
                    context.startActivity(intent);
                }
                return response;
            };

            Interceptor cloudflareRetryInterceptor = chain -> {
                Request request = chain.request();
                okhttp3.Response response = null;
                boolean responseOK = false;
                int tryCount = 0;
                int maxLimit = 3;

                while (!responseOK && tryCount < maxLimit) {
                    try {
                        if (response != null) {
                            response.close();
                        }
                        response = chain.proceed(request);
                        if (response.isSuccessful() || (response.code() < 500 && response.code() != 429)) {
                            responseOK = true;
                        } else {
                            tryCount++;
                            if (tryCount < maxLimit) {
                                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                            }
                        }
                    } catch (Exception e) {
                        tryCount++;
                        if (tryCount >= maxLimit) {
                            if (e instanceof IOException) {
                                throw (IOException) e;
                            }
                            throw new IOException(e);
                        }
                        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                    }
                }
                return response != null ? response : chain.proceed(request);
            };

            ConnectionSpec tlsSpec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3)
                    .cipherSuites(
                            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                            CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
                            CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
                            CipherSuite.TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256,
                            CipherSuite.TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256
                    )
                    .build();

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectionPool(new ConnectionPool(5, 5, TimeUnit.SECONDS))
                    .connectionSpecs(Arrays.asList(tlsSpec, ConnectionSpec.CLEARTEXT))
                    .addInterceptor(cloudflareRetryInterceptor)
                    .addInterceptor(headerInterceptor)
                    .addInterceptor(unauthorizedInterceptor)
                    .addInterceptor(logging)
                    .connectTimeout(12, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .retryOnConnectionFailure(true)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(currentUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
