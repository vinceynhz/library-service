package app.tandv.services.data.handler;

import app.tandv.services.exception.PartialResultException;
import app.tandv.services.util.DisposableHandler;
import app.tandv.services.util.collections.FluentHashMap;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonArray;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.hibernate.exception.ConstraintViolationException;

import javax.persistence.NoResultException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * @author vic on 2020-07-21
 */
abstract class LibraryHandler extends DisposableHandler {
    private static final Map<String, Integer> EXCEPTION_STATUS_CODE = new FluentHashMap<String, Integer>()
            .thenPut(IllegalArgumentException.class.getSimpleName(), HttpResponseStatus.BAD_REQUEST.code())
            .thenPut(NoResultException.class.getSimpleName(), HttpResponseStatus.NOT_FOUND.code())
            .thenPut(ConstraintViolationException.class.getSimpleName(), HttpResponseStatus.CONFLICT.code())
            .thenPut(PartialResultException.class.getSimpleName(), HttpResponseStatus.NOT_FOUND.code());

    void errorHandler(RoutingContext context, Throwable exception) {
        Throwable cause = exception.getCause();
        String exceptionName = cause == null ? exception.getClass().getSimpleName() : cause.getClass().getSimpleName();
        int statusCode = EXCEPTION_STATUS_CODE.getOrDefault(exceptionName, HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
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
