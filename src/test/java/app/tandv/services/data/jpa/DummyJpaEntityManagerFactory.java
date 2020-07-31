package app.tandv.services.data.jpa;

import io.vertx.core.json.JsonObject;
import org.hibernate.cfg.Environment;

import java.util.Properties;

/**
 * @author vic on 2020-07-27
 */
public class DummyJpaEntityManagerFactory extends JpaEntityManagerFactory {
    public DummyJpaEntityManagerFactory(JsonObject config, Class... entityClasses) {
        super(config, entityClasses);
    }

    @Override
    protected Properties getProperties() {
        Properties defaultProperties = super.getProperties();
        defaultProperties.put(Environment.HBM2DDL_AUTO, "create-drop");
        return defaultProperties;
    }
}
