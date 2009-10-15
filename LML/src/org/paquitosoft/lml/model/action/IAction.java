package org.paquitosoft.lml.model.action;

import java.sql.Connection;
import org.paquitosoft.lml.model.exception.InternalErrorException;

/**
 *  This interface dictates the way an action must be called.
 * 
 * @author paquitosoft
 */
public interface IAction {

    /**
     * Every implementing class will have this method as the main one to deal 
     * with its logic.
     * 
     * @param connection
     * @return
     * @throws org.paquitosoft.lml.model.exception.InternalErrorException
     */
    <T> T execute(Connection connection) throws InternalErrorException;
    
}
