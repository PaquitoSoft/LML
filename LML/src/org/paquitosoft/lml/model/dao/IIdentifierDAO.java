package org.paquitosoft.lml.model.dao;

import org.paquitosoft.lml.model.exception.InternalErrorException;

/**
 *  This interface exposes the behaviour that identifierDAO implements.
 * 
 * @author paquitosoft
 */
public interface IIdentifierDAO {

    Object generateIdentifier(Class entityType) throws InternalErrorException;
    
}
