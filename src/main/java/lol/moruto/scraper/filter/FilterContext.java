package lol.moruto.scraper.filter;

import java.util.HashMap;
import java.util.Map;

public class FilterContext {
    private final Map<String, Object> contextMap = new HashMap<>();

    public <T> void put(String key, T value) {
        contextMap.put(key, value);
    }

    public <T> T get(String key, Class<T> clazz) {
        return clazz.cast(contextMap.get(key));
    }
}
