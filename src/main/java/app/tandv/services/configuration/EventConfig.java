package app.tandv.services.configuration;

/**
 * @author vic on 2020-07-21
 */
public final class EventConfig {
    private EventConfig(){
        // To prevent instantiation
    }

    // Plumbing keys: aka those that the verticles use to handle stuff
    public static final String EVENT = "event";
    public static final String DISPOSABLES = "disposables";
    public static final String STATUS = "status";
    public static final String UID = "uid";
    public static final String ST = "st";
    public static final String CONTENT = "content";
    public static final String ENTITY_MANAGER = "em";

    // Entity related keys: aka those used by the database entities
    public static final String ID = "id";
    public static final String SHA_256 = "sha256";
    public static final String CATALOGUING = "cataloguing";

    // Author fields
    public static final String NAME = "name";
    public static final String BOOKS = "books";

    // Book fields
    public static final String TITLE = "title";
    public static final String ISBN = "isbn";
    public static final String YEAR = "year";
    public static final String LANGUAGE = "language";
    public static final String FORMAT = "format";
    public static final String CONTRIBUTORS = "contributors";
}
