package app.tandv.services;

import app.tandv.services.data.entity.ContributorEntity;
import app.tandv.services.data.entity.BookEntity;
import app.tandv.services.data.jpa.JpaEntityManagerFactory;
import app.tandv.services.verticle.ServiceVerticle;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.Promise;
import io.vertx.reactivex.core.Vertx;
import org.hibernate.exception.JDBCConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This is the starting point for the Vert.x application
 * <p>
 * https://vertx.io/docs/vertx-core/java/
 */
public class App {
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    private EntityManagerFactory emf;
    private boolean started = false;
    private Disposable deployment;

    private Vertx vertx;

    App() {
        // Check this though. You are my favorite line
        this.vertx = Vertx.vertx();
    }

    public static void main(String[] args) {
        App app = new App();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down gracefully");
            app.close();
        }));
        app.startUp();
    }

    void startUp() {
        LOGGER.info("Starting application {}", this.getClass().getName());
        long st = System.currentTimeMillis();
        ConfigRetriever retriever = ConfigRetriever.create(vertx, configOptions());
        this.deployment = retriever.rxGetConfig()
                .doOnError(cause -> LOGGER.error("Unable to retrieve configuration", cause))
                .flatMap(config -> this.rxEntityManagerFactory(vertx, config))
                .map(config -> new DeploymentOptions().setConfig(config))
                .flatMap(config -> vertx.rxDeployVerticle(new ServiceVerticle(this.emf), config))
                .subscribe(
                        id -> {
                            LOGGER.info(
                                    "Application started in {}s",
                                    TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - st)
                            );
                            this.started = true;
                        },
                        error -> LOGGER.error("Unable to start verticles", error)
                );
    }

    boolean isStarted() {
        return this.started;
    }

    /**
     * We provide this method as protected to allow for extension. On a test class for example, the test application
     * may add an extra config file on a higher hierarchy than the ones added by default.
     *
     * @return the options for the config retriever.
     */
    ConfigRetrieverOptions configOptions() {
        List<ConfigStoreOptions> configStores = new ArrayList<>();
        configStores.add(new ConfigStoreOptions()
                .setType("file")
                .setFormat("properties")
                .setOptional(true)
                .setConfig(new JsonObject().put("path", "application.properties"))
        );
        configStores.add(new ConfigStoreOptions().setType("env"));
        configStores.add(new ConfigStoreOptions().setType("sys"));

        return new ConfigRetrieverOptions().setStores(configStores);
    }

    /**
     * We provide this method as protected to allow for extension. On a test class for example, the test application
     * may want to use a different version of the entity manager factory.
     *
     * @param promise to fulfill once the entity manager factory is created
     * @param config  of the current application
     */
    void entityManagerFactory(Promise<EntityManagerFactory> promise, JsonObject config) {
        try {
            promise.complete(
                    new JpaEntityManagerFactory(
                            config,
                            BookEntity.class,
                            ContributorEntity.class
                    ).getFactory()
            );
        } catch (Throwable exception) {
            promise.fail(exception);
        }
    }

    private Single<JsonObject> rxEntityManagerFactory(Vertx vertx, JsonObject config) {
        return vertx
                .<EntityManagerFactory>rxExecuteBlocking(promise -> this.entityManagerFactory(promise, config))
                .map(entityManagerFactory -> {
                    this.emf = entityManagerFactory;
                    return config;
                }).toSingle();
    }

    int close() {
        int rc = 0;
        if (this.emf != null && this.emf.isOpen()) {
            LOGGER.debug("Closing Entity Manager Factory");
            try {
                this.emf.close();
                LOGGER.debug("Entity Manager Factory closed");
            } catch (JDBCConnectionException exception) {
                LOGGER.warn("Error closing Entity Manager Factory", exception);
                rc = -1;
            }
        }
        if (this.deployment != null && !this.deployment.isDisposed()) {
            LOGGER.debug("Disposing deployment");
            this.deployment.dispose();
            LOGGER.debug("Deployment disposed");
        }
        LOGGER.debug("Closing Vert.x");
        vertx.close();
        LOGGER.debug("Vert.x closed");
        LOGGER.info("Application finished");
        return rc;
    }
}
