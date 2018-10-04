package app.tandv.services.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * This class is needed to have Springboot add and recognize custom interceptors
 *
 * @author Vic on 9/5/2018
 **/
@Configuration
public class HandlerConfig implements WebMvcConfigurer {

    private final RequestLogging requestLogging;

    @Autowired
    public HandlerConfig(RequestLogging requestLogging) {
        this.requestLogging = requestLogging;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestLogging);
    }


}
