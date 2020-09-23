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
import java.util.stream.Collectors;

/**
 * Notes on book entities:
 *
 * <strong>Normalization</strong>
 * Book titles are going to be normalized according to the rules of title casing defined in
 * {@link StringUtils#titleCase(String)}.
 *
 * <strong>Ordering</strong>
 * The string used for alphabetical cataloguing will be determined according to the rules defined in
 * {@link StringUtils#titleForOrdering(String)}.
 * <p>
 * Please note that two or more books may yield the same cataloguing value:
 * - The Dream Interpretation
 * - Dream Interpretation
 * <p>
 * Above titles will be ordered both under <em>dream interpretation</em>
 *
 * <strong>Uniqueness</strong>
 * A book's uniqueness should be identified by the title in appropriate case as defined by
 * {@link StringUtils::titleCase} normalization as well as by the contributors for that particular book. There is a
 * perfectly reasonable case in which two books by two different contributors may share the same title. For example:
 * - The Outsider by Stephen King
 * - The Outsider by Richard Wright
 *
 * In the same sense, and perhaps in more common scenarios, the books are also unique considering their format, for
 * example the paperback and hardback version of the same book should be
 * <p>
 * Thus, upon book creation the sha56 signature cannot be determined immediately, it is only after contributors have been
 * fully added to the book that the sha will be calculated. In order to prevent issues while coding,
 *
 * @author vic on 2018-08-28
 */
@SuppressWarnings({"unused", "WeakerAccess", "JpaQlInspection"})
@Entity
@Table(name = "book")
@NamedQueries({
        @NamedQuery(
                name = "BookEntity.findAll",
                query = "SELECT DISTINCT b FROM BookEntity b LEFT OUTER JOIN FETCH b.contributors"
        ),
        @NamedQuery(
                name = "BookEntity.findAllById",
                query = "SELECT DISTINCT b FROM BookEntity b LEFT OUTER JOIN FETCH b.contributors WHERE b.id IN :ids"
        )
})
public class BookEntity extends LibraryEntity<BookEntity> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BookEntity.class);

    private static final Set<String> REQUIRED_FIELDS = new FluentHashSet<String>()
            .thenAdd(EventConfig.TITLE)
            .thenAdd(EventConfig.FORMAT)
            .thenAdd(EventConfig.CONTRIBUTORS);

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
                .contributorForOrdering(rawTitle)
                .orElseThrow(() -> new IllegalArgumentException("Unable to generate cataloguing string for title [" + rawTitle + "]"));
        BookEntity entity = new BookEntity()
                .withFormat(format)
                .withTitle(cleanTitle)
                .withCataloguing(ordering);

        Optional.ofNullable(data.getString(EventConfig.ISBN)).ifPresent(entity::setIsbn);
        Optional.ofNullable(data.getString(EventConfig.YEAR)).ifPresent(entity::setYear);

        return entity;
    }

    @Column(name = EventConfig.TITLE, nullable = false)
    private String title;

    @Column(name = EventConfig.ISBN, length = 20)
    private String isbn;

    @Column(name = EventConfig.YEAR, length = 4)
    private String year;

    @Column(name = EventConfig.LANGUAGE, length = 5)
    private String language;

    @Column(name = EventConfig.FORMAT, nullable = false, length = 12)
    @Enumerated(EnumType.STRING)
    private BookFormat format;

    @ManyToMany(cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE
    })
    @JoinTable(name = "book_contributor",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "contributor_id")
    )
    private Set<ContributorEntity> contributors = new HashSet<>();

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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public BookEntity withLanguage(String language){
        this.language = language;
        return this;
    }

    public Set<ContributorEntity> getContributors() {
        return contributors;
    }

    public void setContributors(Set<ContributorEntity> contributors) {
        this.contributors = contributors;
    }

    public BookEntity withContributors(Set<ContributorEntity> contributors) {
        this.contributors = contributors;
        return this;
    }

    public void addContributor(ContributorEntity contributor) {
        this.contributors.add(contributor);
        contributor.getBooks().add(this);
    }

    public void removeContributor(ContributorEntity contributor) {
        contributors.remove(contributor);
        contributor.getBooks().remove(this);
    }

    public void clearAuthors() {
        // We remove the references to this book from all contributors
        contributors.forEach(contributorEntity -> contributorEntity.getBooks().remove(this));
        // Then we remove the contributors from this book
        contributors.clear();
    }

    /**
     * This method will take the title (after construction the title is already in proper casing), and the sha256
     * signature for each contributor and concatenate them to create the book's sha256.
     * <p>
     * This is done in order to identify books with the same title.
     *
     * @return a reference to this instance for fluent API
     */
    public BookEntity calculateSha256() {
        if (this.getContributors().isEmpty()) {
            throw new IllegalArgumentException("No contributor information to determine book sha256 signature");
        }
        String authorsSha256 = this.getContributors().stream()
                .map(ContributorEntity::getSha256)
                .collect(Collectors.joining(StringUtils.WORD_SEPARATOR));
        String bookSha256 = StringUtils.sha256(this.title + StringUtils.WORD_SEPARATOR + authorsSha256)
                .orElseThrow(() -> new IllegalArgumentException("Unable to generate SHA 256 for title [" + this.title + "]"));
        return this.withSha256(bookSha256);
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
                ", " + EventConfig.CATALOGUING + "='" + this.cataloguing + '\'' +
                ", " + EventConfig.ISBN + "='" + isbn + '\'' +
                ", " + EventConfig.YEAR + "='" + year + '\'' +
                ", " + EventConfig.FORMAT + "=" + format +
                ", " + EventConfig.CONTRIBUTORS + "=" + contributors.size() +
                '}';
    }

    @Override
    public JsonObject toJson() {
        JsonArray contributorIds = contributors.stream()
                .map(ContributorEntity::getId)
                .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
        return new JsonObject()
                .put(EventConfig.ID, this.id)
                .put(EventConfig.SHA_256, this.sha256)
                .put(EventConfig.TITLE, this.title)
                .put(EventConfig.CATALOGUING, this.cataloguing)
                .put(EventConfig.ISBN, this.isbn)
                .put(EventConfig.YEAR, this.year)
                .put(EventConfig.FORMAT, this.format.name())
                .put(EventConfig.CONTRIBUTORS, contributorIds);
    }
}
