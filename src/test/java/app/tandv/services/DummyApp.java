package app.tandv.services;

import app.tandv.services.data.entity.AuthorEntity;
import app.tandv.services.data.entity.BookEntity;
import app.tandv.services.data.jpa.DummyJpaEntityManagerFactory;
import app.tandv.services.verticle.ServiceVerticle;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Promise;

import javax.persistence.EntityManagerFactory;

/**
 * @author vic on 2020-07-23
 */
class DummyApp extends App {
    private final int port;

    DummyApp(int port) {
        this.port = port;
    }

    @Override
    protected ConfigRetrieverOptions configOptions() {
        return super.configOptions()
                .addStore(
                        new ConfigStoreOptions()
                                .setType("json")
                                .setConfig(new JsonObject().put(ServiceVerticle.PORT_PROPERTY, port))
                );
    }

    @Override
    protected void entityManagerFactory(Promise<EntityManagerFactory> promise, JsonObject config) {
        try {
            promise.complete(
                    new DummyJpaEntityManagerFactory(
                            config,
                            BookEntity.class,
                            AuthorEntity.class
                    ).getFactory()
            );
        } catch (Throwable exception) {
            promise.fail(exception);
        }
    }
}
