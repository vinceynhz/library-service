package app.tandv.services.exception;

import javax.persistence.PersistenceException;

/**
 * @author vic on 2020-08-04
 */
public class PartialResultException extends PersistenceException {
    public PartialResultException(String message) {
        super(message);
    }
}
