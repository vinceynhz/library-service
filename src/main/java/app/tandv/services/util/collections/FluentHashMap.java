package app.tandv.services.util.collections;

import java.util.HashMap;

/**
 * @author vic on 2020-08-04
 */
public class FluentHashMap<K, V> extends HashMap<K, V> {
    public FluentHashMap<K, V> thenPut(K key, V value) {
        super.put(key, value);
        return this;
    }
}
