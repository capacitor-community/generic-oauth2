package com.byteowls.capacitor.oauth2;

import android.util.Log;

import com.getcapacitor.JSObject;

import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

public class ConfigUtilsTest {

    private final static String BASE_JSON = "{\n" +
        "    \"doubleValue\": 123.4567,\n" +
        "    \"floatValue\": 123.4,\n" +
        "    \"intValue\": 1,\n" +
        "    \"stringValue\": \"string\",\n" +
        "    \"booleanValue\": true,\n" +
        "    \"accessTokenEndpoint\": \"https://byteowls.com\",\n" +
        "    \"first\": {\n" +
        "        \"second\": {\n" +
        "            \"third\": {\n" +
        "                \"doubleValue\": 5.4,\n" +
        "                \"floatValue\": 5.9,\n" +
        "                \"intValue\": 2,\n" +
        "                \"stringValue\": \"stringDeep\",\n" +
        "                \"booleanValue\": false\n" +
        "            }\n" +
        "        }\n" +
        "    },\n" +
        "    \"map\": {\n" +
        "        \"key1\": \"value1\",\n" +
        "        \"key2\": \"value2\",\n" +
        "        \"inMapNullable\": \"notEmpty\"\n" +
        "    },\n" +
        "    \"android\": {\n" +
        "        \"stringValue\": \"stringAndroid\",\n" +
        "        \"accessTokenEndpoint\": \"\",\n" +
        "        \"map\": {\n" +
        "            \"key1\": \"value1Android\",\n" +
        "            \"key3\": \"value3Android\",\n" +
        "            \"inMapNullable\": \"\"\n" +
        "        }\n" +
        "    },\n" +
        "    \"empty\": \"\",\n" +
        "    \"blank\": \" \"\n" +
        "}";

    private JSObject jsObject;

    @BeforeEach
    public void setUp() {
        try {
            this.jsObject = new JSObject(BASE_JSON);
        } catch (Exception e) {
            Log.e("OAuth2", "", e);
        }
    }

    @Test
    public void getParamString() {
        String stringValue = ConfigUtils.getParamString(jsObject, "stringValue");
        Assertions.assertNotNull(stringValue);
        Assertions.assertEquals("string", stringValue);
    }

    @Test
    public void getParam() {
        String stringValue = ConfigUtils.getParam(String.class, jsObject, "stringValue");
        Assertions.assertNotNull(stringValue);
        Assertions.assertEquals("string", stringValue);

        Double doubleValue = ConfigUtils.getParam(Double.class, jsObject, "doubleValue");
        Assertions.assertNotNull(doubleValue);
    }

    @Test
    public void getParamMap() {
        Map<String, String> map = ConfigUtils.getParamMap(jsObject, "map");
        Assertions.assertNotNull(map);
        Assertions.assertEquals("value1", map.get("key1"));
    }

    @Test
    public void getDeepestKey() {
        String deepestKey = ConfigUtils.getDeepestKey("com.example.deep");
        Assertions.assertEquals("deep", deepestKey);

        deepestKey = ConfigUtils.getDeepestKey("com");
        Assertions.assertEquals("com", deepestKey);
    }

    @Test
    public void getDeepestObject() {
        JSObject object = ConfigUtils.getDeepestObject(jsObject, "first.second.third");
        Assertions.assertNotNull(object.getJSObject("third"));
    }

    @Test
    public void getOverwrittenAndroidParam() {
        String overwrittenString = ConfigUtils.getOverwrittenAndroidParam(String.class, jsObject, "stringValue");
        Assertions.assertEquals("stringAndroid", overwrittenString);


        int intValue = ConfigUtils.getOverwrittenAndroidParam(Integer.class, jsObject, "intValue");
        Assertions.assertEquals(1, intValue);
    }

    @Test
    public void getOverwrittenAndroidParamMap() {
        Map<String, String> map = ConfigUtils.getOverwrittenAndroidParamMap(jsObject, "map");
        Assertions.assertNotNull(map);
        Assertions.assertEquals("value1Android", map.get("key1"));
        Assertions.assertEquals("value2", map.get("key2"));
        Assertions.assertEquals("value3Android", map.get("key3"));
    }

    @Test
    public void overwriteWithEmpty() {
        String accessTokenEndpoint = "accessTokenEndpoint";
        Assertions.assertNotNull(ConfigUtils.getParamString(jsObject, accessTokenEndpoint));
        Assertions.assertEquals("", ConfigUtils.getOverwrittenAndroidParam(String.class, jsObject, accessTokenEndpoint));

        String inMapNullable = "inMapNullable";
        Map<String, String> paramMap = ConfigUtils.getParamMap(jsObject, "map");
        Assertions.assertNotNull(paramMap.get(inMapNullable));
        Map<String, String> androidParamMap = ConfigUtils.getOverwrittenAndroidParamMap(jsObject, "map");
        Assertions.assertEquals("", androidParamMap.get(inMapNullable));
    }

    @ParameterizedTest
    @MethodSource("getBooleanArguments")
    public void getOverwrittenBoolean(String json, String key, Boolean expected) throws JSONException {
        JSObject jsObject = new JSObject(json);
        Boolean actual = ConfigUtils.getOverwrittenAndroidParam(Boolean.class, jsObject, key);
        if (expected == null) {
            Assertions.assertNull(actual);
        } else {
            Assertions.assertEquals(expected, actual);
        }
    }

    private static Stream<Arguments> getBooleanArguments() {
        return Stream.of(
            Arguments.of("{ \"pkceEnabled\": true, \"android\":{\"pkceEnabled\": false}}", "pkceEnabled", false),
            Arguments.of("{ \"pkceEnabled\": true}", "pkceEnabled", true),
            Arguments.of("{ \"pkceEnabled\": true}", "android.pkceEnabled", null),
            Arguments.of("{ \"pkceEnabled\": true, \"ios\":{\"pkceEnabled\": false}}", "pkceEnabled", true)
        );
    }

    @Test
    public void getRandomString() {
        String randomString = ConfigUtils.getRandomString(8);
        Assertions.assertNotNull(randomString);
        Assertions.assertEquals(8, randomString.length());
    }

    @Test
    public void empty() {
        // make sure the empty value stays empty
        String emptyValue = ConfigUtils.getParamString(jsObject, "empty");
        Assertions.assertEquals(0, emptyValue.length());
    }

    @Test
    public void blank() {
        // make sure the blank value stays blank
        String blankValue = ConfigUtils.getParamString(jsObject, "blank");
        Assertions.assertEquals(" ", blankValue);
    }

    @Test
    public void trimToNull() {
        Assertions.assertNull(ConfigUtils.trimToNull("  "));
        Assertions.assertNull(ConfigUtils.trimToNull(" "));
        Assertions.assertNull(ConfigUtils.trimToNull(""));
        Assertions.assertEquals("a", ConfigUtils.trimToNull("a"));
    }
}
