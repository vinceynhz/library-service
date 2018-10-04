package app.tandv.services.data.repository;

import app.tandv.services.data.entity.AbstractEntity;
import app.tandv.services.data.entity.AuthorEntity;
import app.tandv.services.data.entity.BookEntity;
import app.tandv.services.exception.LibraryOperationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * To have a generic way to handle the data repositories, this class implements {@link ClassValue} which serves to
 * resolve the repository to use given the entity class.
 *
 * @author Vic on 9/26/2018
 **/
@Component
public class LibraryRepository extends ClassValue<AbstractEntityRepository<? extends AbstractEntity>> {
    private final BooksRepository booksRepository;
    private final AuthorsRepository authorsRepository;

    @Autowired
    public LibraryRepository(BooksRepository booksRepository, AuthorsRepository authorsRepository) {
        this.booksRepository = booksRepository;
        this.authorsRepository = authorsRepository;
    }

    /**
     * This method serves to decide which repository should be used for a given entity class, if not found, this method
     * throws {@link IllegalArgumentException}
     *
     * @param type of entity to find
     * @return the repository to use for given entity class
     */
    @Override
    protected AbstractEntityRepository<? extends AbstractEntity> computeValue(Class<?> type) {
        if (BookEntity.class.isAssignableFrom(type)) {
            return booksRepository;
        }
        if (AuthorEntity.class.isAssignableFrom(type)) {
            return authorsRepository;
        }
        throw new IllegalArgumentException("No repository available for class " + type.getSimpleName());
    }

    /**
     * @param entityType {@link Class} of entities required to resolve the repository
     * @param <E>        generic type of {@link AbstractEntity}
     * @return all instances of E from the repository resolved for
     * @throws LibraryOperationException if failed to retrieve the List of objects
     */
    public <E extends AbstractEntity> Optional<Map<Integer, Object>> getAll(Class<E> entityType)
    throws LibraryOperationException {
        List<? extends AbstractEntity> allEntities = this.get(entityType).findAll();

        if (allEntities == null) {
            throw new LibraryOperationException(
                    "Something went wrong with retrieving all " + entityType.getName(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        if (allEntities.isEmpty()) {
            return Optional.empty();
        }

        Map<Integer, Object> entitiesInResponse = new HashMap<>();
        allEntities.forEach(o -> entitiesInResponse.put(o.getId(), o.toResponse()));
        return Optional.of(entitiesInResponse);
    }

    /**
     * @param entityType {@link Class} of entities required to resolve the repository
     * @param entityId   to use for searching
     * @param <E>        generic type of {@link AbstractEntity}
     * @return the found entity
     * @throws LibraryOperationException if no entity was found or if we fail to cast the extracted object into the
     *                                   desired class
     */
    public <E extends AbstractEntity> E fetchById(Class<E> entityType, Integer entityId)
    throws LibraryOperationException {
        AbstractEntity entity = this.get(entityType).findById(entityId).orElseThrow(() -> new LibraryOperationException(
                entityType.getName() + " with id " + entityId + " not found",
                HttpStatus.NOT_FOUND
        ));
        try {
            return entityType.cast(entity);
        } catch (ClassCastException exception) {
            throw new LibraryOperationException(
                    "Unable to convert found entity of class " + entity.getClass().getName()
                            + " to required " + entityType.getName(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * @param entityType {@link Class} of entities required to resolve the repository
     * @param entityId   to use for searching
     * @param <E>        generic type of {@link AbstractEntity}
     * @return an {@link Optional} that may contain the entity if found, or empty otherwise
     */
    public <E extends AbstractEntity> Optional<E> findById(Class<E> entityType, Integer entityId) {
        Optional<? extends AbstractEntity> entity = this.get(entityType).findById(entityId);
        return entity
                .filter(o -> entityType.isAssignableFrom(o.getClass()))
                .map(entityType::cast);
    }

    /**
     * @param entityType {@link Class} of entities required to resolve the repository
     * @param entity     to save in the database
     * @param <E>        generic type of {@link AbstractEntity}
     */
    public <E extends AbstractEntity> void save(Class<E> entityType, E entity) {
        AbstractEntityRepository repository = this.get(entityType);
        repository.save(entity);
    }

    @SuppressWarnings("unchecked")
    public <E extends AbstractEntity> void delete(Class<E> entityType, E entityId){
        AbstractEntityRepository repository = this.get(entityType);
        repository.delete(entityId);
    }
}
