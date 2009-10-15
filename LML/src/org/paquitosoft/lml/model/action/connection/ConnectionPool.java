package org.paquitosoft.lml.model.action.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import org.paquitosoft.lml.model.exception.InternalErrorException;
import static org.paquitosoft.lml.util.LMLConstants.*;
import org.paquitosoft.lml.util.LMLGlobalOperations;


/**
 *
 * @author paquitosoft
 */
public class ConnectionPool {
    
    private static ConnectionPool instance;
    
    private final static Byte AVAILABLE = 1;
    
    private final static Byte BUSY = 2;
    
    private static HashMap<Connection,Byte> pool = new HashMap<Connection,Byte>();
    
    /**
     * Private constructor because we want to use this class as a singleton
     */
    private ConnectionPool() {                
    }
    
    /**
     * This method always returns the same instance of the pool.
     * The pool is initialized at this time with the parameters founded in 
     * the <b>lml-conn-settings</b> file.
     * @return
     * @throws org.paquitosoft.lml.model.exception.InternalErrorException
     */
    public static ConnectionPool getInstance() throws InternalErrorException {
        
        if (instance == null) {
            try {
                instance = new ConnectionPool();
                Class.forName(LMLGlobalOperations.getConnectionSetting(CONNECTION_SETTINGS_DRIVER).trim());
                initPool();
            } catch (ClassNotFoundException ex) {
                instance = null; // If initialization fails we need to set null to instance in order to recreate it next time we try; otherwise it won't invoke initPool method again
                throw new InternalErrorException("ConnectionPool::getInstance -> Could not load driver: " + 
                        LMLGlobalOperations.getConnectionSetting(CONNECTION_SETTINGS_DRIVER).trim(), ex);
            } catch (SQLException ex) {
                instance = null; // If initialization fails we need to set null to instance in order to recreate it next time we try; otherwise it won't invoke initPool method again
                throw new InternalErrorException("ConnectionPool::getInstance -> SQL problem.", ex);
            }
            
        }
        
        return instance;
    }
    
    /**
     * This method establishes a new connection with the database with the 
     * parameters in the <b>lml-conn-settings</b> file.
     * @return connection
     * @throws java.sql.SQLException
     */
    protected static Connection openConnection() throws SQLException {        
        Connection result = null;
        String url = LMLGlobalOperations.getConnectionSetting(CONNECTION_SETTINGS_URL);
        String user = LMLGlobalOperations.getConnectionSetting(CONNECTION_SETTINGS_USER);
        String password = LMLGlobalOperations.getConnectionSetting(CONNECTION_SETTINGS_PASSWORD);
        result = DriverManager.getConnection(url, user, password);
        return result;
    }
    
    /**
     * This method initialized the pool with as much connections as defined in the <b>lml-conn-settings</b> file.
     * @throws java.sql.SQLException
     */
    protected static void initPool() throws SQLException {
        
        for (int i=0; i < Integer.parseInt(LMLGlobalOperations.getConnectionSetting(CONNECTION_SETTINGS_POOL_SIZE)); i++) {
            pool.put(openConnection(), AVAILABLE);
        }
        
    }
    
    /**
     * This method looks for an available connection in the pool, marks it as busy and the returns it.
     * 
     * @return connection
     */
    public Connection getConnection() {
        // TODO Devuelve null cuando no quedan conexiones disponibles. El pool deberia crecer de forma temporal.
        Connection result = null;
        
        for (Connection conn : pool.keySet()) {
            if (pool.get(conn).equals(AVAILABLE)) {
                pool.remove(conn);
                pool.put(conn, BUSY);
                result = conn;
                break;
            }
        }
        
        return result;
    }
    
    /**
     * This method marks the connection as available again.
     * We don't close connections.
     * 
     * @param connection
     */
    public void releaseConnection(Connection connection) {
        
        pool.remove(connection);
        pool.put(connection, AVAILABLE);
        
    }
    
}
