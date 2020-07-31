package app.tandv.services.verticle;

import app.tandv.services.configuration.EventConfig;
import app.tandv.services.configuration.MediaTypes;
import app.tandv.services.handler.AuthorsHandler;
import app.tandv.services.handler.BooksHandler;
import app.tandv.services.handler.RequestHandler;
import app.tandv.services.handler.ResponseHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.CorsHandler;
import io.vertx.reactivex.ext.web.handler.ResponseContentTypeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

/**
 * @author vic on 2020-07-20
 */
public class ServiceVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceVerticle.class);
    public static final String PORT_PROPERTY = "http.port";
    private static final int DEFAULT_PORT = 8080;

    private final EntityManagerFactory entityManagerFactory;

    private RequestHandler requestHandler;
    private ResponseHandler responseHandler;

    public ServiceVerticle(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public Completable rxStart() {
        int port = this.config().getInteger(PORT_PROPERTY, DEFAULT_PORT);
        LOGGER.info("Starting service verticle in port {}", port);
        this.requestHandler = new RequestHandler();
        this.responseHandler = new ResponseHandler();
        Router router = this.getRouter().mountSubRouter("/data", this.getDataApi());
        return this.vertx.createHttpServer()
                .requestHandler(router)
                .rxListen(port)
                .doOnSuccess(server -> LOGGER.info("Service verticle listening on port: {}", server.actualPort()))
                .doOnError(cause -> LOGGER.error("Unable to start service verticle", cause))
                .ignoreElement();
    }

    private Router getRouter() {
        Router router = Router.router(this.vertx);
        router.route()
                .handler(
                        CorsHandler.create("*")
                                .allowedHeaders(this.getAllowedHeaders())
                                .allowedMethods(this.getAllowedMethods())
                )
                .handler(BodyHandler.create())
                .handler(ResponseContentTypeHandler.create())
                // Do all of this in case of error
                .failureHandler(this::releaseEntityManager)
                .failureHandler(this::routeFailureHandler)
                .failureHandler(this.responseHandler);
        return router;
    }

    /**
     * The purpose of this service is narrowed down to query all the collection of books or to add/update books.
     * <p>
     * More complex things like searches and queries should be performed at the frontend level that can process all at
     * the client side instead at the server side.
     *
     * @return the routes for the data api to read the collections and add/update elements
     */
    private Router getDataApi() {
        BooksHandler booksHandler = new BooksHandler();
        AuthorsHandler authorsHandler = new AuthorsHandler();
        Router router = Router.router(this.vertx);
        // all happy paths
        router.get("/books")
                .produces(MediaTypes.APPLICATION_JSON)
                .handler(this.requestHandler)
                .handler(this::addEntityManager)
                .handler(booksHandler::books)
                .handler(this::releaseEntityManager)
                .handler(this.responseHandler);
        router.post("/book")
                .produces(MediaTypes.APPLICATION_JSON)
                .consumes(MediaTypes.APPLICATION_JSON)
                .handler(this.requestHandler)
                .handler(this::addEntityManager)
                .handler(booksHandler::add)
                .handler(this::releaseEntityManager)
                .handler(this.responseHandler);
        router.get("/authors")
                .produces(MediaTypes.APPLICATION_JSON)
                .handler(this.requestHandler)
                .handler(this::addEntityManager)
                .handler(authorsHandler::authors)
                .handler(this::releaseEntityManager)
                .handler(this.responseHandler);
        router.post("/author")
                .produces(MediaTypes.APPLICATION_JSON)
                .consumes(MediaTypes.APPLICATION_JSON)
                .handler(this.requestHandler)
                .handler(this::addEntityManager)
                .handler(authorsHandler::add)
                .handler(this::releaseEntityManager)
                .handler(this.responseHandler);
        return router;
    }

    private Set<String> getAllowedHeaders() {
        Set<String> allowedHeaders = new HashSet<>();
        allowedHeaders.add("x-requested-with");
        allowedHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN.toString());
        allowedHeaders.add(HttpHeaders.ORIGIN.toString());
        allowedHeaders.add(HttpHeaders.CONTENT_TYPE.toString());
        allowedHeaders.add(HttpHeaders.ACCEPT.toString());
        return allowedHeaders;
    }

    private Set<HttpMethod> getAllowedMethods() {
        Set<HttpMethod> allowedMethods = new HashSet<>();
        allowedMethods.add(HttpMethod.GET);
        allowedMethods.add(HttpMethod.POST);
        allowedMethods.add(HttpMethod.PUT);
        allowedMethods.add(HttpMethod.HEAD);
        allowedMethods.add(HttpMethod.OPTIONS);
        return allowedMethods;
    }

    // Handlers
    private void routeFailureHandler(RoutingContext context) {
        LOGGER.error("Error processing request", context.failure());
        JsonObject error = new JsonObject()
                .put("exception", context.failure().getClass().getName())
                .put("message", context.failure().getMessage())
                .put("timestamp", new Timestamp(System.currentTimeMillis()).toString())
                .put("path", context.request().uri());
        JsonObject event = context.get(EventConfig.EVENT);
        event.put(EventConfig.CONTENT, error.encode())
                .put(EventConfig.STATUS, context.statusCode());
        context.response().setStatusCode(context.statusCode());
        context.next();
    }

    private void addEntityManager(RoutingContext context) {
        LOGGER.debug("Creating Entity Manager");
        EntityManager em = entityManagerFactory.createEntityManager();
        context.put(EventConfig.ENTITY_MANAGER, em).next();
    }

    private void releaseEntityManager(RoutingContext context) {
        LOGGER.debug("Closing Entity Manager");
        EntityManager em = context.get(EventConfig.ENTITY_MANAGER);
        try {
            if (em.getTransaction().isActive()) {
                LOGGER.debug("Committing transaction");
                em.getTransaction().commit();
            }
        } catch (Exception exception) {
            em.getTransaction().rollback();
            context.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), exception);
        } finally {
            em.close();
            LOGGER.debug("Entity manager closed");
        }
        context.next();
    }
}
