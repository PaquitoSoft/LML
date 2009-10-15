package org.paquitosoft.lml.model.action;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.List;
import org.paquitosoft.lml.model.dao.DAOFactory;
import org.paquitosoft.lml.model.dao.IDefaultDAO;
import org.paquitosoft.lml.model.exception.InternalErrorException;
import org.paquitosoft.lml.util.LMLGlobalOperations;

/**
 *
 * @author PaquitoSoft
 */
public class FindExternalRelatedEntitiesAction<T> implements INonTransactionalAction {

    private Object entity;
    private Field externalAssociatedAttribute;
    private String joinTableName;
    private Integer detailLevel;

    /**
     * Public constructor.
     * 
     * @param entity
     * @param externalAssociatedAttribute
     * @param joinTableName
     * @param detailLevel
     */
    public FindExternalRelatedEntitiesAction(Object entity, Field externalAssociatedAttribute,
            String joinTableName, Integer detailLevel) {
        this.entity = entity;
        this.externalAssociatedAttribute = externalAssociatedAttribute;
        this.joinTableName = joinTableName;
        this.detailLevel = detailLevel;        
    }

    /**
     * This method executes this action.
     *
     * @param connection
     * @return
     * @throws InternalErrorException
     */
    public T execute(Connection connection) throws InternalErrorException {

        // Create an instance of default DAO
        IDefaultDAO dao = DAOFactory.getDefaultDAO(connection);

        // Check join table name
        if (joinTableName == null) {
            throw new InternalErrorException("FindExternalRelatedEntitiesAction::execute -> The attribute you're trying to fetch '" +
                    externalAssociatedAttribute.getName() + "' does not have a 'joinTableName' annotation value.");
        }

        // Get the list type
        Class listType = LMLGlobalOperations.getCollectionType(externalAssociatedAttribute);

        // Execute query
        List<T> result = dao.findJoinEntities(entity, listType, joinTableName);

        for (T ent : result) {
            ent = (T) new ReadEntityAction(ent, detailLevel).execute(connection);
        }
        
        return (T) result;
    }

}
