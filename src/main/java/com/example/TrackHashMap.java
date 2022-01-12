package com.example;

import java.util.HashMap;
import java.util.Map;

public class TrackHashMap extends HashMap<String, String> {
    public TrackHashMap(Map<String, String> envMap) {
        super(envMap);
    }

    @Override
    public String get(Object key) {
        if (key instanceof String) {
            Interceptor.getInstance().onEnvVarRead((String) key);
        }
        return super.get(key);
    }
}
