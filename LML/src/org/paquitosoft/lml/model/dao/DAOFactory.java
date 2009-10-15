package org.paquitosoft.lml.model.dao;

import java.sql.Connection;

/**
 *  This class is the factory for creating every DAO used by the library.
 * 
 * @author paquitosoft
 */
public class DAOFactory {

    /**
     *  This method is used to create a new instance of default dao.
     * 
     * @param connection
     * @return default dao
     */
    public final static IDefaultDAO getDefaultDAO(Connection connection) {
        return new DefaultDAOImpl(connection);
    }
    
    /**
     *  This method is used to create a new instance of identifier dao.
     * 
     * @param connection
     * @return identifier dao
     */
    public final static IIdentifierDAO getIdentifierDAO(Connection connection) {
        return new IdentifierDAOImpl(connection);
    }
}
