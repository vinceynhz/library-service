package app.tandv.services.inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Vic on 10/3/2018
 **/
@Configuration
public class BeanFactory {
    private static final String VERSION = "version";
    private static final String TIMESTAMP = "build.timestamp";
    private static final String POM_VERSION = "pom." + VERSION;
    private static final String POM_TIMESTAMP = "pom." + TIMESTAMP;
    private final Environment environment;

    @Autowired
    public BeanFactory(Environment environment) {
        this.environment = environment;
    }

    @Bean
    @Qualifier("build.properties")
    public Map<String, String> buildPropertiesInjector() {
        Map<String, String> buildProperties = new HashMap<>();
        buildProperties.put(VERSION, environment.getProperty(POM_VERSION));
        buildProperties.put(TIMESTAMP, environment.getProperty(POM_TIMESTAMP));
        return buildProperties;
    }
}
