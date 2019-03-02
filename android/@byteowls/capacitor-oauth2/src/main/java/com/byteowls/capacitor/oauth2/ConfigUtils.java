package com.byteowls.capacitor.oauth2;

import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import org.json.JSONObject;

import java.util.Random;

/**
 * @author m.oberwasserlechner@byteowls.com
 */
public abstract class ConfigUtils {

    public static String getCallString(PluginCall call, String key) {
        return getCallParam(String.class, call, key);
    }

    public static <T> T getCallParam(Class<T> clazz, PluginCall call, String key) {
        return getCallParam(clazz, call, key, null);
    }

    public static <T> T getCallParam(Class<T> clazz, PluginCall call, String key, T defaultValue) {
        String k = getDeepestKey(key);
        try {
            JSONObject o = getDeepestObject(call.getData(), key);

            Object value = null;
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
            if (value == null) {
                return defaultValue;
            }
            return (T) value;
        } catch (Exception ignore) {}
        return defaultValue;
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
        for (int i = 0; i < parts.length-1; i++) {
            String k = parts[i];
            o = o.getJSObject(k);
        }
        return o;
    }

    public static String getRandomString(int len) {
        char[] ch = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L',
            'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
            'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
            'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
            'w', 'x', 'y', 'z' };

        char[] c = new char[len];
        Random random = new Random();
        for (int i = 0; i < len; i++) {
            c[i]=ch[random.nextInt(ch.length)];
        }
        return new String(c);
    }


}
