package app.tandv.services.data.jpa;

import io.vertx.core.json.JsonObject;
import org.hibernate.cfg.Environment;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author vic on 2020-07-23
 */
public class JpaEntityManagerFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaEntityManagerFactory.class);

    private static final String DB_URL_PROPERTY = "db.url";
    private static final String DB_HOST_PROPERTY = "db.host";
    private static final String DB_USER_PROPERTY = "db.username";
    private static final String DB_SECRET_PROPERTY = "db.secret";
    private static final String DB_CLASSNAME_PROPERTY = "db.class-name";

    private static final String DB_POOL_SIZE_PROPERTY = "db.pool.size";
    private static final String DB_CONNECTION_TIMEOUT_PROPERTY = "db.timeout.connection";
    private static final String DB_LEAK_DETECTION_THRESHOLD_PROPERTY = "db.timeout.leak";
    private static final String DB_CONNECTION_VALIDATION_TIMEOUT_PROPERTY = "db.timeout.conn-validation";

    private static final String DEFAULT_DB_HOST = "localhost";
    private static final int DEFAULT_POOL_SIZE = 10;
    // in milliseconds
    private static final long DEFAULT_CONNECTION_TIMEOUT = 5000;
    private static final long DEFAULT_LEAK_THRESHOLD = 3000;
    private static final long DEFAULT_CONNECTION_VALIDATION = 1500;

    private final Class[] entityClasses;
    private final JsonObject config;

    private EntityManagerFactory entityManagerFactory = null;

    public JpaEntityManagerFactory(JsonObject config, Class... entityClasses) {
        this.config = config;
        this.entityClasses = entityClasses;
    }

    public EntityManagerFactory getFactory() {
        if (this.entityManagerFactory == null) {
            LOGGER.info("Creating entity manager factory");
            LOGGER.debug("Creating hibernate properties");
            Properties properties = this.getProperties();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Hibernate properties created");
                LOGGER.debug(properties.toString());
            }
            PersistenceUnitInfo persistenceUnitInfo = new HibernatePersistenceUnitInfo(
                    this.getClass().getSimpleName(),
                    this.classEntities(),
                    properties
            );
            Map<String, Object> configuration = Collections.emptyMap();
            this.entityManagerFactory = new EntityManagerFactoryBuilderImpl(
                    new PersistenceUnitInfoDescriptor(persistenceUnitInfo),
                    configuration
            ).build();
            LOGGER.info("Entity manager factory created");
        }
        return this.entityManagerFactory;
    }

    private List<String> classEntities() {
        return Arrays.stream(entityClasses)
                .map(Class::getName)
                .collect(Collectors.toList());
    }

    /**
     * Provided as protected for extension
     *
     * @return properties of the current hibernate setup
     */
    Properties getProperties() {
        Properties jpaProperties = new Properties();

        String url = this.getRequiredProperty(DB_URL_PROPERTY)
                .replace("{host}", config.getString(DB_HOST_PROPERTY, DEFAULT_DB_HOST));

        jpaProperties.put(Environment.CONNECTION_PROVIDER, "com.zaxxer.hikari.hibernate.HikariConnectionProvider");

        jpaProperties.put("hibernate.hikari.dataSource.url", url);
        jpaProperties.put("hibernate.hikari.dataSource.user", this.getRequiredProperty(DB_USER_PROPERTY));
        jpaProperties.put("hibernate.hikari.dataSource.password", this.getRequiredProperty(DB_SECRET_PROPERTY));
        jpaProperties.put("hibernate.hikari.dataSourceClassName", this.getRequiredProperty(DB_CLASSNAME_PROPERTY));

        int poolSize = this.config.getInteger(DB_POOL_SIZE_PROPERTY, DEFAULT_POOL_SIZE);
        long connectionTimeout = this.config.getLong(DB_CONNECTION_TIMEOUT_PROPERTY, DEFAULT_CONNECTION_TIMEOUT);
        long leakThreshold = this.config.getLong(DB_LEAK_DETECTION_THRESHOLD_PROPERTY, DEFAULT_LEAK_THRESHOLD);
        long validationTimeout = this.config.getLong(DB_CONNECTION_VALIDATION_TIMEOUT_PROPERTY, DEFAULT_CONNECTION_VALIDATION);

        jpaProperties.put("hibernate.hikari.maximumPoolSize", String.valueOf(poolSize));
        jpaProperties.put("hibernate.hikari.connectionTimeout", String.valueOf(connectionTimeout));
        jpaProperties.put("hibernate.hikari.leakDetectionThreshold", String.valueOf(leakThreshold));
        jpaProperties.put("hibernate.hikari.validationTimeout", String.valueOf(validationTimeout));

        boolean showSql = this.config.getBoolean(Environment.SHOW_SQL, true);
        boolean queryCache = this.config.getBoolean(Environment.USE_QUERY_CACHE, false);
        boolean secondLevelCache = this.config.getBoolean(Environment.USE_SECOND_LEVEL_CACHE, false);

        jpaProperties.put(Environment.SHOW_SQL, showSql);
        jpaProperties.put(Environment.USE_QUERY_CACHE, queryCache);
        jpaProperties.put(Environment.USE_SECOND_LEVEL_CACHE, secondLevelCache);
        jpaProperties.put(Environment.USE_NEW_ID_GENERATOR_MAPPINGS, false);

        return jpaProperties;
    }

    private String getRequiredProperty(String property) {
        return Optional.ofNullable(this.config.getString(property))
                .orElseThrow(() -> new IllegalArgumentException("Required property '" + property + "' not found in current application configuration"));
    }
}
