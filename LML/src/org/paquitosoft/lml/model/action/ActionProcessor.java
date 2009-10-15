package org.paquitosoft.lml.model.action;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.paquitosoft.lml.model.action.connection.ConnectionPool;
import org.paquitosoft.lml.model.exception.InternalErrorException;
import static org.paquitosoft.lml.util.LMLConstants.*;
import org.paquitosoft.lml.util.LMLGlobalOperations;

/**
 *  This class is responsible for executing every action. 
 *  It has to get a database connection, manage a transaction if 
 *  it's needed and execute the action.
 * 
 * @author paquitosoft
 */
public class ActionProcessor {

    Logger logger = Logger.getLogger(ActionProcessor.class.getName());
    
    private IAction action;

    private static String dataSourceJndiName = LMLGlobalOperations.getConnectionSetting(CONNECTION_SETTING_DATASOURCE_JNDI);
    private static DataSource dataSource;
    private static ConnectionPool connectionPool;
    
    public ActionProcessor(IAction action) throws InternalErrorException {
        this.action = action;
        if (dataSourceJndiName.length() > 0 && dataSource == null) {
            try {                
                Context ctx = new InitialContext();
                dataSource = (DataSource) ctx.lookup(dataSourceJndiName);                
            } catch (NamingException ex) {
                throw new InternalErrorException("ActionProcess::constructor -> Naming error while getting dataSource.", ex);
            } 
        } else if (dataSource == null) {
            connectionPool = ConnectionPool.getInstance();
        }
    }
    
    /**
     * This method is used to execute the action.
     * 
     * @return the result of executing the action
     * @throws org.paquitosoft.lml.model.exception.InternalErrorException
     */
    public <T> T processAction() throws InternalErrorException {
        
        T result = null;
        Connection conn = null;
    
        try {
            
            // Get a connection
            conn = getConnection();
            
            // Set transaction mode if needed
            if (action instanceof ITransactionalAction) {
                conn.setAutoCommit(false);
            }
            
            // Execute action
            result = (T) action.execute(conn);
            
            // Commit action when needed
            if (action instanceof ITransactionalAction) {
                conn.commit();
            }
            
        } catch (SQLException e) {
            
            // Rollback when needed
            if (action instanceof ITransactionalAction) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    throw new InternalErrorException("ActionProcessor::processAction -> Error while rolling back.", ex);
                }
            }            
            throw new InternalErrorException("ActionProcessor::processAction -> Error while processing action.", e);
            
        } catch (InternalErrorException e) {
            
            // Rollback when needed
            if (action instanceof ITransactionalAction) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    throw new InternalErrorException("ActionProcessor::processAction -> Error while rolling back.", ex);
                }
            }            
            throw e;
            
        } finally {
            
            // Commit when needed
            if (action instanceof ITransactionalAction) {
                try {
                    conn.commit();
                } catch (SQLException ex) {
                    throw new InternalErrorException("ActionProcessor::processAction -> Error while commiting.", ex);
                }
            }
            
            // Release connection
            if (conn != null) releaseConnection(conn);
            
        }
        
        return result;
    }
    
    /**
     * Gets a database connection.
     * 
     * @return connection
     * @throws org.paquitosoft.lml.model.exception.InternalErrorException
     */
    protected Connection getConnection() throws InternalErrorException {
        
        Connection result = null;
        
        // Check wether to get a connection from a server or from the connection pool
        if (dataSource != null) {
            try {
                result = dataSource.getConnection();            
            } catch (SQLException ex) {
                throw new InternalErrorException("ActionProcess::getConnection -> SQL error while getting dataSource connection", ex);
            }
        } else {
            result = connectionPool.getConnection();            
        }
        
        return result;
    }
    
    /**
     * Release a database connection.
     * 
     * @param connection to be released
     * @throws org.paquitosoft.lml.model.exception.InternalErrorException
     */
    protected void releaseConnection(Connection connection) throws InternalErrorException {
        // Check wether to give the connection back to dataSource or the connectionPool
        if (dataSource != null) {
            try {
                connection.close();
            } catch (SQLException ex) {
                throw new InternalErrorException("ActionProcess::releaseConnection -> SQL error while closing dataSource connection", ex);
            }
        } else {
            connectionPool.releaseConnection(connection);
        }        
    }
    
}
