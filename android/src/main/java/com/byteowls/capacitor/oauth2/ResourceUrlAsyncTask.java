package com.byteowls.capacitor.oauth2;

import android.os.AsyncTask;
import android.util.Log;
import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author m.oberwasserlechner@byteowls.com
 */
public class ResourceUrlAsyncTask extends AsyncTask<String, Void, ResourceCallResult> {

    private static final String ERR_GENERAL = "ERR_GENERAL";
    private PluginCall pluginCall;
    private OAuth2Options options;
    private String logTag;

    ResourceUrlAsyncTask(PluginCall pluginCall, OAuth2Options options, String logTag) {
        this.pluginCall = pluginCall;
        this.options = options;
        this.logTag = logTag;
    }
    private boolean isNullOrEmpty(String s) {
        return s == null || s.trim().length() == 0;
    }

    @Override
    protected ResourceCallResult doInBackground(String... tokens) {
        String resourceUrl = options.getResourceUrl();
        ResourceCallResult result = new ResourceCallResult();
        String accessToken = tokens[0];

        if (isNullOrEmpty(resourceUrl)) {
            JSObject json = new JSObject();
            json.put("access_token", accessToken);
            result.setResponse(json);
            return result;
        }

        try {
            URL url = new URL(resourceUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.addRequestProperty("Authorization", String.format("Bearer %s", accessToken));
            try {
                InputStream is;


                if (conn.getResponseCode() >= HttpURLConnection.HTTP_OK
                    && conn.getResponseCode() < HttpURLConnection.HTTP_MULT_CHOICE) {
                    is = conn.getInputStream();
                } else {
                    is = conn.getErrorStream();
                    result.setError(true);
                }
                String jsonBody = readInputStream(is);
                if (!result.isError()) {
                    JSObject json = new JSObject(jsonBody);
                    json.put("access_token", accessToken);
                    result.setResponse(json);
                } else {
                    result.setErrorMsg(jsonBody);
                }
                return result;
            } catch (IOException e) {
                Log.e(logTag, "", e);
            } catch (JSONException e) {
                Log.e(logTag, "Resource response no valid json.", e);
            } finally {
                conn.disconnect();
            }
        } catch (MalformedURLException e) {
            Log.e(logTag, "Invalid resource url '" + resourceUrl + "'", e);
        } catch (IOException e) {
            Log.e(logTag, "Unexpected error", e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(ResourceCallResult response) {
        if (response != null) {
            if (!response.isError()) {
                pluginCall.resolve(response.getResponse());
            } else {
                Log.e(logTag, response.getErrorMsg());
                pluginCall.reject(ERR_GENERAL);
            }
        } else {
            pluginCall.reject(ERR_GENERAL);
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
