package org.paquitosoft.lml.model.exception;

/**
 *  This exception is used to warn about an error while instrospecting an entity.
 * 
 * @author paquitosoft
 */
public class ReflectionException extends InternalErrorException {

    public ReflectionException(Throwable cause) {
        super(cause);
    }

    public ReflectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReflectionException(String message) {
        super(message);
    }

    public ReflectionException() {
    }

}
