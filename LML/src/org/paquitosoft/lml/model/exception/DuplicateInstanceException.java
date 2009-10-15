package org.paquitosoft.lml.model.exception;

/**
 *  This instance is used to warn about an entity that has been tried to be 
 *  saved in the database with an already used identifier.
 * 
 * @author paquitosoft
 */
public class DuplicateInstanceException extends InternalErrorException {

     public DuplicateInstanceException() {
        super();
    }
    
    public DuplicateInstanceException(String message) {
        super(message);
    }
    
    public DuplicateInstanceException(Throwable t) {
        super(t);
    }
    
    public DuplicateInstanceException(String message, Throwable t) {
        super(message, t);
    }
    
}
