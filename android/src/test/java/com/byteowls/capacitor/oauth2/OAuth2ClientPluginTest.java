package com.byteowls.capacitor.oauth2;

import android.util.Log;

import com.getcapacitor.JSObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OAuth2ClientPluginTest {

    public static final String CLIENT_ID_ANDROID = "CLIENT_ID_ANDROID";
    private OAuth2ClientPlugin plugin;

    @BeforeEach
    public void setup() {
        plugin = new OAuth2ClientPlugin();
    }

    @Test
    public void allBooleanValues() {
        JSObject jsObject = loadJson("{\n" +
            "    \"appId\": \"CLIENT_ID\",\n" +
            "    \"authorizationBaseUrl\": \"https://accounts.google.com/o/oauth2/auth\",\n" +
            "    \"accessTokenEndpoint\": \"https://www.googleapis.com/oauth2/v4/token\",\n" +
            "    \"scope\": \"email profile\",\n" +
            "    \"pkceEnabled\": true,\n" +
            "    \"logsEnabled\": true,\n" +
            "    \"resourceUrl\": \"https://www.googleapis.com/userinfo/v2/me\",\n" +
            "    \"web\": {\n" +
            "        \"redirectUrl\": \"http://localhost:4200\",\n" +
            "        \"windowOptions\": \"height=600,left=0,top=0\"\n" +
            "    },\n" +
            "    \"android\": {\n" +
            "        \"appId\": \"" + CLIENT_ID_ANDROID + "\",\n" +
            "        \"redirectUrl\": \"com.company.project:/\",\n" +
            "        \"handleResultMethod\": \"TEST\",\n" +
            "        \"logsEnabled\": false,\n" +
            "        \"handleResultOnNewIntent\": true,\n" +
            "        \"handleResultOnActivityResult\": false,\n" +
            "        \"responseType\": \"TOKEN\"\n" +
            "    },\n" +
            "    \"ios\": {\n" +
            "        \"appId\":  \"CLIENT_ID_IOS\",\n" +
            "        \"responseType\": \"code\",\n" +
            "        \"redirectUrl\": \"com.company.project:/\"\n" +
            "    }\n" +
            "}\n");
        OAuth2Options options = plugin.buildAuthenticateOptions(jsObject);
        Assertions.assertNotNull(options);
        Assertions.assertTrue(options.isPkceEnabled());
        Assertions.assertFalse(options.isLogsEnabled());
        Assertions.assertTrue(options.isHandleResultOnNewIntent());
        Assertions.assertFalse(options.isHandleResultOnActivityResult());
    }

    @Test
    public void responseTypeToken() {
        JSObject jsObject = loadJson("{\n" +
            "    \"appId\": \"CLIENT_ID\",\n" +
            "    \"authorizationBaseUrl\": \"https://accounts.google.com/o/oauth2/auth\",\n" +
            "    \"accessTokenEndpoint\": \"https://www.googleapis.com/oauth2/v4/token\",\n" +
            "    \"scope\": \"email profile\",\n" +
            "    \"pkceEnabled\": true,\n" +
            "    \"resourceUrl\": \"https://www.googleapis.com/userinfo/v2/me\",\n" +
            "    \"web\": {\n" +
            "        \"redirectUrl\": \"http://localhost:4200\",\n" +
            "        \"windowOptions\": \"height=600,left=0,top=0\"\n" +
            "    },\n" +
            "    \"android\": {\n" +
            "        \"appId\": \"" + CLIENT_ID_ANDROID + "\",\n" +
            "        \"redirectUrl\": \"com.company.project:/\",\n" +
            "        \"handleResultMethod\": \"TEST\",\n" +
            "        \"responseType\": \"TOKEN\"\n" +
            "    },\n" +
            "    \"ios\": {\n" +
            "        \"appId\":  \"CLIENT_ID_IOS\",\n" +
            "        \"responseType\": \"code\",\n" +
            "        \"redirectUrl\": \"com.company.project:/\"\n" +
            "    }\n" +
            "}\n");
        OAuth2Options options = plugin.buildAuthenticateOptions(jsObject);
        Assertions.assertNotNull(options);
        Assertions.assertEquals(CLIENT_ID_ANDROID, options.getAppId());
        Assertions.assertEquals("token", options.getResponseType().toLowerCase());
        Assertions.assertTrue(options.isHandleResultOnActivityResult());
    }

    @Test
    public void serverAuthorizationHandling() {
        JSObject jsObject = loadJson("{\n" +
            "    \"appId\": \"CLIENT_ID\",\n" +
            "    \"authorizationBaseUrl\": \"https://accounts.google.com/o/oauth2/auth\",\n" +
            "    \"responseType\": \"code id_token\",\n" +
            "    \"redirectUrl\": \"https://project.myserver.com/oauth\",\n" +
            "    \"resourceUrl\": \"https://www.googleapis.com/userinfo/v2/me\",\n" +
            "    \"scope\": \"email profile\",\n" +
            "    \"web\": {\n" +
            "        \"windowOptions\": \"height=600,left=0,top=0\"\n" +
            "    },\n" +
            "    \"android\": {\n" +
            "        \"appId\": \"" + CLIENT_ID_ANDROID + "\"\n" +
            "    },\n" +
            "    \"ios\": {\n" +
            "        \"appId\":  \"CLIENT_ID_IOS\"\n" +
            "    }\n" +
            "}\n");
        OAuth2Options options = plugin.buildAuthenticateOptions(jsObject);
        Assertions.assertNotNull(options.getAppId());
        Assertions.assertEquals(CLIENT_ID_ANDROID, options.getAppId());
        Assertions.assertNotNull(options.getAuthorizationBaseUrl());
        Assertions.assertEquals("code id_token", options.getResponseType());
        Assertions.assertNotNull(options.getRedirectUrl());
    }

    @Test
    public void buildRefreshTokenOptions() {
        JSObject jsObject = loadJson("{\n" +
            "    \"appId\": \"CLIENT_ID\",\n" +
            "    \"accessTokenEndpoint\": \"https://www.googleapis.com/oauth2/v4/token\",\n" +
            "    \"refreshToken\": \"ss4f6sd5f4\",\n" +
            "    \"scope\": \"email profile\"\n" +
            "}");
        OAuth2RefreshTokenOptions options = plugin.buildRefreshTokenOptions(jsObject);
        Assertions.assertNotNull(options);
        Assertions.assertNotNull(options.getAppId());
        Assertions.assertNotNull(options.getAccessTokenEndpoint());
        Assertions.assertNotNull(options.getRefreshToken());
        Assertions.assertNotNull(options.getScope());
    }

    private JSObject loadJson(String json) {
        try {
            return new JSObject(json);
        } catch (Exception e) {
            Log.e("OAuth2", "", e);
        }
        return null;
    }
}
