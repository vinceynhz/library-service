package app.tandv.services.data.entity;

import app.tandv.services.configuration.EventConfig;
import io.vertx.core.json.JsonObject;

import javax.persistence.*;

/**
 * @author vic on 2020-09-22
 */
@Entity
@Table(name = "book_contributor")
@IdClass(BookContributorKey.class)
public class BookContributor {
    @Id
    @Column(name = "book_id")
    private Long bookId;

    @Id
    @Column(name = "contributor_id")
    private Long contributorId;

    @ManyToOne
    @JoinColumn(name="book_id", updatable = false, insertable = false, referencedColumnName = "id")
    private BookEntity book;


    @ManyToOne
    @JoinColumn(name="contributor_id", updatable = false, insertable = false, referencedColumnName = "id")
    private ContributorEntity contributor;

    @Column(name = EventConfig.TYPE, nullable = false, length = 12)
    @Enumerated(EnumType.STRING)
    private ContributorType type;

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(long bookId) {
        this.bookId = bookId;
    }

    public Long getContributorId() {
        return contributorId;
    }

    public void setContributorId(long contributorId) {
        this.contributorId = contributorId;
    }

    public ContributorType getType() {
        return type;
    }

    public void setType(ContributorType type) {
        this.type = type;
    }

    public BookContributor withType(ContributorType type) {
        this.type = type;
        return this;
    }

    public BookEntity getBook() {
        return this.book;
    }

    public void setBook(BookEntity book) {
        this.book = book;
        this.bookId = book.getId();
    }

    public BookContributor withBook(BookEntity book) {
        this.setBook(book);
        return this;
    }

    public ContributorEntity getContributor() {
        return this.contributor;
    }

    public void setContributor(ContributorEntity contributor) {
        this.contributor = contributor;
        this.contributorId = contributor.getId();
    }

    public BookContributor withContributor(ContributorEntity contributor) {
        this.setContributor(contributor);
        return this;
    }

    public JsonObject toBookJson() {
        return new JsonObject()
                .put(EventConfig.ID, this.getContributor().getId())
                .put(EventConfig.TYPE, this.getType().name());
    }

    public JsonObject toContributionJson() {
        return new JsonObject()
                .put(EventConfig.ID, this.getBook().getId())
                .put(EventConfig.TYPE, this.getType().name());
    }

}
