package com.byteowls.capacitor.oauth2;

import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

import com.byteowls.capacitor.oauth2.test.R;
import com.getcapacitor.JSObject;

import java.io.InputStream;

public class OAuth2ClientPluginTest {

    private OAuth2ClientPlugin plugin;

    @Before
    public void setup() {
         plugin = new OAuth2ClientPlugin();
    }


    @Test
    public void handleOnActivityResult() {

        plugin.handleOnActivityResult(OAuth2ClientPlugin.REQ_OAUTH_AUTHORIZATION, 0, null);


    }

    @Test
    public void buildAuthenticateOptions() {
        try (InputStream in = getInstrumentation().getContext().getResources().openRawResource(R.raw.config_no_resouce_url)) {
            JSObject jsObject = new JSObject(IOUtils.toString(in, UTF_8));
            OAuth2Options oAuth2Options = plugin.buildAuthenticateOptions(jsObject);
            Assert.assertNotNull(oAuth2Options);
        } catch (Exception e) {
            Log.e("OAuth2", "", e);
        }
    }

    @Test
    public void buildRefreshTokenOptions() {
        try (InputStream in = getInstrumentation().getContext().getResources().openRawResource(R.raw.config_no_resouce_url)) {
            JSObject jsObject = new JSObject(IOUtils.toString(in, UTF_8));
            OAuth2Options oAuth2Options = plugin.buildAuthenticateOptions(jsObject);
            Assert.assertNotNull(oAuth2Options);
        } catch (Exception e) {
            Log.e("OAuth2", "", e);
        }
    }
}
