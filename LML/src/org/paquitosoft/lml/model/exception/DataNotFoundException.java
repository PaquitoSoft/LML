package org.paquitosoft.lml.model.exception;

/**
 *  This exception is used to warn about a read query that didn't 
 *  find any result.
 * 
 * @author paquitosoft
 */
public class DataNotFoundException extends InternalErrorException {

    public DataNotFoundException() {
        super();
    }
    
    public DataNotFoundException(String message) {
        super(message);
    }
    
    public DataNotFoundException(Throwable t) {
        super(t);
    }
    
    public DataNotFoundException(String message, Throwable t) {
        super(message, t);
    }

    
}
