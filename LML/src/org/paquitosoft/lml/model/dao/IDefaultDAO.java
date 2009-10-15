package org.paquitosoft.lml.model.dao;

import java.util.List;
import org.paquitosoft.lml.model.exception.InternalErrorException;

/**
 *  This interface exposes the behaviour that defaultDAO implements.
 * 
 * @author paquitosoft
 */
public interface IDefaultDAO {

    /**
     * This method is intended to create a new record in the database.
     * It raises an exception when we try to insert a record with an already used identifier.
     * @param vo
     * @return T
     * @throws InternalErrorException (DuplicateInstanceException)
     */    
    <T> T insert(T entity) throws InternalErrorException;
    
    /**
     * This method is intended to read a record from the database by its identifier/s.
     * When the entity has a compund key, entityId is the entity itself filled with key values.
     * It raises an exception when the record is not founded.
     * @param identifiers
     * @return T
     * @throws InternalErrorException (DataNotFoundException, ReflectionException)
     */    
    <T> T read(Class<T> entityClass, Object entityId) throws InternalErrorException;
    
    /**
     * This method is inteded to delete a record from the database.
     * It raises an exception when no record, or more than one have been removed.
     * @param entityClass
     * @param entityId
     * @throws org.paquitosoft.lml.model.exception.InternalErrorException (ReflectionException)
     */
    <T> void remove(Class<T> entityClass, Object entityId) throws InternalErrorException;
    
    /**
     * This method is intended to update a single record in the database.
     * it raises an exception when the record we want to update is not present in 
     * the database (DataNotFoundException), or when more than one record has 
     * been updated (IncoherentUpdatingException)
     * @param entity
     * @return T
     * @throws InternalErrorException (DataNotFoundException, IncoherentUpdatingException)
     */
    <T> T update(T entity) throws InternalErrorException;
    
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
    <T> List<T> finder(Class<T> returnType, String query, Object ... values)
                    throws InternalErrorException;

    /**
     * This method is used to find associated entities where the relationship
     * is n-m.
     * 
     * @param <T>
     * @param sourceEntity
     * @param destinationEntityClass
     * @param joinTableName
     * @param sourceEntityId
     * @return
     * @throws InternalErrorException
     */
    <T> List<T> findJoinEntities(Object sourceEntity, Class<T> destinationEntityClass,
            String joinTableName) throws InternalErrorException;
}
