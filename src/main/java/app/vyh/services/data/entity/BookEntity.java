package app.vyh.services.data.entity;

import app.vyh.services.model.request.book.AddBookRequest;
import app.vyh.services.model.response.BookResponse;
import app.vyh.services.util.EntityUtils;
import app.vyh.services.util.StringUtils;

import javax.persistence.*;
import java.util.*;

/**
 * @author Vic on 8/28/2018
 **/
@SuppressWarnings({"unused", "WeakerAccess"})
@Entity
@Table(name = "books")
public class BookEntity {
    @Id
    private Integer id;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "ordering_title", nullable = false, length = 500)
    private String orderingTitle;

    @Column(name = "isbn", nullable = false, length = 20)
    private String isbn;

    @Column(name = "year", nullable = false, length = 4)
    private String year;

    @Column(name = "format", nullable = false, length = 2)
    @Enumerated(EnumType.STRING)
    private BookFormat format;

    @Column(name = "quantity")
    private Byte quantity = 1;

    @ManyToMany(cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE
    })
    @JoinTable(name = "books_authors",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    private Set<AuthorEntity> authors = new HashSet<>();

    public BookEntity() {
    }

    public BookEntity(String title, String isbn, String year, BookFormat format) {
        this.isbn = isbn;
        this.year = year;
        this.format = format;
        // Make the title clean and nice for display
        this.title = StringUtils.titleCase(title);
        // Then we get the normalized string for ordering
        this.orderingTitle = StringUtils.titleForOrdering(title);
        // Here we are making a positive integer out of the hashCode.
        this.id = this.hashCode() & 0x7FFFFFFF;
    }

    public BookEntity(AddBookRequest request) {
        this(request.getTitle(), request.getIsbn(), request.getYear(), request.getFormat());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOrderingTitle() {
        return orderingTitle;
    }

    public void setOrderingTitle(String orderingTitle) {
        this.orderingTitle = orderingTitle;
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

    public Byte getQuantity() {
        return quantity;
    }

    public void setQuantity(Byte quantity) {
        this.quantity = quantity;
    }

    public Set<AuthorEntity> getAuthors() {
        return authors;
    }

    public void setAuthors(Set<AuthorEntity> authors) {
        this.authors = authors;
    }

    public void addAuthor(AuthorEntity author) {
//        BooksAuthorsEntity booksAuthors = new BooksAuthorsEntity(author, this);
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

    public BookResponse toResponse() {
        BookResponse responseBook = (BookResponse) new BookResponse()
                .withId(this.id)
                .withOrderingTitle(this.orderingTitle)
                .withQuantity(this.quantity)
                .withTitle(this.title)
                .withFormat(this.format)
                .withIsbn(this.isbn)
                .withYear(this.year);

        this.authors.forEach(authorInBook -> responseBook.getAuthors().add(authorInBook.getId()));
        return responseBook;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookEntity that = (BookEntity) o;
        return Objects.equals(orderingTitle, that.orderingTitle) &&
                Objects.equals(isbn, that.isbn) &&
                Objects.equals(year, that.year) &&
                format == that.format;
    }

    @Override
    public int hashCode() {
        return EntityUtils.entityHash(orderingTitle, isbn, year, format);
    }

    @Override
    public String toString() {
        return "BookEntity{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", orderingTitle='" + orderingTitle + '\'' +
                ", isbn='" + isbn + '\'' +
                ", year='" + year + '\'' +
                ", format=" + format +
                ", quantity=" + quantity +
                ", authors=" + authors.size() +
                '}';
    }
}
