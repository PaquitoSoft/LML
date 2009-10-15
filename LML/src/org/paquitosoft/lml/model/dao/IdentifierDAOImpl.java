package org.paquitosoft.lml.model.dao;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.paquitosoft.lml.model.exception.InternalErrorException;
import org.paquitosoft.lml.model.exception.ReflectionException;
import org.paquitosoft.lml.model.util.ModelUtilities;

/**
 *  This is the DAO used to deal with entity identifiers generation.
 * 
 * @author paquitosoft
 */
public class IdentifierDAOImpl extends DefaultDAOImpl implements IIdentifierDAO {

//    private static Logger logger = Logger.getLogger(IdentifierDAOImpl.class.getName());
    
    private static final int MAX_CHAR_VALUE = 255;

    private static final String CHECK_TABLE_EXISTS_QUERY = "SELECT * FROM ENTITY_KEYS";
    private static final String CREATE_TABLE_QUERY = "CREATE TABLE ENTITY_KEYS ( TABLE_NAME VARCHAR(45) NOT NULL, NEXT_VALUE VARCHAR(75) NOT NULL, PRIMARY KEY (TABLE_NAME))";
    private static final String READ_COUNTER_QUERY = "SELECT NEXT_VALUE FROM ENTITY_KEYS WHERE TABLE_NAME = ?";
    private static final String INSERT_COUNTER_QUERY = "INSERT INTO ENTITY_KEYS (TABLE_NAME, NEXT_VALUE) VALUES (?, ?)";
    private static final String UPDATE_COUNTER_QUERY = "UPDATE ENTITY_KEYS SET NEXT_VALUE = ? WHERE TABLE_NAME = ?";
    
    public IdentifierDAOImpl(Connection connection) {
        super(connection);
    }
    
    public Object generateIdentifier(Class entityType) throws InternalErrorException {
        
        Object result = null;
        PreparedStatement stm = null;
        ResultSet rs = null;
        
        // Get table name
        String tableName = ModelUtilities.getTableName(entityType);
        
        try {
            
            // Get identifier type
            Class pkType = ModelUtilities.getEntityIdType(entityType);
            
            // First we need to ensure the existence of the table
            try {                
                stm = conn.prepareStatement(CHECK_TABLE_EXISTS_QUERY);
                stm.executeQuery();
            } catch (SQLException e) {
                // An exception here means the table does not exists. Let's create it
                stm = conn.prepareStatement(CREATE_TABLE_QUERY);
                stm.executeUpdate();
            }

            // Now we get next available value
            stm = conn.prepareStatement(READ_COUNTER_QUERY);
            stm.setString(1, tableName);
            rs = stm.executeQuery();
            if (rs.next()) {
                result = ModelUtilities.getValueFromQuery(pkType, rs, "NEXT_VALUE");
            } else {
                // This means there is no row for this entity. Let's create it                
                result = getNextIdValue(getLastEntityIdentifier(entityType, conn), pkType); // We need to search entity's table because of the chance the table is not emtpy.
                stm = conn.prepareStatement(INSERT_COUNTER_QUERY);
                stm.setString(1, tableName);                
                ModelUtilities.insertValueInQuery(result, stm, 2);
                if (stm.executeUpdate() != 1) {
                    throw new InternalErrorException("IdentifierDAOImpl::generateIdentifier -> Insert new row failed!");
                }
            }
            
            // Update next available value
            stm = conn.prepareStatement(UPDATE_COUNTER_QUERY);
            ModelUtilities.insertValueInQuery(getNextIdValue(result, pkType), stm, 1);
            stm.setString(2, tableName);            
            if (stm.executeUpdate() != 1) {
                throw new InternalErrorException("IdentifierDAOImpl::generateIdentifier -> Update next available value failed!");
            }
            
        } catch (SQLException e) {
            throw new InternalErrorException("IdentifierDAOImpl::generateIdentifier", e);
        } catch (Exception e) {
            throw new InternalErrorException("IdentifierDAOImpl::generateIdentifier", e);
        } finally {
            closeResouces(stm, rs);
        }
        
        return result;
    }
    
    /**
     * This method is used to generate the next value for the identifier.
     * 
     * @param id
     * @return next identifier value
     */
    private Object getNextIdValue(Object id, Class idType) {
        
        Object result = null;
        
        if (Double.class.isAssignableFrom(idType)) {
            result = (id != null) ? ((Double) id).doubleValue() + 1 : 1;
        } else if (Float.class.isAssignableFrom(idType)) {
            result = (id != null) ? ((Float) id).floatValue() + 1 : 1;
        } else if (Long.class.isAssignableFrom(idType)) {
            result = (id != null) ? ((Long) id).longValue() + 1 : 1;
        } else if (Integer.class.isAssignableFrom(idType)) {
            result = (id != null) ? ((Integer) id).intValue() + 1 : 1;
        } else if (Short.class.isAssignableFrom(idType)) {
            result = (id != null) ? ((Short) id).shortValue() + 1 : 1;
        } else if (Byte.class.isAssignableFrom(idType)) {
            result = (id != null) ? ((Byte) id).byteValue() + 1 : 1;
        } else if (BigDecimal.class.isAssignableFrom(idType)) {
            result = (id != null) ? ((BigDecimal) id).add(BigDecimal.ONE) : 1;
        } else if (String.class.isAssignableFrom(idType)) {
            if (id != null) {
                StringBuilder currentId = new StringBuilder((String) id);
                char lastChar = currentId.charAt(currentId.length() - 1);
                if (lastChar >= MAX_CHAR_VALUE) {
                    currentId.append(1);
                } else {
                    currentId.replace(currentId.length() - 1, currentId.length(), String.valueOf(1));
                }
                result = currentId;
            } else {
                result = new String(new char[] {1});
            }
        } else if (Character.class.isAssignableFrom(idType)) {
            char charId = 1;
            if (id != null) {
                charId = ((Character) id).charValue();
                if (charId >= MAX_CHAR_VALUE) {
                    charId = 1; // TODO Ojito con esto. Al finalizar con los caracteres disponibles, se inicia desde el principio (puede haber sido previamente utilizado)
                } else {
                    charId += 1;
                }                
            } 
            result = new Character(charId);
        }
        
        return result;
    }
 
    /**
     * This method is used to get the last used identifier for this entity from its 
     * corresponding table. <br/>
     * Used to initialize identifiers table.
     * 
     * @param entityType
     * @return query
     * @throws org.paquitosoft.lml.model.exception.InternalErrorException
     */
    protected Object getLastEntityIdentifier(Class entityType, Connection connection) throws InternalErrorException {
    
        Object result = null;
        
        StringBuilder sb = new StringBuilder("SELECT ");
        List<Field> pkFields = ModelUtilities.getEntityIdentifierFields(entityType);
        String idColumnName = "";
        if (pkFields.size() == 1) {
            idColumnName = ModelUtilities.getColumnName(pkFields.get(0));
        } else {
            throw new ReflectionException("IdentifierDAOImpl::getFirsAvailableIdentifier -> Cannot generate coumpund keys");
        }
        sb.append(idColumnName);
        sb.append(" FROM ");
        sb.append(ModelUtilities.getTableName(entityType));
        sb.append(" ORDER BY ");
        sb.append(idColumnName);
        sb.append(" DESC");
        
        try {
            PreparedStatement stm = connection.prepareStatement(sb.toString());
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                result = ModelUtilities.getValueFromQuery(pkFields.get(0).getType(), rs, idColumnName);
            }
        } catch (SQLException e) {
            throw new InternalErrorException("IdentiiferDAOImpl::getLastEntityIdentifier -> Error while looking for the last used entity " +
                    "identifier in its table.", e);
        }
        
        return result;
    }

}