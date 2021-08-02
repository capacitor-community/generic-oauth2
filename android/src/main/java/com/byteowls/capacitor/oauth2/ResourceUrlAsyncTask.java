package com.byteowls.capacitor.oauth2;

import android.os.AsyncTask;
import android.util.Log;
import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;

import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.TokenResponse;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * @author m.oberwasserlechner@byteowls.com
 */
public class ResourceUrlAsyncTask extends AsyncTask<String, Void, ResourceCallResult> {

    private static final String ERR_GENERAL = "ERR_GENERAL";
    private static final String ERR_NO_ACCESS_TOKEN = "ERR_NO_ACCESS_TOKEN";
    private static final String MSG_RETURNED_TO_JS = "Returned to JS:\n";

    private final PluginCall pluginCall;
    private final OAuth2Options options;
    private final String logTag;
    private final AuthorizationResponse authorizationResponse;
    private final TokenResponse accessTokenResponse;

    public ResourceUrlAsyncTask(PluginCall pluginCall, OAuth2Options options, String logTag, AuthorizationResponse authorizationResponse, TokenResponse accessTokenResponse) {
        this.pluginCall = pluginCall;
        this.options = options;
        this.logTag = logTag;
        this.authorizationResponse = authorizationResponse;
        this.accessTokenResponse = accessTokenResponse;
    }

    @Override
    protected ResourceCallResult doInBackground(String... tokens) {
        ResourceCallResult result = new ResourceCallResult();

        String resourceUrl = options.getResourceUrl();
        String accessToken = tokens[0];
        if (resourceUrl != null) {
            Log.i(logTag, "Resource url: GET " + resourceUrl);
            if (accessToken != null) {
                Log.i(logTag, "Access token:\n" + accessToken);

                try {
                    URL url = new URL(resourceUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.addRequestProperty("Authorization", String.format("Bearer %s", accessToken));
                    // additional headers
                    if (options.getAdditionalResourceHeaders() != null) {
                        for (Map.Entry<String, String> entry : options.getAdditionalResourceHeaders().entrySet()) {
                            conn.addRequestProperty(entry.getKey(), entry.getValue());
                        }
                    }

                    InputStream is = null;
                    try {
                        if (conn.getResponseCode() >= HttpURLConnection.HTTP_OK
                            && conn.getResponseCode() < HttpURLConnection.HTTP_MULT_CHOICE) {
                            is = conn.getInputStream();
                        } else {
                            is = conn.getErrorStream();
                            result.setError(true);
                        }
                        String resourceResponseBody = readInputStream(is);
                        if (!result.isError()) {
                            JSObject resultJson = new JSObject(resourceResponseBody);
                            if (options.isLogsEnabled()) {
                                Log.i(logTag, "Resource response:\n" + resourceResponseBody);
                            }
                            OAuth2Utils.assignResponses(resultJson, accessToken, this.authorizationResponse, this.accessTokenResponse);
                            if (options.isLogsEnabled()) {
                                Log.i(logTag, MSG_RETURNED_TO_JS + resultJson);
                            }
                            result.setResponse(resultJson);
                        } else {
                            result.setErrorMsg(resourceResponseBody);
                        }
                    } catch (IOException e) {
                        Log.e(logTag, "", e);
                    } catch (JSONException e) {
                        Log.e(logTag, "Resource response no valid json.", e);
                    } finally {
                        conn.disconnect();
                        if (is != null) {
                            is.close();
                        }
                    }
                } catch (MalformedURLException e) {
                    Log.e(logTag, "Invalid resource url '" + resourceUrl + "'", e);
                } catch (IOException e) {
                    Log.e(logTag, "Unexpected error", e);
                }
            } else {
                if (options.isLogsEnabled()) {
                    Log.i(logTag, "No accessToken was provided although you configured a resourceUrl. Remove the resourceUrl from the config.");
                }
                pluginCall.reject(ERR_NO_ACCESS_TOKEN);
            }
        } else {
            JSObject json = new JSObject();
            OAuth2Utils.assignResponses(json, accessToken, this.authorizationResponse, this.accessTokenResponse);
            if (options.isLogsEnabled()) {
                Log.i(logTag, MSG_RETURNED_TO_JS + json);
            }
            result.setResponse(json);
        }
        return result;
    }

    @Override
    protected void onPostExecute(ResourceCallResult response) {
        if (response != null) {
            if (!response.isError()) {
                pluginCall.resolve(response.getResponse());
            } else {
                Log.e(logTag, response.getErrorMsg());
                pluginCall.reject(ERR_GENERAL, response.getErrorMsg());
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
