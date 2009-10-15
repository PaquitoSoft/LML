package org.paquitosoft.lml.model.dao;

import java.sql.Connection;

/**
 *
 * @author paquitosoft
 */
public abstract class DAO {

    private Connection connection;
    
    public DAO(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        return connection;
    }
    
}
