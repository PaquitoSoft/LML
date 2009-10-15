package org.paquitosoft.lml.model.facade;

import java.util.List;
import org.paquitosoft.lml.model.action.ActionProcessor;
import org.paquitosoft.lml.model.action.FindEntitiesAction;
import org.paquitosoft.lml.model.action.IAction;
import org.paquitosoft.lml.model.action.PersistAction;
import org.paquitosoft.lml.model.action.ReadEntityAction;
import org.paquitosoft.lml.model.exception.InternalErrorException;
import static org.paquitosoft.lml.util.LMLConstants.*;

/**
 *  This is the actual implementation class of public methods of this library.
 * 
 * @author paquitosoft
 */
public class LMLFacadeImpl implements ILMLFacade {

    /**
     * This method is used to save an entity in the database.
     * 
     * @param entity
     * @return saved entity
     * @throws InternalErrorException (DuplicateInstanceException, ReflectionException)
     */
    public <T> T save(T entity) throws InternalErrorException {
        IAction action = new PersistAction(entity, PERSIST_MODE_SAVE);
        return (T) new ActionProcessor(action).processAction();
    }
    
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
    public <T> T read(Class<T> entityType, Object entityId, Integer detailLevel) throws InternalErrorException {
        IAction action = new ReadEntityAction(entityType, entityId, detailLevel);
        return (T) new ActionProcessor(action).processAction();
    }
 
    /**
     * This method updates the information from an entity in the database.
     * 
     * @param entity
     * @return udpated entity
     * @throws InternalErrorException (DataNotFoundException, ReflectionException)
     */
    public <T> T update(T entity) throws InternalErrorException {
        IAction action = new PersistAction(entity, PERSIST_MODE_UPDATE);
        return (T) new ActionProcessor(action).processAction();
    }
    
    /**
     * This method removes an entity from the database.
     * 
     * @param entityType
     * @param entityId
     * @return removed entity
     * @throws InternalErrorException (DataNotFoundException, ReflectionException)
     */
    public <T> T remove(T entity) throws InternalErrorException {
        IAction action = new PersistAction(entity, PERSIST_MODE_DELETE);
        return (T) new ActionProcessor(action).processAction();
    }
    
    /**
     * This methods executes a custom query with provided parameters.
     * @param query
     * @param entityType
     * @param detailLevel
     * @param params
     * @return collection of entities
     * @throws InternalErrorException (DataNotFoundException, ReflectionException)
     */
    public <T> List<T> finder(String query, Class<T> entityType, Integer detailLevel, Object ... params) throws InternalErrorException {
        IAction action = new FindEntitiesAction(query, entityType, detailLevel, params);
        return new ActionProcessor(action).processAction();
    }
    
}
