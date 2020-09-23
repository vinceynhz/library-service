package app.tandv.services.data.repository;

import app.tandv.services.data.entity.ContributorEntity;
import io.vertx.reactivex.core.Vertx;

import javax.persistence.EntityManager;

/**
 * @author vic on 2018-09-01
 */
public class ContributorsRepository extends LibraryRepository<ContributorEntity> {
    public ContributorsRepository(Vertx vertx, EntityManager entityManager) {
        super(ContributorEntity.class, vertx, entityManager);
    }
}
