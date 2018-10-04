package app.tandv.services.aspect;

import app.tandv.services.data.entity.AuthorEntity;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author Vic on 9/18/2018
 **/
@Aspect
@Configuration
public class LoggingAspect {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingAspect.class);

    @Pointcut("execution(public app.tandv.services.model.response.LibraryResponse app.tandv.services.service.LibraryService.*(..))")
    public void allServiceMethods() {
    }

    @Around("allServiceMethods()")
    public Object aroundServiceCall(ProceedingJoinPoint joinPoint) throws Throwable {
        LOGGER.debug(joinPoint.getSignature().getName());
        if (LOGGER.isTraceEnabled()) {
            Object[] args = joinPoint.getArgs();
            for (Object arg : args) {
                LOGGER.trace(arg.toString());
            }
        }
        Object result = joinPoint.proceed();
        LOGGER.trace(joinPoint.getSignature().getName() + " " + result.toString());
        return result;
    }

    @Pointcut("execution(* app.tandv.services.service.LibraryService.responseWith*(..))")
    public void responseBuilders() {
    }

    @Before("responseBuilders()")
    public void beforeResponseBuilders() {
        LOGGER.debug("Building response");
    }

    @Pointcut("execution(public void app.tandv.services.data.entity.BookEntity.removeAuthor(app.tandv.services.data.entity.AuthorEntity))")
    public void removeAuthorFromBook() {
    }

    @Around("removeAuthorFromBook()")
    public Object aroundRemoveAuthorFromBook(ProceedingJoinPoint joinPoint) throws Throwable {
        AuthorEntity authorEntity = (AuthorEntity) joinPoint.getArgs()[0];
        LOGGER.debug("Removing author with id " + authorEntity.getId());
        Object result = joinPoint.proceed();
        LOGGER.debug("Author removed");
        return result;
    }

    @Pointcut("execution(public void app.tandv.services.data.entity.BookEntity.clearAuthors())")
    public void clearAuthorsFromBook() {
    }

    @Around("clearAuthorsFromBook()")
    public Object aroundClearAuthorsFromBook(ProceedingJoinPoint joinPoint) throws Throwable {
        LOGGER.debug("Removing book references from all authors, then removing authors from book");
        Object result = joinPoint.proceed();
        LOGGER.debug("Authors cleared");
        return result;
    }

    @Pointcut("execution(public !void app.tandv.services.data.repository.LibraryRepository.*(..))")
    public void repositoryNotVoidMethods() {
    }

    @Around("repositoryNotVoidMethods()")
    public Object aroundRepositoryNotVoidMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(joinPoint.getSignature().getName() + " " + Arrays.toString(joinPoint.getArgs()));
        }
        Object result = joinPoint.proceed();
        if (result != null && LOGGER.isDebugEnabled()) {
            if (result instanceof List) {
                LOGGER.debug(((List) result).size() + " entities found");
            } else if (result instanceof Optional) {
                LOGGER.debug("Entity found? " + ((Optional) result).isPresent());
            } else {
                LOGGER.trace(result.toString());
            }
        }
        return result;
    }

    @Pointcut("execution(public void app.tandv.services.data.repository.LibraryRepository.*(..))")
    public void repositoryVoidMethods() {
    }

    @Around("repositoryVoidMethods()")
    public Object aroundRepositoryVoidMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(joinPoint.getSignature().getName() + " " + Arrays.toString(joinPoint.getArgs()));
        }
        Object result = joinPoint.proceed();
        LOGGER.debug(joinPoint.getSignature().getName() + " completed");
        return result;
    }
}
