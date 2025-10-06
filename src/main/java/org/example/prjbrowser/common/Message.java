package org.example.prjbrowser.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Message implements Serializable {
    private final Map<String, Object> data = new HashMap<>();

    public void put(String key, Object value) {
        data.put(key, value);
    }

    public Object get(String key) {
        return data.get(key);
    }

    public String getAction() {
        Object action = data.get("action");
        return action != null ? action.toString() : null;
    }

    public Map<String, Object> getData() {
        return data;
    }
}
