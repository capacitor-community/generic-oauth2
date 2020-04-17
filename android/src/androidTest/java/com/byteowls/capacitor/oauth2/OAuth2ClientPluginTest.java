package com.byteowls.capacitor.oauth2;

import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.byteowls.capacitor.oauth2.test.R;
import com.getcapacitor.JSObject;

import java.io.InputStream;

public class OAuth2ClientPluginTest {

    private OAuth2ClientPlugin plugin;

    @Before
    public void setup() {
        plugin = new OAuth2ClientPlugin();
    }

//    @Test
//    public void handleAuthorizationRequestActivity() {
////        plugin.handleAuthorizationRequestActivity();
//    }

    @Test
    public void responseTypeToken() {
        JSObject jsObject = loadJson(R.raw.response_type_token);
        OAuth2Options options = plugin.buildAuthenticateOptions(jsObject);
        Assert.assertNotNull(options);
        Assert.assertEquals("CLIENT_ID_ANDROID", options.getAppId());
        Assert.assertEquals("token", options.getResponseType().toLowerCase());
        Assert.assertNotNull(options.getHandleResultMethod());
    }

    @Test
    public void buildRefreshTokenOptions() {
        JSObject jsObject = loadJson(R.raw.refresh_token_config);
        OAuth2RefreshTokenOptions options = plugin.buildRefreshTokenOptions(jsObject);
        Assert.assertNotNull(options);
        Assert.assertNotNull(options.getAppId());
        Assert.assertNotNull(options.getAccessTokenEndpoint());
        Assert.assertNotNull(options.getRefreshToken());
        Assert.assertNotNull(options.getScope());
    }

    private JSObject loadJson(int resource) {
        try (InputStream in = getInstrumentation().getContext().getResources().openRawResource(resource)) {
            return new JSObject(IOUtils.toString(in, UTF_8));
        } catch (Exception e) {
            Log.e("OAuth2", "", e);
        }
        return null;
    }
}
