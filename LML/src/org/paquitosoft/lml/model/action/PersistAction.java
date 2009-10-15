package org.paquitosoft.lml.model.action;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.paquitosoft.lml.model.util.ModelUtilities;
import org.paquitosoft.lml.model.annotation.AssociationType;
import org.paquitosoft.lml.model.annotation.PersistentAttribute;
import org.paquitosoft.lml.model.annotation.AssociatedEntityList;
import org.paquitosoft.lml.model.annotation.PersistentEntity;
import org.paquitosoft.lml.model.dao.DAOFactory;
import org.paquitosoft.lml.model.dao.IDefaultDAO;
import org.paquitosoft.lml.model.exception.DataNotFoundException;
import org.paquitosoft.lml.model.exception.DuplicateInstanceException;
import org.paquitosoft.lml.model.exception.InternalErrorException;
import org.paquitosoft.lml.model.exception.ReflectionException;
import static org.paquitosoft.lml.util.LMLConstants.*;

/**
 *  This action is used to create, update or delete an entity in the database.
 * 
 * @author paquitosoft
 */
public class PersistAction<T> implements ITransactionalAction {

    Logger logger = Logger.getLogger(PersistAction.class.getName());
    
    private T entity;
    
    private byte mode;
    
    /**
     * Constructor dictates params the action needs to be executed.
     * <b>mode</b> is one of the constants defined in <code>LMLConstants</code>.
     * @param entity
     * @param mode
     */
    public PersistAction(T entity, byte mode) {
        this.entity = entity;
        this.mode = mode;
    }
    
    /**
     * 
     * @param connection
     * @return entity
     * @throws org.paquitosoft.lml.model.exception.InternalErrorException
     */
    public T execute(Connection connection) throws InternalErrorException {
        
        // Get cascade attributes from entity
        List<Field> cascadeAttributes = ModelUtilities.getCascadeAttributes(entity.getClass());
        
        // Get default DAO
        IDefaultDAO dao = DAOFactory.getDefaultDAO(connection);
        T result = entity;
        
        if (PERSIST_MODE_SAVE == mode) {
            result = dao.insert(entity);
            persistAssociatedAttributes(cascadeAttributes, connection, entity, mode); // AFTER
        } else if (PERSIST_MODE_UPDATE == mode) {
            result = dao.update(entity);
            persistAssociatedAttributes(cascadeAttributes, connection, entity, mode); // AFTER
        } else if (PERSIST_MODE_DELETE == mode) {
            try {
                persistAssociatedAttributes(cascadeAttributes, connection, entity, mode); // BEFORE
                dao.remove(entity.getClass(), ModelUtilities.getEntityIdentifier(entity));
            } catch (ReflectionException ex) {
                throw new InternalErrorException("PersisAction::execute::removeEntity", ex);
            }
        } else {
            throw new InternalErrorException("PersistAction::execute -> Unknown mode operation: " + mode);
        }
        
        return result;
    }

    /**
     * This method is used to persist a collection of cascade attributes from an entity.
     * 
     * @param cascadeAttributes (attributes with cascade type OPTIONAL or REQUIRED)
     * @param connection
     * @param mode
     * @throws InternalErrorException (ReflectionException)
     */
    protected void persistAssociatedAttributes(List<Field> cascadeAttributes, Connection connection, Object entity, byte mode) 
            throws InternalErrorException {

        try {
            for (Field f : cascadeAttributes) {
                
                f.setAccessible(true);
                Object fieldValue = f.get(entity);
                AssociationType at = ModelUtilities.getAssociationType(f, mode);
                
                if (f.getAnnotation(PersistentAttribute.class) != null){
                    try {
                        if (fieldValue == null && AssociationType.REQUIRED.equals(at)) {
                            throw new InternalErrorException("PersistAction::persistAssociatedAttributes -> You tried to persist a null value for " +
                                    "a required associated attribute: " + f.getName());
                        } else if (fieldValue == null) {
                            continue;
                        } else if (fieldValue != null && !AssociationType.NONE.equals(at)) {
                            f.set(entity, new PersistAction(fieldValue, mode).execute(connection));
                        }                        
                    } catch (DuplicateInstanceException e) {
                        if (AssociationType.REQUIRED.equals(at)) {
                            logger.log(Level.WARNING, "PersistAction::execute::persistAssociatedAttributes -> You tried to persist (" + mode + ") an " +
                                "associated entity that it's already in the database: " + f.get(entity).toString());
                            throw e;
                        }
                    } catch (DataNotFoundException e) {
                        if (AssociationType.REQUIRED.equals(at)) {
                            logger.log(Level.WARNING, "PersistAction::execute::persistAssociatedAttributes -> You tried to persist (" + mode + ") an " +
                                "associated entity that it's not in the database: " + f.get(entity).toString());
                            throw e;
                        }
                    }
                } else if (f.getAnnotation(AssociatedEntityList.class) != null) {                                        
                    if (fieldValue == null && AssociationType.REQUIRED.equals(at)) {
                        throw new InternalErrorException("PersistAction::persistAssociatedAttributes -> You tried to persist a null value for " +
                                    "a required associated entity list: " + f.getName());
                    } else if (fieldValue == null) {
                        continue;
                    }
                    ArrayList<?> associatedEntityList = (ArrayList<?>) fieldValue;
                    ArrayList<Object> persistedEntities = (ArrayList<Object>) associatedEntityList.clone();
                    persistedEntities.clear();
                    for (Object rEntity : associatedEntityList) {
                        try {
                            if (rEntity != null && !AssociationType.NONE.equals(at)) {
                                // TODO Asegurarse de que las entidades asociadas tienen establecido el identificador de la actual
                                Object entityPk = ModelUtilities.getEntityIdentifier(entity);
                                String columnName = f.getAnnotation(AssociatedEntityList.class).externalKey();
                                checkRelatedAttributeInAssociatedEntity(columnName, rEntity, entityPk);
                                
                                persistedEntities.add(new PersistAction(rEntity, mode).execute(connection));
                            } else {
                                persistedEntities.add(rEntity);
                            }
                        } catch (DuplicateInstanceException e) {                            
                            if (AssociationType.REQUIRED.equals(at)) {
                                logger.log(Level.WARNING, "PersistAction::execute::persistAssociatedAttributes -> You tried to persist (" + mode + ") a " +
                                "related entity that it's already in the database: " + rEntity.toString());
                                throw e;
                            }
                        } catch (DataNotFoundException e) {
                            if (AssociationType.REQUIRED.equals(at)) {
                                logger.log(Level.WARNING, "PersistAction::execute::persistAssociatedAttributes -> You tried to persist (" + mode + ") an " +
                                "associated entity that it's not in the database: " + f.get(entity).toString());
                                throw e;
                            }
                        }
                    }
                    f.set(entity, persistedEntities);
                }
            }
        } catch (IllegalAccessException e) {
            throw new ReflectionException("PersistAction::execute::persistAssociatedAttributes", e);
        } catch (IllegalArgumentException e) {
            throw new ReflectionException("PersistAction::execute::persistAssociatedAttributes", e);
        }
        
    }

    /**
     * This method is used to ensure that an associated entity has the identifier from the main entity.
     * 
     * @param columnName
     * @param relatedEntity
     * @param value
     * @throws ReflectionException
     */
    private void checkRelatedAttributeInAssociatedEntity(String columnName, Object relatedEntity, Object value) throws ReflectionException {
        
        try {

            // We first need to find the attribute in the related entity representing the main entity in this action
            List<Field> fields = ModelUtilities.getAllPersistentEntityFields(relatedEntity.getClass());
            for (Field f : fields) {
                PersistentAttribute a = f.getAnnotation(PersistentAttribute.class);
                if (a != null && columnName.equalsIgnoreCase(a.columnName())) {
                    f.setAccessible(true);

                    if (f.getClass().getAnnotation(PersistentEntity.class) != null) {
                        List<Field> pkFields = ModelUtilities.getEntityIdentifierFields(f.getClass());
                        if (pkFields.size() == 1) {
                                Field pk = pkFields.get(0);
                                pk.setAccessible(true);
                                pk.set(relatedEntity, value);
                            }
                    } else {
                        f.set(relatedEntity, value);
                    }
                    break;
                }
            }

        } catch (IllegalAccessException e) {
            throw new ReflectionException("Error while looking for an attribute by its column name.", e);
        }
        
    }

}
