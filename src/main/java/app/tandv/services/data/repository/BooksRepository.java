package app.tandv.services.data.repository;

import app.tandv.services.data.entity.BookEntity;
import io.vertx.reactivex.core.Vertx;

import javax.persistence.EntityManager;

/**
 * We may remove this class, it all depends on the other entities as we add them and see if the library repository is
 * flexible enough to handle all possible cases
 *
 * @author vic on 2018-08-29
 */
public class BooksRepository extends LibraryRepository<BookEntity> {
    public BooksRepository(Vertx vertx, EntityManager entityManager) {
        super(BookEntity.class, vertx, entityManager);
    }
}
