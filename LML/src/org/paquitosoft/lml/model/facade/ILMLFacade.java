package org.paquitosoft.lml.model.facade;

import java.util.List;
import org.paquitosoft.lml.model.exception.InternalErrorException;

/**
 *  This interface exposes the behaviour of this library public facade implementation.
 * 
 * @author paquitosoft
 */
public interface ILMLFacade {

    /**
     * This method is used to save an entity in the database.
     * 
     * @param entity
     * @return saved entity
     * @throws InternalErrorException (DuplicateInstanceException, ReflectionException)
     */
    <T> T save(T entity) throws InternalErrorException;
    
     /**
     * This method reads an entity from the database. detailLevel param determines the 
     * amount of related info that must be read.
     * 
     * @param entityType
     * @param entityId
     * @param detailLevel
     * @return entity read
     * @throws InternalErrorException (DataNotFoundException, ReflectionException)
     */
    <T> T read(Class<T> entityType, Object entityId, Integer detailLevel) throws InternalErrorException;
    
    /**
     * This method updates the information from an entity in the database.
     * 
     * @param entity
     * @return udpated entity
     * @throws InternalErrorException (DataNotFoundException, ReflectionException)
     */
    <T> T update(T entity) throws InternalErrorException;
    
     /**
     * This method removes an entity from the database.
     * 
     * @param entityType
     * @param entityId
     * @return removed entity
     * @throws InternalErrorException (DataNotFoundException, ReflectionException)
     */
    <T> T remove(T entity) throws InternalErrorException;
    
    /**
     * 
     * @param query
     * @param entityType
     * @param detailLevel
     * @param params
     * @return collection of entities
     * @throws InternalErrorException (DataNotFoundException, ReflectionException)
     */
    <T> List<T> finder(String query, Class<T> entityType, Integer detailLevel, Object ... params) throws InternalErrorException;
    
}
