package org.paquitosoft.lml.model.exception;

/**
 *  This exception encapsulates is the superclass of all 
 *  library's exceptions. 
 * 
 * @author paquitosoft
 */
public class InternalErrorException extends Exception {

    public InternalErrorException() {
        super();
    }
    
    public InternalErrorException(String message) {
        super(message);
    }
    
    public InternalErrorException(Throwable t) {
        super(t);
    }
    
    public InternalErrorException(String message, Throwable t) {
        super(message, t);
    }
    
}
