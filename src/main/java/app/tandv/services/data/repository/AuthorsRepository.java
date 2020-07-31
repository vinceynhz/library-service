package app.tandv.services.data.repository;

import app.tandv.services.data.entity.AuthorEntity;
import io.vertx.reactivex.core.Vertx;

import javax.persistence.EntityManager;

/**
 * @author vic on 2018-09-01
 */
public class AuthorsRepository extends LibraryRepository<AuthorEntity> {
    public AuthorsRepository(Vertx vertx, EntityManager entityManager) {
        super(AuthorEntity.class, vertx, entityManager);
    }
}
