package app.tandv.services.data.handler;

import app.tandv.services.configuration.EventConfig;
import app.tandv.services.data.entity.ContributorEntity;
import app.tandv.services.data.repository.ContributorsRepository;
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
public class ContributorHandler extends LibraryHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContributorHandler.class);

    public void contributors(RoutingContext context) {
        LOGGER.debug("Retrieving all contributors from DB");
        JsonObject event = context.get(EventConfig.EVENT);
        EntityManager entityManager = context.get(EventConfig.ENTITY_MANAGER);
        ContributorsRepository repository = new ContributorsRepository(context.vertx(), entityManager);

        Disposable toDispose = repository.fetchAll()
                .map(ContributorEntity::toJson)
                .collect(JsonArray::new, JsonArray::add)
                .map(JsonArray::encode)
                .map(contributors -> event
                        .put(EventConfig.CONTENT, contributors)
                        .put(EventConfig.STATUS, HttpResponseStatus.OK.code())
                )
                .subscribe(
                        evt -> context.next(),
                        error -> this.errorHandler(context, error)
                );
        this.dispose(context.get(EventConfig.DISPOSABLES), toDispose);
    }

    public void add(RoutingContext context) {
        LOGGER.debug("Adding contributor to DB");
        JsonObject event = context.get(EventConfig.EVENT);
        JsonObject body = context.getBodyAsJson();
        EntityManager entityManager = context.get(EventConfig.ENTITY_MANAGER);
        ContributorsRepository repository = new ContributorsRepository(context.vertx(), entityManager);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(body.encode());
        }

        // To add contributors we actually need a whole transaction so we can get entities in managed state
        entityManager.getTransaction().begin();

        // Get the new contributor entity to build
        Disposable toDispose = Single.just(body)
                .map(ContributorEntity::fromJson)
                // Save it
                .map(repository::add)
                // Make it nice looking for the response
                .map(ContributorEntity::toJson)
                .map(JsonObject::encode)
                // Put it in the response
                .map(contributor -> event
                        .put(EventConfig.CONTENT, contributor)
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
