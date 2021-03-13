package app.tandv.services.data.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author vic on 2020-09-22
 */
public class BookContributorKey implements Serializable {

    private Long bookId;

    private Long contributorId;

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public Long getContributorId() {
        return contributorId;
    }

    public void setContributorId(Long contributorId) {
        this.contributorId = contributorId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookContributorKey key = (BookContributorKey) o;
        return Objects.equals(getBookId(), key.getBookId()) &&
                Objects.equals(getContributorId(), key.getContributorId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBookId(), getContributorId());
    }
}
