package app.tandv.services.data.entity;

import app.tandv.services.configuration.EventConfig;
import app.tandv.services.util.EntityUtils;
import app.tandv.services.util.StringUtils;
import app.tandv.services.util.collections.FluentHashSet;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.util.*;

/**
 * Notes on book entities:
 *
 * <strong>Normalization</strong>
 * Book titles are going to be normalized according to the rules of title casing defined in
 * {@link StringUtils#titleCase(String)}.
 *
 * <strong>Ordering</strong>
 * The string used for alphabetical ordering will be determined according to the rules defined in
 * {@link StringUtils#titleForOrdering(String)}.
 * <p>
 * Please note that two or more books may yield the same ordering value:
 *
 * @author vic on 2018-08-28
 */
@SuppressWarnings({"unused", "WeakerAccess", "JpaQlInspection"})
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
public class BookEntity extends AbstractEntity<BookEntity> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BookEntity.class);

    private static final Set<String> REQUIRED_FIELDS = new FluentHashSet<String>()
            .thenAdd(EventConfig.TITLE)
            .thenAdd(EventConfig.FORMAT)
            .thenAdd(EventConfig.AUTHORS);

    public BookEntity() {
        super(BookEntity.class);
    }

    public static BookEntity fromJson(JsonObject data) {
        LOGGER.debug("Building book entity from json data");
        if (!data.fieldNames().containsAll(REQUIRED_FIELDS)) {
            throw new IllegalArgumentException("Not enough attributes provided. Required: " + REQUIRED_FIELDS.toString());
        }
        BookFormat format = BookFormat.valueOf(data.getString(EventConfig.FORMAT));
        String rawTitle = data.getString(EventConfig.TITLE);
        String cleanTitle = StringUtils
                .titleCase(rawTitle)
                .orElseThrow(() -> new IllegalArgumentException("Unable to generate title case for title [" + rawTitle + "]"));
        String ordering = StringUtils
                .authorForOrdering(rawTitle)
                .orElseThrow(() -> new IllegalArgumentException("Unable to generate ordering string for title [" + rawTitle + "]"));
        String sha256 = StringUtils
                .sha256(cleanTitle)
                .orElseThrow(() -> new IllegalArgumentException("Unable to generate SHA 256 for title [" + cleanTitle + "]"));
        BookEntity entity = new BookEntity()
                .withFormat(format)
                .withTitle(cleanTitle)
                .withOrdering(ordering)
                .withSha256(sha256);

        Optional.ofNullable(data.getString(EventConfig.ISBN)).ifPresent(entity::setIsbn);
        Optional.ofNullable(data.getString(EventConfig.YEAR)).ifPresent(entity::setYear);
        Optional.ofNullable(data.getInteger(EventConfig.PAGES)).map(Integer::shortValue).ifPresent(entity::setPages);

        return entity;
    }

    @Column(name = EventConfig.TITLE, nullable = false)
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BookEntity withTitle(String title) {
        this.title = title;
        return this;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public BookEntity withIsbn(String isbn) {
        this.isbn = isbn;
        return this;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public BookEntity withYear(String year) {
        this.year = year;
        return this;
    }

    public BookFormat getFormat() {
        return format;
    }

    public void setFormat(BookFormat format) {
        this.format = format;
    }

    public BookEntity withFormat(BookFormat format) {
        this.format = format;
        return this;
    }

    public Short getPages() {
        return pages;
    }

    public void setPages(Short pages) {
        this.pages = pages;
    }

    public BookEntity withPages(Short pages) {
        this.pages = pages;
        return this;
    }

    public Set<AuthorEntity> getAuthors() {
        return authors;
    }

    public void setAuthors(Set<AuthorEntity> authors) {
        this.authors = authors;
    }

    public BookEntity withAuthors(Set<AuthorEntity> authors) {
        this.authors = authors;
        return this;
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
        return Objects.equals(this.sha256, that.sha256);
    }

    @Override
    public int hashCode() {
        return EntityUtils.entityHash(this.sha256);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                EventConfig.ID + "=" + id +
                ", " + EventConfig.SHA_256 + "='" + this.sha256 + '\'' +
                ", " + EventConfig.TITLE + "='" + title + '\'' +
                ", " + EventConfig.ORDERING + "='" + this.ordering + '\'' +
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
                .put(EventConfig.SHA_256, this.sha256)
                .put(EventConfig.TITLE, this.title)
                .put(EventConfig.ORDERING, this.ordering)
                .put(EventConfig.ISBN, this.isbn)
                .put(EventConfig.YEAR, this.year)
                .put(EventConfig.FORMAT, this.format.name())
                .put(EventConfig.AUTHORS, authorIds);
    }
}
