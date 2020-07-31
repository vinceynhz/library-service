package app.tandv.services.handler;

import app.tandv.services.configuration.EventConfig;
import io.reactivex.disposables.CompositeDisposable;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This handler is in charge of finishing up a single request. It will close any opened resources and dispose of any
 * pending observers
 *
 * @author vic on 2020-07-20
 */
public class ResponseHandler implements Handler<RoutingContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseHandler.class);

    @Override
    public void handle(RoutingContext context) {
        JsonObject event = context.get(EventConfig.EVENT);

        // To dispose of any disposables captured during execution
        this.dispose(context.get(EventConfig.DISPOSABLES));

        int status = event.getInteger(EventConfig.STATUS);
        HttpServerResponse response = context.response();
        response.setStatusCode(status);
        if (event.containsKey(EventConfig.CONTENT)) {
            String responseContent = event.getString(EventConfig.CONTENT);
            LOGGER.trace(responseContent);
            response.end(responseContent);
        } else {
            response.end();
        }

        long st = event.getLong(EventConfig.ST, -1L);
        long tt = st != -1L ? System.currentTimeMillis() - st : st;
        LOGGER.info("Total time {}ms {}", tt, status);
    }

    private void dispose(CompositeDisposable compositeDisposable) {
        if (compositeDisposable != null) {
            LOGGER.debug("Disposing of pending observers...");
            compositeDisposable.dispose();
        }
    }
}
