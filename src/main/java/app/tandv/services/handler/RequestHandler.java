package app.tandv.services.handler;

import app.tandv.services.configuration.EventConfig;
import io.reactivex.disposables.CompositeDisposable;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * @author vic on 2020-07-20
 */
public class RequestHandler implements Handler<RoutingContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestHandler.class);

    @Override
    public void handle(RoutingContext context) {
        LOGGER.info("Request received {}", context.request().uri());
        // Here we'll add a nice way to handle disposing of any subscriber created during the execution of this request
        context.put(EventConfig.DISPOSABLES, new CompositeDisposable());
        JsonObject event = this.getRequestEvent();
        context.put(EventConfig.EVENT, event);
        context.response().putHeader(EventConfig.UID, event.getString(EventConfig.UID));
        context.next();
    }

    private JsonObject getRequestEvent(){
        return new JsonObject()
                .put(EventConfig.UID, UUID.randomUUID().toString())
                .put(EventConfig.ST, System.currentTimeMillis());
    }
}
