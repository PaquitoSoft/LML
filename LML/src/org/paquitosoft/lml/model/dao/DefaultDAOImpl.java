package org.paquitosoft.lml.model.dao;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.paquitosoft.lml.model.annotation.PersistentEntity;
import org.paquitosoft.lml.model.annotation.PersistentAttribute;
import org.paquitosoft.lml.model.exception.AutogeneratedKeysReturnNotSupportedException;
import org.paquitosoft.lml.model.exception.DataNotFoundException;
import org.paquitosoft.lml.model.exception.DuplicateInstanceException;
import org.paquitosoft.lml.model.exception.InternalErrorException;
import org.paquitosoft.lml.model.exception.ReflectionException;
import org.paquitosoft.lml.model.util.ModelUtilities;

/**
 *  This is the main DAO in the library. 
 *  It implements functionality to CRUD operations and a finder method to 
 *  run custom queries.
 * 
 * @author paquitosoft
 */
public class DefaultDAOImpl implements IDefaultDAO {

    Logger logger = Logger.getLogger(getClass().getName());
    
     protected Connection conn;
     
    /** 
     * Creates a new instance of DAOGeneric.
     */
    public DefaultDAOImpl(Connection con) {
            this.conn = con;
    }
    
    /**
     * This method is intended to create a new record in the database.
     * It raises an exception when we try to insert a record with an already used identifier.
     * @param vo
     * @return T
     * @throws InternalErrorException (DuplicateInstanceException)
     */    
    public <T> T insert(T entity) throws InternalErrorException {
       
        T result = null;
        PreparedStatement stm = null;
        
        try {

            // Check the object does not already exisist
            Object entityId = ModelUtilities.getEntityIdentifier(entity);
            
            if (entityId != null) {
                    Object aux = null;
                    try {
                            aux = this.read(entity.getClass(), entityId);
                    } catch (DataNotFoundException e) {
                        // An exception must be thrown, otherwise means the entity already exists in database
                    }
                    if (aux != null) {
                            throw new DuplicateInstanceException("The entity you try to save is already in the database! -> " + entity);
                    }
            } 

            // Get the identifier when necessary
            if (entityId == null && entity.getClass().getAnnotation(PersistentEntity.class).generateKey()) {
                Object id = new IdentifierDAOImpl(conn).generateIdentifier(entity.getClass());
                List<Field> pkFields = ModelUtilities.getEntityIdentifierFields(entity.getClass());
                if (pkFields.size() == 1) {
                    pkFields.get(0).setAccessible(true);
                    pkFields.get(0).set(entity, id);
                } else {
                    // A compound key cannot be auto-generated
                    throw new InternalErrorException("DefaultDAOImpl::insert -> A compound key cannot be auto-generated. (" + entity + ")");
                }
            }
            
            // Get the query
            String query = ModelUtilities.getQueryForInsert(entity.getClass());
            String[] fieldNames = query.substring(query.indexOf('(') + 1, query.indexOf(')')).split(",");

            // Prepare query
            stm = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            
            // Fill query
            for (int i = 0; i < fieldNames.length; i++) {
                logger.log(Level.INFO, "Value we set into insert query: " + fieldNames[i] + " -- " + 
                        ModelUtilities.getFieldValue(fieldNames[i], entity));
                ModelUtilities.insertValueInQuery(ModelUtilities.getFieldValue(fieldNames[i], entity), stm, (i + 1));
            }

            // Execute insert query
            int i = stm.executeUpdate();

            // If database generated PK, we need to get it
            if (entityId == null && !entity.getClass().getAnnotation(PersistentEntity.class).generateKey()) {
                ResultSet pkRs = stm.getGeneratedKeys();
                if (pkRs != null) {
                    if (pkRs.next()) {
                        List<Field> pkFields = ModelUtilities.getEntityIdentifierFields(entity.getClass());
                        if (pkFields.size() == 1) {
                            Field pkField = pkFields.get(0);
                            pkField.setAccessible(true);
//                            ResultSetMetaData rsmd = pkRs.getMetaData();
                            pkField.set(entity, ModelUtilities.getValueFromQuery(pkField.getType(), pkRs, 1));
                        } else {
                            // A compound key cannot be auto-generated
                            throw new InternalErrorException("DefaultDAOImpl::insert -> A compound key cannot be auto-generated. (" + entity + ")");
                        }
                    } else {
                        throw new InternalErrorException("ERROR: COULD NOT RETRIEVE PK GENERATED BY DATBASE.");
                    }
                } else {
                    throw new AutogeneratedKeysReturnNotSupportedException();
                }
            }            
            
            // We must have inserted exactly one row
            if (i != 1) {
                    throw new InternalErrorException("ERROR: INSERT METHOD FAILED. " + i + " records have been inserted. (" + entity + ")");
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "SQL Exception while inserting: " + e.getMessage(), e);
            throw new InternalErrorException("Database Exception while inserting.... (" + entity + ")", e);
        } catch (IllegalAccessException e) {
            logger.log(Level.SEVERE, "Reflection exception while inserting (" + entity + "): " + e.getMessage(), e);
            throw new ReflectionException(e);
        } finally {
                closeResouces(stm, null);
        }
     
        result = entity; // The entity with its new identifier
        return result;
    }
    
    
    /**
     * This method is intended to read a record from the database by its identifier/s.
     * When the entity has a compund key, entityId is the entity itself filled with key values.
     * It raises an exception when the record is not founded.
     * @param identifiers
     * @return T
     * @throws InternalErrorException (DataNotFoundException, ReflectionException)
     */    
    public <T> T read(Class<T> entityClass, Object entityId) throws InternalErrorException {

        PreparedStatement stm = null;        
        ResultSet rs = null;
        T entity = null;

        try {
    
            // Get the query
            String query = ModelUtilities.getQueryForRead(entityClass);
        
            // Prepare query
            stm = conn.prepareStatement(query);
                
            // Populate query
            int index = 1;
            // When it is a compund key, we receive an entity with its key attributes filled
            if (entityId.getClass().isAnnotationPresent(PersistentEntity.class)) {
                // We get primary key fields the same order getQueryForRead() method did
                List<Field> pkFields = ModelUtilities.getEntityIdentifierFields(entityId.getClass());
                for (Field f : pkFields) {
                    ModelUtilities.insertValueInQuery(f.get(entityId), stm, index++);
                }
            } else {
                // When it is a single key
                ModelUtilities.insertValueInQuery(entityId, stm, index);
            }
                			
            // Execute query
            rs = stm.executeQuery();

            if (rs.next()) {
                    
                // Populate entity with result
                entity = ModelUtilities.createEntityWithValues(entityClass, rs, false);

            } else {
                throw new DataNotFoundException("Entity: " + entityClass + " with id: " + entityId.toString() + " has not been found");
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "SQL Exception while inserting: " + e.getMessage(), e);
            throw new InternalErrorException("Database Exception....", e);
        } catch (IllegalAccessException e) {
            logger.log(Level.SEVERE, "Reflection exception while inserting: " + e.getMessage(), e);
            throw new ReflectionException("DefaultDAO::read\n", e);
        } catch (IllegalArgumentException e) {
            logger.log(Level.SEVERE, "Reflection exception while inserting: " + e.getMessage(), e);
            throw new ReflectionException("DefaultDAO::read\n", e);
        } finally {
            closeResouces(stm, rs);
        }

        return entity;
    }

    /**
     * This method is inteded to delete a record from the database.
     * It raises an exception when no record, or more than one have been removed.
     * @param entityClass
     * @param entityId
     * @throws org.paquitosoft.lml.model.exception.InternalErrorException (ReflectionException)
     */
    public <T> void remove(Class<T> entityClass, Object entityId) throws InternalErrorException {
        
        PreparedStatement stm = null;
        
        try {
            
            // Get the query
            String query = ModelUtilities.getQueryForDelete(entityClass);
            
            // Prepare query
            stm = conn.prepareStatement(query);
            
            // Fill query
            int index = 1;
            // When it is a compund key, we receive an entity with its key attributes filled
            if (entityId.getClass().isAnnotationPresent(PersistentEntity.class)) {
                // We get primary key fields the same order getQueryForRead() method did
                List<Field> pkFields = ModelUtilities.getEntityIdentifierFields(entityId.getClass());
                for (Field f : pkFields) {
                    ModelUtilities.insertValueInQuery(f.get(entityId), stm, index++);
                }
            } else {
                // When it is a single key
                ModelUtilities.insertValueInQuery(entityId, stm, index);
            }

            // Execute insert query
            int i = stm.executeUpdate();

            // We must have inserted exactly one row
            if (i != 1) {
                    throw new InternalErrorException("ERROR: DELETE METHOD FAILED. " + i + " records have been deleted.");
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "SQL Exception while inserting: " + e.getMessage(), e);
            throw new InternalErrorException("Database Exception....", e);
        } catch (IllegalAccessException e) {
            logger.log(Level.SEVERE, "Reflection exception while inserting: " + e.getMessage(), e);
            throw new ReflectionException("DefaultDAO::remove\n", e);
        } finally {
                closeResouces(stm, null);
        }
        
    }

    /**
     * This method is intended to update a single record in the database.
     * it raises an exception when the record we want to update is not present in 
     * the database (DataNotFoundException), or when more than one record has 
     * been updated (IncoherentUpdatingException)
     * @param entity
     * @return T
     * @throws InternalErrorException (DataNotFoundException, IncoherentUpdatingException)
     */
    public <T> T update(T entity) throws InternalErrorException {

        PreparedStatement stm = null;

        try {

            // Get the query
            String query = ModelUtilities.getQueryForUpdate(entity.getClass());

            // Prepare query
            stm = conn.prepareStatement(query);

            // Fill entity values
            int index = 1;
            List<Field> attributes = ModelUtilities.getAllPersistentEntityFields(entity.getClass());
            for (Field f : attributes) {
                f.setAccessible(true);
                // If field is an entity entity we need to get its identifier
                if (f.getAnnotation(PersistentAttribute.class).entity()) {
                    ModelUtilities.insertValueInQuery(ModelUtilities.getEntityIdentifier(f.get(entity)), stm, index++); // TODO Does it worth it to check the identifier here?
                } else if (!ModelUtilities.isPKField(f)) {
                    ModelUtilities.insertValueInQuery(f.get(entity), stm, index++);
                }
            }

            // Get entity primaryKeys (we get the ids the same order the getQuery method did)
            List<Field> pkFields = ModelUtilities.getEntityIdentifierFields(entity.getClass());

            // Fill where clause
            for (Field pkField : pkFields) {
                pkField.setAccessible(true);
                ModelUtilities.insertValueInQuery(pkField.get(entity), stm, index++);
            }

            // Execute insert query
            int i = stm.executeUpdate();

            // We must have updated exactly one row
            if (i != 1) {
                    throw new InternalErrorException("ERROR: UPDATE METHOD FAILED. " + i + " records have been updated.");
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "SQL Exception while inserting: " + e.getMessage(), e);
            throw new InternalErrorException("Database Exception....", e);
        } catch (IllegalAccessException e) {
            logger.log(Level.SEVERE, "Reflection exception while inserting: " + e.getMessage(), e);
            throw new ReflectionException("DefaultDAO::update\n", e);
        } finally {
                closeResouces(stm, null);
        }

        return entity;
    }

    /**
     * This method is used to execute a parametized simple query.
     * (This method executes <b>simple</b> queries. If you need something more sophisticated, 
     * write your own subclass and overwrite this method or write a new one for your specific requirements).
     * 
     * @param returnType
     * @param query
     * @param values
     * @return entity collection of type T
     * @throws org.paquitosoft.lml.model.exception.InternalErrorException (DataNotFoundException)
     */
    public <T> List<T> finder(Class<T> returnType, String query, Object ... values)
                    throws InternalErrorException {

        List<T> result = new ArrayList<T>();
        PreparedStatement stm = null;
        ResultSet rs = null;
        query = query.toUpperCase();
        logger.log(Level.INFO, "Finder query to be executed: " + query);

        try {
                //Prepare query
                query = query.toUpperCase();
                stm = this.conn.prepareStatement(query);

                //Fill query
                if (values != null) {
                    int i = 1;
                    for (int j = 0; j < values.length; j++) {
                            ModelUtilities.insertValueInQuery(values[j], stm, i++);
                    }
                }

                //Excetue query
                rs = stm.executeQuery();

                while (rs.next()) {
                    result.add(ModelUtilities.createEntityWithValues(returnType, rs, true));
                }

                if (result.isEmpty()) {
                        throw new DataNotFoundException("DefaultDAO::finder -> Data not found!");
                }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "SQL Exception while finding: " + e.getMessage(), e);
            throw new InternalErrorException(e);
        } finally {
            closeResouces(stm, rs);
        }

        return result;
    }

    /**
     * This method is used to find associated entities where the relationship
     * is n-m.
     *
     * @param <T>
     * @param sourceEntity
     * @param destinationEntityClass
     * @param joinTableName     
     * @return associated destination entities
     * @throws InternalErrorException (SQLException)
     */
    public <T> List<T> findJoinEntities(Object sourceEntity, Class<T> destinationEntityClass,
            String joinTableName) throws InternalErrorException {

        List<T> result = new ArrayList<T>();
        PreparedStatement stm = null;
        ResultSet rs = null;

        String query = ModelUtilities.getJoinFinderQuery(sourceEntity.getClass(), destinationEntityClass, joinTableName);

        logger.log(Level.INFO, "FindJoinEntities query to be executed: " + query);

        try {
                //Prepare query
                query = query.toUpperCase();
                stm = this.conn.prepareStatement(query);

                // Populate query
                int index = 1;
                Object sourceEntityId = ModelUtilities.getEntityIdentifier(sourceEntity);
                // When it is a compund key, we receive an entity with its key attributes filled
                if (sourceEntityId.getClass().isAnnotationPresent(PersistentEntity.class)) {
                    // We get primary key fields the same order getQueryForRead() method did
                    List<Field> pkFields = ModelUtilities.getEntityIdentifierFields(sourceEntityId.getClass());
                    for (Field f : pkFields) {
                        ModelUtilities.insertValueInQuery(f.get(sourceEntityId), stm, index++);
                    }
                } else {
                    // When it is a single key
                    ModelUtilities.insertValueInQuery(sourceEntityId, stm, index);
                }

                //Excetue query
                rs = stm.executeQuery();

                while (rs.next()) {
                    result.add(ModelUtilities.createEntityWithValues(destinationEntityClass, rs, true));
                }

                if (result.isEmpty()) {
                        throw new DataNotFoundException("DefaultDAO::findJoinEntities -> Data not found!");
                }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "SQL Exception while finding: " + e.getMessage(), e);
            throw new InternalErrorException(e);
        } catch (IllegalAccessException e) {
            logger.log(Level.SEVERE, "Reflection exception while inserting: " + e.getMessage(), e);
            throw new ReflectionException("DefaultDAO::findJoinEntities \n", e);
        } finally {
            closeResouces(stm, rs);
        }

        return result;
    }

    /**
     * Closes resources used when interacting with the database.
     * @param stm
     * @param rs
     * @throws InternalErrorException
     */
    protected void closeResouces(PreparedStatement stm, ResultSet rs) throws InternalErrorException {
        if (stm != null) {
            try {
                stm.close();
            } catch (Exception exception) {
                logger.log(Level.SEVERE, "Close statement exception while reading from database: " + exception.getMessage(), exception);
                throw new InternalErrorException(exception);
            }
        }
        if (rs != null) {
            try {
                rs.close();
            } catch (Exception exception) {
                logger.log(Level.SEVERE, "Close resultSet exception while reading from database: " + exception.getMessage(), exception);
                throw new InternalErrorException(exception);
            }
        }
    }
}
