package app.tandv.services.handler;

import app.tandv.services.configuration.EventConfig;
import app.tandv.services.data.entity.AuthorEntity;
import app.tandv.services.data.repository.AuthorsRepository;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;

/**
 * @author vic on 2020-07-21
 */
public class AuthorsHandler extends AbstractDBHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorsHandler.class);

    public void authors(RoutingContext context) {
        LOGGER.debug("Retrieving all authors from DB");
        JsonObject event = context.get(EventConfig.EVENT);
        EntityManager entityManager = context.get(EventConfig.ENTITY_MANAGER);
        AuthorsRepository repository = new AuthorsRepository(context.vertx(), entityManager);

        Disposable toDispose = repository.fetchAll()
                .map(AuthorEntity::toJson)
                .collect(JsonArray::new, JsonArray::add)
                .map(JsonArray::encode)
                .map(authors -> event
                        .put(EventConfig.CONTENT, authors)
                        .put(EventConfig.STATUS, HttpResponseStatus.OK.code())
                )
                .subscribe(
                        evt -> context.next(),
                        error -> this.errorHandler(context, error)
                );
        this.dispose(context.get(EventConfig.DISPOSABLES), toDispose);
    }

    public void add(RoutingContext context) {
        LOGGER.debug("Adding author to DB");
        JsonObject event = context.get(EventConfig.EVENT);
        JsonObject body = context.getBodyAsJson();
        EntityManager entityManager = context.get(EventConfig.ENTITY_MANAGER);
        AuthorsRepository repository = new AuthorsRepository(context.vertx(), entityManager);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(body.encode());
        }

        // To add authors we actually need a whole transaction so we can get entities in managed state
        entityManager.getTransaction().begin();

        // Get the new author entity to build
        Disposable toDispose = Single.just(body)
                .map(AuthorEntity::fromJson)
                // Save it
                .map(repository::add)
                // Make it nice looking for the response
                .map(AuthorEntity::toJson)
                .map(JsonObject::encode)
                // Put it in the response
                .map(author -> event
                        .put(EventConfig.CONTENT, author)
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
