package org.paquitosoft.lml.model.exception;

/**
 *  This exception is used to warn about a dirty update.
 * @author paquitosoft
 */
public class IncoherentUpdatingException extends InternalErrorException {

    public IncoherentUpdatingException() {
        super();
    }
    
    public IncoherentUpdatingException(String message) {
        super(message);
    }
    
    public IncoherentUpdatingException(Throwable t) {
        super(t);
    }
    
    public IncoherentUpdatingException(String message, Throwable t) {
        super(message, t);
    }
    
}
