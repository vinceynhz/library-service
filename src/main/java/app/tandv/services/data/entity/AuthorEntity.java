package app.tandv.services.data.entity;

import app.tandv.services.configuration.EventConfig;
import app.tandv.services.util.EntityUtils;
import app.tandv.services.util.collections.FluentHashSet;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author vic on 2018-08-31
 */
@SuppressWarnings({"unused", "WeakerAccess"})
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
public class AuthorEntity extends AbstractEntity {
    private static final Set<String> REQUIRED_FIELDS = new FluentHashSet<String>()
            .thenAdd(EventConfig.NAME);

    @Column(name = EventConfig.NAME, nullable = false)
    private String name;

    // This maps to the field in the BookEntity class, not to the table
    @ManyToMany(mappedBy = EventConfig.AUTHORS)
    private Set<BookEntity> books = new HashSet<>();

    public AuthorEntity() {
    }

    public static AuthorEntity fromJson(JsonObject data) throws IllegalArgumentException {
        if (!data.fieldNames().containsAll(REQUIRED_FIELDS)){
            throw new IllegalArgumentException("Not enough attributes provided. Required: " + REQUIRED_FIELDS.toString());
        }
        AuthorEntity entity = new AuthorEntity();
        entity.setName(data.getString(EventConfig.NAME));
        return entity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<BookEntity> getBooks() {
        return books;
    }

    public void setBooks(Set<BookEntity> books) {
        this.books = books;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthorEntity that = (AuthorEntity) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return EntityUtils.entityHash(name);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                EventConfig.ID + "=" + this.id +
                ", " + EventConfig.NAME + "='" + this.name + '\'' +
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
                .put(EventConfig.NAME, this.name)
                .put(EventConfig.BOOKS, bookIds);
    }
}
