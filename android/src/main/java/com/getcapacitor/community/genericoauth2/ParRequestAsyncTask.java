package com.getcapacitor.community.genericoauth2;

import android.os.AsyncTask;
import android.util.Log;
import com.getcapacitor.PluginCall;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONObject;

class ParRequestAsyncTask extends AsyncTask<Void, Void, ParRequestResult> {

    private static final String ERR_PAR_FAILED = "ERR_PAR_FAILED";

    private final PluginCall pluginCall;
    private final OAuth2Options options;
    private final String logTag;
    private final GenericOAuth2Plugin plugin;

    ParRequestAsyncTask(PluginCall pluginCall, OAuth2Options options, String logTag, GenericOAuth2Plugin plugin) {
        this.pluginCall = pluginCall;
        this.options = options;
        this.logTag = logTag;
        this.plugin = plugin;
    }

    @Override
    protected ParRequestResult doInBackground(Void... voids) {
        ParRequestResult result = new ParRequestResult();

        String parEndpoint = options.getParEndpoint();
        if (parEndpoint == null) {
            return result;
        }

        try {
            URL url = new URL(parEndpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");

            Map<String, String> params = new LinkedHashMap<>();
            params.put("client_id", options.getAppId());
            params.put("response_type", options.getResponseType());
            if (options.getRedirectUrl() != null) {
                params.put("redirect_uri", options.getRedirectUrl());
            }
            if (options.getScope() != null) {
                params.put("scope", options.getScope());
            }
            if (options.getState() != null) {
                params.put("state", options.getState());
            }

            if (options.isPkceEnabled() && options.getPkceCodeVerifier() != null) {
                String challenge = ConfigUtils.derivePkceCodeChallenge(options.getPkceCodeVerifier());
                params.put("code_challenge", challenge);
                params.put("code_challenge_method", "S256");
            }

            if (options.getAdditionalParameters() != null) {
                for (Map.Entry<String, String> entry : options.getAdditionalParameters().entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (key != null && key.trim().length() > 0 && value != null && value.trim().length() > 0) {
                        if (!params.containsKey(key)) {
                            params.put(key, value);
                        }
                    }
                }
            }

            StringBuilder bodyBuilder = new StringBuilder();
            try {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if (bodyBuilder.length() > 0) {
                        bodyBuilder.append('&');
                    }
                    bodyBuilder
                        .append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                        .append('=')
                        .append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                }
            } catch (Exception e) {
                Log.e(logTag, "Error encoding PAR parameters", e);
            }

            String body = bodyBuilder.toString();
            if (options.isLogsEnabled()) {
                Log.i(logTag, "PAR request: POST " + parEndpoint);
            }

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes("UTF-8"));
            }

            InputStream is = null;
            String responseBody = null;
            try {
                if (conn.getResponseCode() >= HttpURLConnection.HTTP_OK && conn.getResponseCode() < HttpURLConnection.HTTP_MULT_CHOICE) {
                    is = conn.getInputStream();
                } else {
                    is = conn.getErrorStream();
                }
                responseBody = readInputStream(is);
            } finally {
                if (is != null) {
                    is.close();
                }
                conn.disconnect();
            }

            if (conn.getResponseCode() >= HttpURLConnection.HTTP_OK && conn.getResponseCode() < HttpURLConnection.HTTP_MULT_CHOICE) {
                try {
                    JSONObject json = new JSONObject(responseBody != null ? responseBody : "{}");
                    String requestUri =
                        json.optString("request_uri", null) != null
                            ? json.optString("request_uri", null)
                            : (json.optString("requestUri", null) != null ? json.optString("requestUri", null) : json.optString("request-uri", null));
                    if (requestUri == null || requestUri.trim().length() == 0) {
                        result.setError(true);
                        result.setErrorMsg("PAR_FAILED: missing request_uri in response");
                    } else {
                        result.setRequestUri(requestUri);
                    }
                } catch (Exception e) {
                    Log.e(logTag, "PAR response no valid json.", e);
                    result.setError(true);
                    result.setErrorMsg("PAR_FAILED: invalid JSON response");
                }
            } else {
                String message = "PAR_FAILED: HTTP " + conn.getResponseCode();
                try {
                    JSONObject json = new JSONObject(responseBody != null ? responseBody : "{}");
                    if (json.has("error")) {
                        message += " " + json.optString("error");
                        if (json.has("error_description")) {
                            message += " - " + json.optString("error_description");
                        }
                    }
                } catch (Exception ignore) {}
                result.setError(true);
                result.setErrorMsg(message);
            }
        } catch (MalformedURLException e) {
            Log.e(logTag, "Invalid PAR endpoint url '" + options.getParEndpoint() + "'", e);
            result.setError(true);
            result.setErrorMsg("PAR_FAILED: invalid PAR endpoint url");
        } catch (IOException e) {
            Log.e(logTag, "Unexpected error during PAR request", e);
            result.setError(true);
            result.setErrorMsg("PAR_FAILED: network error");
        }

        return result;
    }

    @Override
    protected void onPostExecute(ParRequestResult result) {
        if (result != null && !result.isError()) {
            options.setParRequestUri(result.getRequestUri());
            plugin.startAuthorization(pluginCall);
        } else if (result != null) {
            Log.e(logTag, result.getErrorMsg());
            pluginCall.reject(ERR_PAR_FAILED, result.getErrorMsg());
        } else {
            pluginCall.reject(ERR_PAR_FAILED);
        }
    }

    private static String readInputStream(InputStream in) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            char[] buffer = new char[1024];
            StringBuilder sb = new StringBuilder();
            int readCount;
            while ((readCount = br.read(buffer)) != -1) {
                sb.append(buffer, 0, readCount);
            }
            return sb.toString();
        }
    }
}

