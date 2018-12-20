package com.byteowls.capacitor.oauth2;

import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import org.json.JSONObject;

/**
 * @author m.oberwasserlechner@byteowls.com
 */
public abstract class ConfigUtils {

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
                value = o.getBoolean(k);
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


}
