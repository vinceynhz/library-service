package app.tandv.services.data.entity;

import app.tandv.services.configuration.EventConfig;
import app.tandv.services.util.EntityUtils;
import app.tandv.services.util.collections.FluentHashSet;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.util.*;

/**
 * @author vic on 2018-08-28
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@Entity
@Table(name = "book")
@NamedQueries({
        @NamedQuery(
                name = "BookEntity.findAll",
                query = "SELECT DISTINCT b FROM BookEntity b LEFT OUTER JOIN FETCH b.authors"
        ),
        @NamedQuery(
                name = "BookEntity.findAllById",
                query = "SELECT DISTINCT b FROM BookEntity b LEFT OUTER JOIN FETCH b.authors WHERE b.id IN :ids"
        )
})
public class BookEntity extends AbstractEntity {
    private static final Logger LOGGER = LoggerFactory.getLogger(BookEntity.class);

    private static final Set<String> REQUIRED_FIELDS = new FluentHashSet<String>()
            .thenAdd(EventConfig.TITLE)
            .thenAdd(EventConfig.FORMAT)
            .thenAdd(EventConfig.AUTHORS);

    public static BookEntity fromJson(JsonObject data) {
        LOGGER.debug("Building book entity from json data");
        if (!data.fieldNames().containsAll(REQUIRED_FIELDS)) {
            throw new IllegalArgumentException("Not enough attributes provided. Required: " + REQUIRED_FIELDS.toString());
        }
        BookFormat format = BookFormat.valueOf(data.getString(EventConfig.FORMAT));
        BookEntity entity = new BookEntity();
        entity.setTitle(data.getString(EventConfig.TITLE));
        entity.setFormat(format);
        if (data.containsKey(EventConfig.ISBN)) {
            entity.setIsbn(data.getString(EventConfig.ISBN));
        }
        if (data.containsKey(EventConfig.YEAR)) {
            entity.setYear(data.getString(EventConfig.YEAR));
        }
        if (data.containsKey(EventConfig.PAGES)) {
            entity.setPages(data.getInteger(EventConfig.PAGES).shortValue());
        }
        return entity;
    }

    @Column(name = EventConfig.TITLE, nullable = false, length = 500)
    private String title;

    @Column(name = EventConfig.ISBN, length = 20)
    private String isbn;

    @Column(name = EventConfig.YEAR, length = 4)
    private String year;

    @Column(name = EventConfig.FORMAT, nullable = false, length = 12)
    @Enumerated(EnumType.STRING)
    private BookFormat format;

    @Column(name = EventConfig.PAGES)
    private Short pages;

    @ManyToMany(cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE
    })
    @JoinTable(name = "book_author",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    private Set<AuthorEntity> authors = new HashSet<>();

    public BookEntity() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public BookFormat getFormat() {
        return format;
    }

    public void setFormat(BookFormat format) {
        this.format = format;
    }

    public Short getPages() {
        return pages;
    }

    public void setPages(Short pages) {
        this.pages = pages;
    }

    public Set<AuthorEntity> getAuthors() {
        return authors;
    }

    public void setAuthors(Set<AuthorEntity> authors) {
        this.authors = authors;
    }

    public void addAuthor(AuthorEntity author) {
        this.authors.add(author);
        author.getBooks().add(this);
    }

    public void removeAuthor(AuthorEntity author) {
        authors.remove(author);
        author.getBooks().remove(this);
    }

    public void clearAuthors() {
        // We remove the references to this book from all authors
        authors.forEach(authorEntity -> authorEntity.getBooks().remove(this));
        // Then we remove the authors from this book
        authors.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookEntity that = (BookEntity) o;
        return Objects.equals(title, that.title) &&
                Objects.equals(isbn, that.isbn) &&
                Objects.equals(year, that.year) &&
                format == that.format;
    }

    @Override
    public int hashCode() {
        return EntityUtils.entityHash(title, isbn, year, format);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                EventConfig.ID + "=" + id +
                ", " + EventConfig.TITLE + "='" + title + '\'' +
                ", " + EventConfig.ISBN + "='" + isbn + '\'' +
                ", " + EventConfig.YEAR + "='" + year + '\'' +
                ", " + EventConfig.FORMAT + "=" + format +
                ", " + EventConfig.AUTHORS + "=" + authors.size() +
                '}';
    }

    @Override
    public JsonObject toJson() {
        JsonArray authorIds = authors.stream()
                .map(AuthorEntity::getId)
                .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
        return new JsonObject()
                .put(EventConfig.ID, this.id)
                .put(EventConfig.TITLE, this.title)
                .put(EventConfig.ISBN, this.isbn)
                .put(EventConfig.YEAR, this.year)
                .put(EventConfig.FORMAT, this.format.name())
                .put(EventConfig.AUTHORS, authorIds);
    }
}
