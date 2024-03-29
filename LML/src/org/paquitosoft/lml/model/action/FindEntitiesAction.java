package org.paquitosoft.lml.model.action;

import java.sql.Connection;
import java.util.List;
import org.paquitosoft.lml.model.dao.DAOFactory;
import org.paquitosoft.lml.model.dao.IDefaultDAO;
import org.paquitosoft.lml.model.exception.InternalErrorException;
import org.paquitosoft.lml.model.util.ModelUtilities;

/**
 *  This action can be used to find a collection of entities using a 
 *  custom query.
 * 
 * @author paquitosoft
 */
public class FindEntitiesAction<T> implements INonTransactionalAction {

    private String query;
    
    private Class<T> entityType;
    
    private Integer detailLevel;
    
    private Object[] params;
    
    public FindEntitiesAction(String query, Class<T> entityType, Integer detailLevel, Object ... params) {
        this.query = query.toUpperCase();
        this.entityType = entityType;
        this.detailLevel = detailLevel;
        this.params = params;
    }
    
    public T execute(Connection connection) throws InternalErrorException {
        
        // Create an instance of default DAO
        IDefaultDAO dao = DAOFactory.getDefaultDAO(connection);
        
        // Execute query
        List<T> result = dao.finder(entityType, query, params);
     
        // Count selected fields        
        String[] selectFields = query.substring(query.indexOf("SELECT") + 7, query.indexOf("FROM") - 1).split(",");
        int entityFieldsCount = ModelUtilities.getAllPersistentEntityFields(entityType).size();
        
        // We only fill the result when query selected all entity fields because we need all FKs to be sure we can fill it
        if (selectFields.length == entityFieldsCount || 
                (selectFields.length == 1 && entityFieldsCount != selectFields.length && selectFields[0].equals("*"))) { // Trick to 'SELECT *' queries
            for (T entity : result) {
                entity = (T) new ReadEntityAction(entity, detailLevel).execute(connection);
            }
        }        
        
        return (T) result;
    }

}
