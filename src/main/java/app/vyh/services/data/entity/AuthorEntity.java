package app.vyh.services.data.entity;

import app.vyh.services.model.request.book.UpdateAuthorRequest;
import app.vyh.services.model.response.AuthorResponse;
import app.vyh.services.util.EntityUtils;
import app.vyh.services.util.StringUtils;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author Vic on 8/31/2018
 **/
@SuppressWarnings({"unused", "WeakerAccess"})
@Entity
@Table(name = "authors")
public class AuthorEntity {
    @Id
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "ordering_name")
    private String orderingName;

    @Column(name = "initials", length = 2)
    private String initials;

    @ManyToMany(mappedBy = "authors")
    private Set<BookEntity> books = new HashSet<>();

    public AuthorEntity() {
    }

    public AuthorEntity(String name) {
        this.name = StringUtils.titleCase(name);
        String[] working = StringUtils.authorForOrdering(name);
        this.orderingName = working[0];
        this.initials = working[1];
        this.id = this.hashCode() & 0x7FFFFFFF;
    }

    public AuthorEntity(UpdateAuthorRequest request) {
        this(request.getName());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrderingName() {
        return orderingName;
    }

    public void setOrderingName(String orderingName) {
        this.orderingName = orderingName;
    }

    public String getInitials() {
        return initials;
    }

    public void setInitials(String initials) {
        this.initials = initials;
    }

    public Set<BookEntity> getBooks() {
        return books;
    }

    public void setBooks(Set<BookEntity> books) {
        this.books = books;
    }

    public AuthorResponse toResponse() {
        AuthorResponse responseAuthor = new AuthorResponse()
                .withId(this.id)
                .withName(this.name)
                .withOrderingName(this.orderingName)
                .withInitials(this.initials);

        this.books.forEach(bookByAuthor -> responseAuthor.getBooks().add(bookByAuthor.getId()));
        return responseAuthor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthorEntity that = (AuthorEntity) o;
        return Objects.equals(orderingName, that.orderingName);
    }

    @Override
    public int hashCode() {
        return EntityUtils.entityHash(orderingName);
    }

    @Override
    public String toString() {
        return "AuthorEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", orderingName='" + orderingName + '\'' +
                ", initials='" + initials + '\'' +
                ", books=" + books.size() +
                '}';
    }
}
