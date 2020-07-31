package app.tandv.services.handler;

import app.tandv.services.util.DisposableHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonArray;
import io.vertx.reactivex.ext.web.RoutingContext;

import javax.persistence.PersistenceException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * @author vic on 2020-07-21
 */
abstract class AbstractDBHandler extends DisposableHandler {
    void errorHandler(RoutingContext context, Throwable exception) {
        int statusCode;
        if (exception instanceof PersistenceException) {
            statusCode = HttpResponseStatus.NOT_FOUND.code();
        } else if (exception instanceof IllegalArgumentException) {
            statusCode = HttpResponseStatus.BAD_REQUEST.code();
        } else {
            statusCode = HttpResponseStatus.INTERNAL_SERVER_ERROR.code();
        }
        context.fail(statusCode, exception);
    }

    <T> List<T> unwrapJsonArray(JsonArray array, BiFunction<JsonArray, Integer, T> mapper) {
        List<T> result = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            result.add(mapper.apply(array, i));
        }
        return result;
    }
}
