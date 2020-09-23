package app.tandv.services.data.repository;

import app.tandv.services.data.entity.LibraryEntity;
import app.tandv.services.exception.PartialResultException;
import io.reactivex.Observable;
import io.vertx.reactivex.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Supplier;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;

/**
 * To have a generic way to handle the data repositories, this class implements {@link ClassValue} which serves to
 * resolve the repository to use given the entity class.
 *
 * @author vic on 2018-09-26
 */
public abstract class LibraryRepository<T extends LibraryEntity> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContributorsRepository.class);

    private final EntityManager entityManager;
    private final Class<T> type;
    private final Vertx vertx;

    // Technically this could exist for any entity matching all criteria of LibraryEntity
    LibraryRepository(Class<T> type, Vertx vertx, EntityManager entityManager) {
        this.type = type;
        this.vertx = vertx;
        this.entityManager = entityManager;
    }

    /**
     * Depending on the presence of the id we'll either persist (id == null : new entity) or merge ( id != null :
     * existing entity) the entity.
     *
     * @param entity to save/merge.
     * @return the same entity being saved
     */
    public T add(T entity) {
        if (entity.getId() == null) {
            entityManager.persist(entity);
        } else {
            entityManager.merge(entity);
        }
        return entity;
    }

    /**
     * This method executes the named query "findAll" from the {@link javax.persistence.Entity} {@code T} annotated class
     *
     * @return an {@link Observable} of all results found in the DB
     */
    public Observable<T> fetchAll() {
        String queryName = type.getSimpleName() + ".findAll";
        return this.rxExecuteQuery(
                () -> entityManager.createNamedQuery(queryName, type)
                        .getResultList()
        );
    }

    /**
     * This method executes the named query "findAllById" from the {@link javax.persistence.Entity} {@code T} annotated
     * class
     *
     * @param ids to search for
     * @return an {@link Observable} of all results found in the DB
     */
    public Observable<T> fetchAllById(Collection<Long> ids) {
        LOGGER.debug(String.valueOf(ids));
        String queryName = type.getSimpleName() + ".findAllById";
        return this.rxExecuteQuery(
                () -> {
                    List<T> result = entityManager
                            .createNamedQuery(queryName, type)
                            .setParameter("ids", ids)
                            .getResultList();
                    if (result.isEmpty()) {
                        throw new NoResultException("None of expected " + type.getSimpleName() + " ids were found");
                    }
                    if (result.size() < ids.size()) {
                        throw new PartialResultException("Not all of expected " + type.getSimpleName() + "ids were found");
                    }
                    return result;
                }
        );
    }

    private Observable<T> rxExecuteQuery(Supplier<List<T>> query) {
        return vertx
                // get results from the database
                .<List<T>>rxExecuteBlocking(promise -> {
                    try {
                        promise.complete(query.get());
                    } catch (PersistenceException exception) {
                        promise.fail(exception);
                    }
                })
                // and put them in superposition
                .flatMapObservable(Observable::fromIterable);
    }
}
