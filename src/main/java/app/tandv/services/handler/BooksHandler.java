package app.tandv.services.handler;

import app.tandv.services.configuration.EventConfig;
import app.tandv.services.data.entity.BookEntity;
import app.tandv.services.data.repository.AuthorsRepository;
import app.tandv.services.data.repository.BooksRepository;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;

/**
 * @author vic on 2020-07-21
 */
public class BooksHandler extends AbstractDBHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(BooksHandler.class);

    /**
     * To retrieve all books available in the DB.
     *
     * @param context of the current request
     */
    public void books(RoutingContext context) {
        LOGGER.debug("Retrieving all books from DB");
        JsonObject event = context.get(EventConfig.EVENT);
        EntityManager entityManager = context.get(EventConfig.ENTITY_MANAGER);
        BooksRepository repository = new BooksRepository(context.vertx(), entityManager);

        Disposable toDispose = repository.fetchAll()
                .map(BookEntity::toJson)
                .collect(JsonArray::new, JsonArray::add)
                .map(JsonArray::encode)
                .map(books -> event
                        .put(EventConfig.CONTENT, books)
                        .put(EventConfig.STATUS, HttpResponseStatus.OK.code())
                )
                .subscribe(
                        evt -> context.next(),
                        error -> this.errorHandler(context, error)
                );
        this.dispose(context.get(EventConfig.DISPOSABLES), toDispose);
    }

    /**
     * To add a book to the database
     *
     * @param context to retrieve parameters
     */
    public void add(RoutingContext context) {
        LOGGER.debug("Adding book to DB");
        JsonObject body = context.getBodyAsJson();
        JsonObject event = context.get(EventConfig.EVENT);
        Vertx vertx = context.vertx();

        EntityManager entityManager = context.get(EventConfig.ENTITY_MANAGER);
        BooksRepository booksRepository = new BooksRepository(vertx, entityManager);
        AuthorsRepository authorsRepository = new AuthorsRepository(vertx, entityManager);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(body.encode());
        }

        // To add books we actually need a whole transaction so we can get a bunch of entities in managed state
        // ...
        // in theory. And it worked :)
        entityManager.getTransaction().begin();

        Disposable toDispose = Single.just(body)
                .filter(b -> b.containsKey(EventConfig.AUTHORS))
                .map(b -> b.getJsonArray(EventConfig.AUTHORS))
                .map(authors -> this.unwrapJsonArray(authors, JsonArray::getLong))
                // Find all the authors with those ids
                .flatMapObservable(authorsRepository::fetchAllById)
                // And add them to the book entity, if the book entity fails the whole thing fails
                .collect(() -> BookEntity.fromJson(body), BookEntity::addAuthor)
                // Save the book entity
                .map(booksRepository::add)
                // Make it nice looking for the response
                .map(BookEntity::toJson)
                .map(JsonObject::encode)
                // Put it in the response
                .map(book -> event
                        .put(EventConfig.CONTENT, book)
                        .put(EventConfig.STATUS, HttpResponseStatus.CREATED.code())
                )
                // Materialize everything
                .subscribe(
                        evt -> context.next(),
                        error -> this.errorHandler(context, error)
                );
        this.dispose(context.get(EventConfig.DISPOSABLES), toDispose);
    }
}
