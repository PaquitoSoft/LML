package org.paquitosoft.lml.util;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  Auxiliar methods for the library.
 * 
 * @author paquitosoft
 */
public class LMLGlobalOperations {

    private static Logger logger = Logger.getLogger(LMLGlobalOperations.class.getName());
    
    /**
     * This method is used to gather a value from the connection settings file.
     * 
     * @param key
     * @return the value we'return looking for or an empty string if it could not be obtained.
     */
    public static String getConnectionSetting(String key) {
        
        String result = "";        
        
        try {
            ResourceBundle rb = ResourceBundle.getBundle(LMLConstants.CONNECTION_SETTINGS_FILE_NAME);
            result = rb.getString(key);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not load setting value: " + key);
        }        
        
        return result;
    }

    /**
     * This method is used to get the type of a typed collection.
     * 
     * @param collectionAttribute
     * @return collection type
     */
    public static Class getCollectionType(Field collectionAttribute) {
        return (Class) ((ParameterizedType) collectionAttribute.getGenericType()).getActualTypeArguments()[0];
    }
    
}
