package com.byteowls.capacitor.oauth2;

import com.getcapacitor.JSObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

/**
 * @author m.oberwasserlechner@byteowls.com
 */
public abstract class ConfigUtils {

    public static String getParamString(JSObject data, String key) {
        return getParam(String.class, data, key);
    }

    public static <T> T getParam(Class<T> clazz, JSObject data, String key) {
        return getParam(clazz, data, key, null);
    }

    public static <T> T getParam(Class<T> clazz, JSObject data, String key, T defaultValue) {
        String k = getDeepestKey(key);
        if (k != null) {
            try {
                Object value = null;
                JSONObject o = getDeepestObject(data, key);

                // #109
                if (o.has(k)) {
                    if (clazz.isAssignableFrom(String.class)) {
                        value = o.getString(k);
                    } else if (clazz.isAssignableFrom(Boolean.class)) {
                        value = o.optBoolean(k);
                    } else if (clazz.isAssignableFrom(Double.class)) {
                        value = o.getDouble(k);
                    } else if (clazz.isAssignableFrom(Integer.class)) {
                        value = o.getInt(k);
                    } else if (clazz.isAssignableFrom(Long.class)) {
                        value = o.getLong(k);
                    } else if (clazz.isAssignableFrom(Float.class)) {
                        Double doubleValue = o.getDouble(k);
                        value = doubleValue.floatValue();
                    } else if (clazz.isAssignableFrom(Integer.class)) {
                        value = o.getInt(k);
                    }
                }
                if (value == null) {
                    return defaultValue;
                }
                return (T) value;
            } catch (Exception ignore) {
            }
        }
        return defaultValue;
    }

    public static Map<String, String> getParamMap(JSObject data, String key) {
        Map<String, String> map = new HashMap<>();
        String k = getDeepestKey(key);
        if (k != null) {
            try {
                JSONObject o = getDeepestObject(data, key);
                JSONObject jsonObject = o.getJSONObject(k);
                Iterator<String> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    String mapKey = keys.next();
                    if (mapKey != null && mapKey.trim().length() > 0) {
                        try {
                            String mapValue = jsonObject.getString(mapKey);
                            map.put(mapKey, mapValue);
                        } catch (JSONException ignore) {}
                    }
                }
            } catch (Exception ignore) {
            }
        }
        return map;
    }

    public static String getDeepestKey(String key) {
        String[] parts = key.split("\\.");
        if (parts.length > 0) {
            return parts[parts.length - 1];
        }
        return null;
    }

    public static JSObject getDeepestObject(JSObject o, String key) {
        // Split on periods
        String[] parts = key.split("\\.");
        // Search until the second to last part of the key
        for (int i = 0; i < parts.length - 1; i++) {
            String k = parts[i];
            o = o.getJSObject(k);
        }
        return o;
    }

    public static <T> T getOverwrittenAndroidParam(Class<T> clazz, JSObject data, String key) {
        T baseParam = getParam(clazz, data, key);
        T androidParam = getParam(clazz, data, "android." + key);
        if (androidParam != null) {
            baseParam = androidParam;
        }
        return baseParam;
    }

    public static Map<String, String> getOverwrittenAndroidParamMap(JSObject data, String key) {
        Map<String, String> baseParam = getParamMap(data, key);
        Map<String, String> androidParam = getParamMap(data, "android." + key);
        Map<String, String> mergedParam = new HashMap<>(baseParam);
        mergedParam.putAll(androidParam);
        return mergedParam;
    }

    public static String getRandomString(int len) {
        char[] ch = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L',
            'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
            'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
            'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
            'w', 'x', 'y', 'z'};

        char[] c = new char[len];
        Random random = new Random();
        for (int i = 0; i < len; i++) {
            c[i] = ch[random.nextInt(ch.length)];
        }
        return new String(c);
    }

    public static String trimToNull(String value) {
        if (value != null && value.trim().length() == 0) {
            return null;
        }
        return value;
    }


}
