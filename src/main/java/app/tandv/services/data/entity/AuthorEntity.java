package app.tandv.services.data.entity;

import app.tandv.services.configuration.EventConfig;
import app.tandv.services.util.EntityUtils;
import app.tandv.services.util.StringUtils;
import app.tandv.services.util.collections.FluentHashSet;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Notes on author entities:
 *
 * <strong>Normalization</strong>
 * Author names are going to be normalized according to the rules of title casing defined in
 * {@link StringUtils#titleCase(String)}.
 *
 * <strong>Ordering</strong>
 * The string used for alphabetical ordering will be determined according to the rules defined in
 * {@link StringUtils#authorForOrdering(String)}.
 * <p>
 * Please note that two or more authors may yield the same ordering value:
 * - Diane Maxwell
 * - Dr. Diane Maxwell
 * - Diane Maxwell Jr.
 * - Diane Maxwell III
 * <p>
 * All above names would be ordered as <em>maxwell diane</em> once all honorifics, special characters and roman numerals
 * are removed.
 *
 * <strong>Uniqueness</strong>
 * An author uniqueness is determined by the {@link StringUtils#sha256(String)} function over the normalized version of
 * the author's name as described above.
 *
 * <p>
 * The case for homonym authors is still to be determined.
 *
 * @author vic on 2018-08-31
 */
@SuppressWarnings({"unused", "WeakerAccess", "JpaQlInspection"})
@Entity
@Table(name = "author")
@NamedQueries({
        @NamedQuery(
                name = "AuthorEntity.findAll",
                query = "SELECT DISTINCT a FROM AuthorEntity a LEFT OUTER JOIN FETCH a.books"
        ),
        @NamedQuery(
                name = "AuthorEntity.findAllById",
                query = "SELECT DISTINCT a FROM AuthorEntity a WHERE a.id IN :ids"
        )
})
public class AuthorEntity extends AbstractEntity<AuthorEntity> {
    private static final Set<String> REQUIRED_FIELDS = new FluentHashSet<String>()
            .thenAdd(EventConfig.NAME);

    @Column(name = EventConfig.NAME, nullable = false)
    private String name;

    // This maps to the field in the BookEntity class, not to the table
    @ManyToMany(mappedBy = EventConfig.AUTHORS)
    private Set<BookEntity> books = new HashSet<>();

    public AuthorEntity() {
        super(AuthorEntity.class);
    }

    public static AuthorEntity fromJson(JsonObject data) throws IllegalArgumentException {
        if (!data.fieldNames().containsAll(REQUIRED_FIELDS)) {
            throw new IllegalArgumentException("Not enough attributes provided. Required: " + REQUIRED_FIELDS.toString());
        }
        String rawName = data.getString(EventConfig.NAME);
        String normalized = StringUtils
                .titleCase(rawName, true) // for author names we need to handle upper case
                .orElseThrow(() -> new IllegalArgumentException("Unable to generate title case for name [" + rawName + "]"));
        String ordering = StringUtils
                .authorForOrdering(rawName)
                .orElseThrow(() -> new IllegalArgumentException("Unable to generate ordering string for name [" + rawName + "]"));
        String sha256 = StringUtils
                .sha256(normalized)
                .orElseThrow(() -> new IllegalArgumentException("Unable to generate SHA 256 for name [" + normalized + "]"));

        return new AuthorEntity()
                .withName(normalized)
                .withOrdering(ordering)
                .withSha256(sha256);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AuthorEntity withName(String name) {
        this.name = name;
        return this;
    }

    public Set<BookEntity> getBooks() {
        return books;
    }

    public void setBooks(Set<BookEntity> books) {
        this.books = books;
    }

    public AuthorEntity withBooks(Set<BookEntity> books) {
        this.books = books;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthorEntity that = (AuthorEntity) o;
        return Objects.equals(this.sha256, that.sha256);
    }

    @Override
    public int hashCode() {
        return EntityUtils.entityHash(this.sha256);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                EventConfig.ID + "=" + this.id +
                ", " + EventConfig.SHA_256 + "='" + this.sha256 + '\'' +
                ", " + EventConfig.NAME + "='" + this.name + '\'' +
                ", " + EventConfig.ORDERING + "='" + this.ordering + '\'' +
                ", " + EventConfig.BOOKS + "=" + this.books.size() +
                '}';
    }

    @Override
    public JsonObject toJson() {
        JsonArray bookIds = books.stream()
                .map(BookEntity::getId)
                .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
        return new JsonObject()
                .put(EventConfig.ID, this.id)
                .put(EventConfig.SHA_256, this.sha256)
                .put(EventConfig.NAME, this.name)
                .put(EventConfig.ORDERING, this.ordering)
                .put(EventConfig.BOOKS, bookIds);
    }
}
