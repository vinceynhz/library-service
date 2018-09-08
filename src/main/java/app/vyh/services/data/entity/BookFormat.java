package app.vyh.services.data.entity;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Vic on 8/28/2018
 **/
public enum BookFormat {
    HC("hardcover"),
    PB("paperback"),
    GN("graphicNovel");

    private static final Map<String, BookFormat> ENUM_MAP;
    private final String alias;

    BookFormat(final String alias) {
        this.alias = alias;
    }

    public final String alias() {
        return alias;
    }

    static {
        Map<String, BookFormat> map = new ConcurrentHashMap<>();
        for (BookFormat bf : BookFormat.values()) {
            map.put(bf.alias(), bf);
        }
        ENUM_MAP = Collections.unmodifiableMap(map);
    }

    public static BookFormat fromString(final String alias) {
        if (!ENUM_MAP.containsKey(alias)) {
            throw new IllegalArgumentException("Unknown format value with alias [" + alias + "]");
        }
        return ENUM_MAP.get(alias);
    }
}
