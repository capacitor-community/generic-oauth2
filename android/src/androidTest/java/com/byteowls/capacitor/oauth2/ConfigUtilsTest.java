package com.byteowls.capacitor.oauth2;

import android.util.Log;

import com.getcapacitor.JSObject;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Map;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.byteowls.capacitor.oauth2.test.R;

public class ConfigUtilsTest {

    private JSObject jsObject;

    @Before
    public void setUp() {
        try (InputStream in = getInstrumentation().getContext().getResources().openRawResource(R.raw.config_utils_features)) {
            this.jsObject = new JSObject(IOUtils.toString(in, UTF_8));
        } catch (Exception e) {
            Log.e("OAuth2", "", e);
        }
    }

    @Test
    public void getParamString() {
        String stringValue = ConfigUtils.getParamString(jsObject, "stringValue");
        Assert.assertNotNull(stringValue);
        Assert.assertEquals("string", stringValue);

        String booleanValue = ConfigUtils.getParamString(jsObject, "booleanValue");
        Assert.assertEquals("true", booleanValue);
    }

    @Test
    public void getParam() {
        String stringValue = ConfigUtils.getParam(String.class, jsObject, "stringValue");
        Assert.assertNotNull(stringValue);
        Assert.assertEquals("string", stringValue);

        Double doubleValue = ConfigUtils.getParam(Double.class, jsObject, "doubleValue");
        Assert.assertNotNull(doubleValue);
    }

    @Test
    public void getParamMap() {
        Map<String, String> map = ConfigUtils.getParamMap(jsObject, "map");
        Assert.assertNotNull(map);
        Assert.assertEquals("value1", map.get("key1"));
    }

    @Test
    public void getDeepestKey() {
        String deepestKey = ConfigUtils.getDeepestKey("com.example.deep");
        Assert.assertEquals("deep", deepestKey);

        deepestKey = ConfigUtils.getDeepestKey("com");
        Assert.assertEquals("com", deepestKey);
    }

    @Test
    public void getDeepestObject() {
        JSObject object = ConfigUtils.getDeepestObject(jsObject, "first.second.third");
        Assert.assertNotNull(object.getJSObject("third"));
    }

    @Test
    public void getOverwrittenAndroidParam() {
        String overwrittenString = ConfigUtils.getOverwrittenAndroidParam(String.class, jsObject, "stringValue");
        Assert.assertEquals("stringAndroid", overwrittenString);


        int intValue = ConfigUtils.getOverwrittenAndroidParam(Integer.class, jsObject, "intValue");
        Assert.assertEquals(1, intValue);
    }

    @Test
    public void getOverwrittenAndroidParamMap() {
        Map<String, String> map = ConfigUtils.getOverwrittenAndroidParamMap(jsObject, "map");
        Assert.assertNotNull(map);
        Assert.assertEquals("value1Android", map.get("key1"));
        Assert.assertEquals("value2", map.get("key2"));
        Assert.assertEquals("value3Android", map.get("key3"));
    }

    @Test
    public void overwriteWithEmpty() {
        String accessTokenEndpoint = "accessTokenEndpoint";
        Assert.assertNotNull(ConfigUtils.getParamString(jsObject, accessTokenEndpoint));
        Assert.assertEquals("", ConfigUtils.getOverwrittenAndroidParam(String.class, jsObject, accessTokenEndpoint));

        String inMapNullable = "inMapNullable";
        Map<String, String> paramMap = ConfigUtils.getParamMap(jsObject, "map");
        Assert.assertNotNull(paramMap.get(inMapNullable));
        Map<String, String> androidParamMap = ConfigUtils.getOverwrittenAndroidParamMap(jsObject, "map");
        Assert.assertEquals("", androidParamMap.get(inMapNullable));
    }

    @Test
    public void getRandomString() {
        String randomString = ConfigUtils.getRandomString(8);
        Assert.assertNotNull(randomString);
        Assert.assertEquals(8, randomString.length());
    }

    @Test
    public void empty() {
        // make sure the empty value stays empty
        String emptyValue = ConfigUtils.getParamString(jsObject, "empty");
        Assert.assertEquals(0, emptyValue.length());
    }

    @Test
    public void blank() {
        // make sure the blank value stays blank
        String blankValue = ConfigUtils.getParamString(jsObject, "blank");
        Assert.assertEquals(" ", blankValue);
    }

    @Test
    public void trimToNull() {
        Assert.assertNull(ConfigUtils.trimToNull("  "));
        Assert.assertNull(ConfigUtils.trimToNull(" "));
        Assert.assertNull(ConfigUtils.trimToNull(""));
        Assert.assertEquals("a", ConfigUtils.trimToNull("a"));
    }
}
