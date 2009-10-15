package org.paquitosoft.lml.model.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.paquitosoft.lml.model.annotation.AssociationType;
import org.paquitosoft.lml.model.annotation.PersistentEntity;
import org.paquitosoft.lml.model.annotation.PersistentAttribute;
import org.paquitosoft.lml.model.annotation.AssociatedEntityList;
import org.paquitosoft.lml.model.exception.ReflectionException;
import org.paquitosoft.lml.util.LMLGlobalOperations;
import static org.paquitosoft.lml.util.LMLConstants.*;

/**
 *
 * @author paquitosoft
 */
public class ModelUtilities {

    private static Logger logger = Logger.getLogger(ModelUtilities.class.getName());
    
    private static HashMap<Class,String> insertQueries = new HashMap<Class,String>();
    private static HashMap<Class,String> readQueries = new HashMap<Class,String>();
    private static HashMap<Class,String> updateQueries = new HashMap<Class,String>();
    private static HashMap<Class,String> deleteQueries = new HashMap<Class,String>();
    private static HashMap<Class,String> baseReadQueries = new HashMap<Class,String>();
    private static HashMap<String,String> joinFinderQueries = new HashMap<String,String>();

    /**
     * Generates the query for creating and entity
     * @param entityClass
     * @return insert query
     */
    public static String getQueryForInsert(Class entityClass) {
        
        String result = insertQueries.get(entityClass);
        
        if (result == null) {
            boolean skipPK = isEntityPKGeneratedByDatabase(entityClass);
            boolean pkSkipped = false;
            StringBuilder sb = new StringBuilder("INSERT INTO ");
            sb.append(getTableName(entityClass));
            sb.append(" (");
            List<Field> fields = getAllPersistentEntityFields(entityClass);
            for (Field f : fields) {
                if (skipPK && isPKField(f)) {
                    pkSkipped = true;                    
                } else {
                    sb.append(getColumnName(f));
                    sb.append(',');
                }
            }
            sb.delete(sb.length() - 1, sb.length());
            sb.append(") VALUES (");
            for (int i = 0; i < (fields.size() - (pkSkipped ? 1 : 0)); i++) {
                sb.append("?,");
            }
            sb.delete(sb.length() - 1, sb.length());
            sb.append(")");
            result = sb.toString();
            insertQueries.put(entityClass, result);
            logger.log(Level.INFO, "INSERT query generated ->" + sb.toString() + "<-");
        }
        
        return result;
    }

    /**
     * Generates the query for delete an entity
     * @param entityClass
     * @return delete query
     */
    public static String getQueryForDelete(Class entityClass) {
        
        String result = deleteQueries.get(entityClass);
        
        if (result == null) {
            StringBuilder sb = new StringBuilder("DELETE FROM ");
            sb.append(getTableName(entityClass));
            sb.append(getWhereClause(entityClass));        
            result = sb.toString();
            deleteQueries.put(entityClass, result);
            logger.log(Level.INFO, "DELETE query generated ->" + sb.toString() + "<-");
        }
        
        return result;
    }

    /**
     * Generates the query for update an entity
     * @param entityClass
     * @return update query
     */
    public static String getQueryForUpdate(Class entityClass) {
        
        String result = updateQueries.get(entityClass);
        
        if (result == null) {
            StringBuilder sb = new StringBuilder("UPDATE ");
            sb.append(getTableName(entityClass));
            sb.append(" SET ");
            List<Field> fields = getAllPersistentEntityFields(entityClass);
            for (Field f : fields) {
                if (!isPKField(f)) {
                    sb.append(getColumnName(f));
                    sb.append("=?, ");
                }                
            }
            sb.delete(sb.length() - 2, sb.length());
            sb.append(getWhereClause(entityClass));        
            result = sb.toString();
            updateQueries.put(entityClass, result);
            logger.log(Level.INFO, "UPDATE query generated ->" + sb.toString() + "<-");
        }
        
        return result;
    }
    
    /**
     * Generates the base query for consulting the table of the entity provided.
     * 
     * @param entityClass
     * @return base read query
     */
    public static String getQueryBaseForRead(Class entityClass) {

        String result = baseReadQueries.get(entityClass);

        if (result == null) {
            StringBuilder sb = new StringBuilder("SELECT ");
            ArrayList<Field> fields = getAllPersistentEntityFields(entityClass);
            for (Field f : fields) {
                sb.append(getColumnName(f));
                sb.append(", ");
            }
            sb.delete(sb.length() - 2, sb.length());
            sb.append(" FROM ");
            sb.append(getTableName(entityClass));
            result = sb.toString();
            baseReadQueries.put(entityClass, result);
            logger.log(Level.INFO, "Base FIND query generated ->" + sb.toString() + "<-");
        }

        return result;

    }

    /**
     * Generate the query for read the an entity using its identifiers.
     *
     * @param entityClass
     * @return read query
     */
    public static String getQueryForRead(Class entityClass) {
        
        String result = readQueries.get(entityClass);
        
        if (result == null) {
            result = getQueryBaseForRead(entityClass) + getWhereClause(entityClass);
            readQueries.put(entityClass, result);
            logger.log(Level.INFO, "FIND query generated ->" + result + "<-");
        }
        
        return result;
    }
    
    /**
     * Generates the query to get the entity associated list from another entity 
     * when tehy association type is N-M.
     * 
     * @param sourceEntityClass
     * @param destinationEntityClass
     * @param joinTableName
     * @return join finder query
     */
    public static String getJoinFinderQuery(Class sourceEntityClass,
            Class destinationEntityClass, String joinTableName) {

        String queryId = sourceEntityClass.getName() + "->" +
                destinationEntityClass.getName();
        String result = joinFinderQueries.get(queryId);

        if (result == null) {

            StringBuilder sb = new StringBuilder(getQueryBaseForRead(destinationEntityClass));
            sb.append(" WHERE ");

            List<String> sourcePkColumnNames = getEntityIdentifiersColumnNames(sourceEntityClass);
            List<String> destinationPkColumnNames = getEntityIdentifiersColumnNames(destinationEntityClass);
            for (int i = 0; i < sourcePkColumnNames.size(); i++) {
                sb.append(destinationPkColumnNames.get(i));
                sb.append(" IN (SELECT ");
                sb.append(destinationPkColumnNames.get(i));
                sb.append(" FROM ");
                sb.append(joinTableName);
                sb.append(" WHERE ");
                sb.append(sourcePkColumnNames.get(i));
                sb.append(" = ?)");
            }

            result = sb.toString();
            joinFinderQueries.put(queryId, result);
            logger.log(Level.INFO, "FIND query generated ->" + result + "<-");
        }

        return result;
    }

    /**
     * Inserts a value into a preparedStatement.
     * 
     * @param value
     * @param stm
     * @param index
     * @throws java.sql.SQLException
     */
    public static void insertValueInQuery(Object value, PreparedStatement stm, int index) throws SQLException {
        
        if (value == null) {
            stm.setString(index, null);
        } else if (String.class.equals(value.getClass())) {
            stm.setString(index, (String) value);
        } else if (Integer.class.equals(value.getClass())) {
            if (value != null) {
                stm.setInt(index, (Integer) value);
            } else {
                stm.setObject(index, null);
            }            
        } else if (Long.class.equals(value.getClass())) {
            if (value != null) {
                stm.setLong(index, (Long) value);
            } else {
                stm.setObject(index, null);
            }
        } else if (Double.class.equals(value.getClass())) {
            if (value != null) {
                stm.setDouble(index, (Double) value);
            } else {
                stm.setObject(index, null);
            }
        } else if (Float.class.equals(value.getClass())) {
            if (value != null) {
                stm.setFloat(index, (Float) value);
            } else {
                stm.setObject(index, null);
            }
        } else if (value instanceof Calendar) {
            if (value != null) {
                stm.setTimestamp(index, new Timestamp(((Calendar) value).getTimeInMillis()));
            } else {
                stm.setObject(index, null);
            }            
        } else if (value instanceof Date) {
            if (value != null) {
                stm.setTimestamp(index, new Timestamp(((Date) value).getTime()));
            } else {
                stm.setObject(index, null);
            }
        } else if (Boolean.class.equals(value.getClass())) {
            if (value != null) {
                stm.setBoolean(index, (Boolean) value);
            } else {
                stm.setObject(index, null);
            }
        } else if (Byte.class.equals(value.getClass())) {
            if (value != null) {
                stm.setByte(index, (Byte) value);
            } else {
                stm.setObject(index, null);
            }
        } else if (Short.class.equals(value.getClass())) {
            if (value != null) {
                stm.setShort(index, (Short) value);
            } else {
                stm.setObject(index, null);
            }
        } else if (Character.class.equals(value.getClass())) {
            if (value != null) {
                stm.setString(index, ((Character) value).toString());
            } else {
                stm.setObject(index, null);
            }
        } else if (BigDecimal.class.equals(value.getClass())) {
            if (value != null) {
                stm.setBigDecimal(index, (BigDecimal) value);
            } else {
                stm.setObject(index, null);
            }
        } else {
            stm.setObject(index, value);
        }
        
    }

    /**
     * Gets the value from the resultSet defined by its columName and valueType.
     * 
     * @param valueType
     * @param rs
     * @param columnName
     * @return
     * @throws java.sql.SQLException
     */
    public static <T> T getValueFromQuery(Class<T> valueType, ResultSet rs, String columnName) throws SQLException {
        
        T result = null;
        
        if (String.class.equals(valueType)) {
            result = (T) rs.getString(columnName);
        } else if (Integer.class.equals(valueType)) {
            Integer aux = rs.getInt(columnName);
            result = (rs.wasNull()) ? null : (T) aux;
        } else if (Long.class.equals(valueType)) {
            Long aux = rs.getLong(columnName);
            result = (rs.wasNull()) ? null : (T) aux;
        } else if (Double.class.equals(valueType)) {
            Double aux = rs.getDouble(columnName);
            result = (rs.wasNull()) ? null : (T) aux;
        } else if (Float.class.equals(valueType)) {
            Float aux = rs.getFloat(columnName);
            result = (rs.wasNull()) ? null : (T) aux;
        } else if (Calendar.class.equals(valueType)) {	
            Calendar aux1 = null;
            Timestamp aux2 = rs.getTimestamp(columnName);
            if (aux2 != null) {
                    Date time = new Date(aux2.getTime());
                    aux1 = Calendar.getInstance();
                    aux1.setTime(time);
                    result = (T) aux1;
            }            
        } else if (Date.class.equals(valueType)) {
            Timestamp aux = rs.getTimestamp(columnName);
            if (aux != null) {
                    Date time = new Date(aux.getTime());                    
                    result = (T) time;
            }
        } else if (Boolean.class.equals(valueType)) {
            Boolean aux = rs.getBoolean(columnName);
            result = (rs.wasNull()) ? null : (T) aux;
        } else if (Byte.class.equals(valueType)) {
            Byte aux = rs.getByte(columnName);
            result = (rs.wasNull()) ? null : (T) aux;
        } else if (Short.class.equals(valueType)) {
            Short aux = rs.getShort(columnName);
            result = (rs.wasNull()) ? null : (T) aux;
        } else if (char.class.equals(valueType) || Character.class.equals(valueType)) {
            String aux = rs.getString(columnName);
            result = (aux != null) ? (T) new Character(aux.charAt(0)) : null;
        } else {            
            result = (T) rs.getObject(columnName);
        }
        
        return result;
    }
        
    /**
     * Gets the value from the resultSet defined by its columName and valueType.
     * 
     * @param valueType
     * @param rs
     * @param columnIndex
     * @return
     * @throws java.sql.SQLException
     */
    public static <T> T getValueFromQuery(Class<T> valueType, ResultSet rs, int columnIndex) throws SQLException {
        
        T result = null;
        
        if (String.class.equals(valueType)) {
            result = (T) rs.getString(columnIndex);
        } else if (Integer.class.equals(valueType)) {
            Integer aux = rs.getInt(columnIndex);
            result = (rs.wasNull()) ? null : (T) aux;
        } else if (Long.class.equals(valueType)) {
            Long aux = rs.getLong(columnIndex);
            result = (rs.wasNull()) ? null : (T) aux;
        } else if (Double.class.equals(valueType)) {
            Double aux = rs.getDouble(columnIndex);
            result = (rs.wasNull()) ? null : (T) aux;
        } else if (Float.class.equals(valueType)) {
            Float aux = rs.getFloat(columnIndex);
            result = (rs.wasNull()) ? null : (T) aux;
        } else if (Calendar.class.equals(valueType)) {	
            Calendar aux1 = null;
            Timestamp aux2 = rs.getTimestamp(columnIndex);
            if (aux2 != null) {
                    Date time = new Date(aux2.getTime());
                    aux1 = Calendar.getInstance();
                    aux1.setTime(time);
                    result = (T) aux1;
            }            
        } else if (Date.class.equals(valueType)) {
            Timestamp aux = rs.getTimestamp(columnIndex);
            if (aux != null) {
                    Date time = new Date(aux.getTime());                    
                    result = (T) time;
            }
        } else if (Boolean.class.equals(valueType)) {
            Boolean aux = rs.getBoolean(columnIndex);
            result = (rs.wasNull()) ? null : (T) aux;
        } else if (Byte.class.equals(valueType)) {
            Boolean aux = rs.getBoolean(columnIndex);
            result = (rs.wasNull()) ? null : (T) aux;
        } else if (Short.class.equals(valueType)) {
            Short aux = rs.getShort(columnIndex);
            result = (rs.wasNull()) ? null : (T) aux;
        } else if (char.class.equals(valueType) || Character.class.equals(valueType)) {
            String aux = rs.getString(columnIndex);
            result = (aux != null) ? (T) new Character(aux.charAt(0)) : null;
        } else {            
            result = (T) rs.getObject(columnIndex);
        }
        
        return result;
    }

    /**
     * Populates an entity from a resultSet. <i>partialEntity</i> indicates whether the resultSet bring only 
     * some of the entity attributes.
     * @param entityType
     * @param rs
     * @param partialEntity <b>true</b> when we executed a query that doesn't bring all entity fields
     * @return
     * @throws java.lang.Exception
     */
    public static <T> T createEntityWithValues(Class<T> entityType, ResultSet rs, boolean partialEntity) throws ReflectionException {
        
        T result = null;
        
        try {
            
            result = (T) entityType.newInstance();

            ArrayList<Field> fields = getAllPersistentEntityFields(entityType);

            for (Field f : fields) {
                f.setAccessible(true); // VERY IMPORTANT!!!
                PersistentAttribute annot = f.getAnnotation(PersistentAttribute.class);
                // If this field is not annotated we must ignore it
                if (annot == null) {
                    continue;
                }
                String columnName = getColumnName(f);
                boolean readField = true;
                if (annot.entity()) {

                    // If it is a custom type we need to create a instance an populate its identifier
                    List<Field> pkFields = getEntityIdentifierFields(f.getType());                
                    if (pkFields.size() == 1) {

                        Field pkField = pkFields.get(0);
                        pkField.setAccessible(true); // VERY IMPORTANT!!!
                        Object associated = f.getType().newInstance();
//                        PersistentAttribute at = f.getAnnotation(PersistentAttribute.class);
                        try {
                            // This is done because of finder queries where we don't want all entity fields
                            rs.findColumn(columnName);
                        } catch (SQLException e) {
                            readField = false;
                            if (!partialEntity) {
                                throw e;
                            }
                        }      
                        if (readField) {
                            pkField.set(associated, getValueFromQuery(pkField.getType(), rs, columnName));
                        }
                        f.set(result, associated);

                    } else {
                        // TODO Crear una excepcion especifica para darle significado a este suceso
                        throw new ReflectionException("ERROR: Compound key for associated entity.");
                    }

                } else {
                    // If it is a common type we use the column name from the annotation or 
                    // the name of the field if that has not been set
                    try {
                        // This is done because of finder queries where we don't want all entity fields
                        rs.findColumn(columnName);                        
                    } catch (SQLException e) {
                        readField = false;
                        if (!partialEntity) {
                            throw e;
                        }
                    }
                    if (readField) {
                        f.set(result, getValueFromQuery(f.getType(), rs, columnName));
                    }
                }
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Method: getFieldValue()", e);
            throw new ReflectionException(e);
        }
        
        return result;
    }
    
    /**
     * Devuelve todos los atributos marcados como persistentes de las clase, 
     * buscando de forma recursiva hacia arriba.
     * @param entityClass
     * @return peristent fields or empty list
     */
    public static ArrayList<Field> getAllPersistentEntityFields(Class entityClass) {
        
        ArrayList<Field> result = new ArrayList<Field>();
        
        Field[] ownFields = entityClass.getDeclaredFields();
        for (int i = 0; i < ownFields.length; i++) {            
            if (ownFields[i].getAnnotation(PersistentAttribute.class) != null) {
                result.add(ownFields[i]);
            }
        }        
        
        Class superClass = entityClass.getSuperclass();
        if (superClass != null) {            
            result.addAll(getAllPersistentEntityFields(superClass));
        }
        
        // Sort fileds by name
        Collections.sort(result, new FieldNameComparator());
        
        return result;
    }

    /**
     * This method returns all primary keys attributes from this entity, 
     * sorted by name.
     * 
     * @param entityClass
     * @return primary key attributes
     * @throws java.lang.Exception
     */
    public static List<Field> getEntityIdentifierFields(Class entityClass) {
    
        List<Field> result = new ArrayList<Field>();
        
        Field[] fs = entityClass.getDeclaredFields();
        for (int i = 0; i < fs.length; i++) {
            PersistentAttribute fieldAT = fs[i].getAnnotation(PersistentAttribute.class);            
            if (fieldAT != null && fieldAT.primaryKey()) {
                result.add(fs[i]);
            }
        }
        
        // Sort fileds by name
        Collections.sort(result, new FieldNameComparator());
        
        return result;
    }
    
    /**
     * Este metodo me devuelve el nombre de la columna en la tabla correspondiente 
     * al atributo de entidad que le proporcionamos. En caso de no tenerlo establecido, 
     * devuelve el nombre del atributo.
     * @param persistentField
     * @return column name for the field provided
     */
    public static String getColumnName(Field f) {
        
        String result = f.getName();        
        PersistentAttribute att = f.getAnnotation(PersistentAttribute.class);            
        if (att != null) {
            result = (att.columnName().length() > 0) ? att.columnName().toUpperCase(): f.getName().toUpperCase();
        }            
        return result;
    }

    /**
     * This method onbtains the name of the PK column names from the table 
     * of the provided entity.
     * 
     * @param entityClass
     * @return primary key column names
     */
    public static List<String> getEntityIdentifiersColumnNames(Class entityClass) {
        List<Field> idFields = getEntityIdentifierFields(entityClass);
        List<String> result = new ArrayList<String>(idFields.size());
        for (Field f : idFields) {
            result.add(getColumnName(f));
        }
        return result;
    }

    /**
     * This methods obtains the table name for a given entity.
     *
     * @param entityClass
     * @return table name for the entity provided
     */
    public static String getTableName(Class entityClass) {
        
        String result = entityClass.getSimpleName();
        
        PersistentEntity att = (PersistentEntity) entityClass.getAnnotation(PersistentEntity.class);
        if (att != null && att.tableName().length() > 0) {
            result = att.tableName();
        }
        
        return result.toUpperCase();
    }

    /**
     * Metodo utilizado para construir la clausula WHERE basica de una consulta 
     * con las claves primarias en la entidad proporcionada
     * @param entityClass
     * @return
     * @throws java.lang.Exception
     */
    protected static String getWhereClause(Class entityClass) {
        
        StringBuilder sb = new StringBuilder();
        
        sb.append(" WHERE ");
        List<Field> fields = getEntityIdentifierFields(entityClass);
        for (Field f : fields) {
            sb.append(getColumnName(f));
            sb.append("=? AND ");
        }
        int i = sb.lastIndexOf(" AND ");
        if (i != -1) {
            sb.delete(i, sb.length());
        }
        
        return sb.toString();
    }

    /**
     * Este metodo se utiliza para obtener todos los metodos accesores de una clase (getters y setters) de 
     * forma recursiva. 
     * accessorPrefix debe ser <b>get</b> o <b>set</b>.
     * @param entityClass
     * @param accessorPrefix debe ser <b>get</b> o <b>set</b>
     * @return
     */
    public static List<Method> getAllPersistentEntityAccessors(Class entityClass, String accessorPrefix) throws ReflectionException {
        
        List<Method> result = new ArrayList<Method>();

        // Ger class methods
        Method[] ownMethods = entityClass.getDeclaredMethods();
        for (int i = 0; i < ownMethods.length; i++) {
            StringBuilder mName = new StringBuilder(ownMethods[i].getName());
            // Pick olny the getter/setter methods
            if (mName.toString().startsWith(accessorPrefix)) {
                mName.delete(0, 3);
                mName.replace(0, 1, String.valueOf(mName.charAt(0)).toUpperCase());
                // Select only those from the annotated fields
                try {
                    if (entityClass.getField(mName.toString()).isAnnotationPresent(PersistentAttribute.class)) {
                        result.add(ownMethods[i]);
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Method: getAllPersistentEntityAccessors()", e);
                    throw new ReflectionException(e);
                }
                                                
            }
        }        
        
        // This is to work recursovely upwards
        Class superClass = entityClass.getSuperclass();
        if (superClass != null) {            
            result.addAll(getAllPersistentEntityAccessors(entityClass, accessorPrefix));
        }
        
        // Sort fileds by name
        Collections.sort(result, new FieldNameComparator());
        
        return result;
    }

    /**
     * Devuelve el valor del atributo indicado del objeto proporcionado.
     * @param fieldName
     * @param entity
     * @return
     * @throws java.lang.Exception
     */
    public static Object getFieldValue(String fieldName, Object entity) throws ReflectionException {
        
        Object result = null;
        
        for (Field f : getAllPersistentEntityFields(entity.getClass())) {
            PersistentAttribute antt = f.getAnnotation(PersistentAttribute.class);            
            if ((antt.columnName().length() > 0 && fieldName.equalsIgnoreCase(antt.columnName())) ||
                    fieldName.equalsIgnoreCase(f.getName())){
                try {
                    f.setAccessible(true);
                    if (antt.entity()) {
                        result = getEntityIdentifier(f.get(entity));  // TODO Does it worth it to check the identifier here?
                    } else {
                        result = f.get(entity);
                    }                    
                    break;
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Method: getFieldValue()", ex);
                    throw new ReflectionException(ex);
                } 
            }
        }
        
        return result;
    }
    
    /**
     * This method is used to get all the fields from an entity (recursively) that are 
     * annotated with a custom annotation.
     * 
     * @param entityClass
     * @param annotationType
     * @return list of fields annotated with <i>annotationType</i> (sorted by name)
     */
    public static ArrayList<Field> getAllAnnotatedEntityFields(Class entityClass, Class annotationType) {
        
        ArrayList<Field> result = new ArrayList<Field>();
        
        Field[] ownFields = entityClass.getDeclaredFields();
        for (int i = 0; i < ownFields.length; i++) {            
            if (ownFields[i].getAnnotation(annotationType) != null) {
                result.add(ownFields[i]);
            }
        }        
        
        Class superClass = entityClass.getSuperclass();
        if (superClass != null) {            
            result.addAll(getAllAnnotatedEntityFields(superClass, annotationType));
        }
        
        // Sort fileds by name
        Collections.sort(result, new FieldNameComparator());
        
        return result;
    }
    
    /**
     * This method is used to get all the entity entity attributes (recursively).
     * 
     * @param entityClass
     * @return list of associates attributes (sorted by name)
     */
    public static ArrayList<Field> getAllAssociatedEntityFields(Class entityClass) {
        
        ArrayList<Field> result = new ArrayList<Field>();
        
        Field[] ownFields = entityClass.getDeclaredFields();
        for (int i = 0; i < ownFields.length; i++) {            
            if (ownFields[i].getAnnotation(PersistentAttribute.class) != null && 
                    ownFields[i].getAnnotation(PersistentAttribute.class).entity()) {
                result.add(ownFields[i]);
            }
        }        
        
        Class superClass = entityClass.getSuperclass();
        if (superClass != null) {            
            result.addAll(getAllAssociatedEntityFields(superClass));
        }
        
        // Sort fileds by name
        Collections.sort(result, new FieldNameComparator());
        
        return result;
    }
    
    /**
     * This method is used to get the primary key attributes from 
     * this entity. 
     * If the primary key is a single value, this value is returned.
     * If the primary key is coumpound, the entity itself is returned.
     * If there is no primary key, a ReflectionException is raised.
     * 
     * @param entityClass
     * @return primary key
     */
    public static Object getEntityIdentifier(Object entity) throws ReflectionException {
    
        Object result = null;
        
        int pkCounter = 0;
        Field[] fs = entity.getClass().getDeclaredFields();
        for (int i = 0; i < fs.length; i++) {
            PersistentAttribute fieldAT = fs[i].getAnnotation(PersistentAttribute.class);            
            if (fieldAT != null && fieldAT.primaryKey()) {
                pkCounter++;
            }
        }
        
        if (pkCounter == 1) {
            try {
                fs[0].setAccessible(true); // TODO Esto no es correcto. Supone que los campos vienen ordenados y la documentacion dice que no es asi. Es necesario cambiar esto.
                result = fs[0].get(entity);
            } catch (IllegalAccessException e) {
                throw new ReflectionException(e);
            }
            
        } else if (pkCounter  > 1) {
            result = entity;
        } else {
            throw new ReflectionException("" + entity.getClass().getName() + " instance has no primary keys.");
        }
        
        return result;
    }
 
    /**
     * This method is used to create the query we must use to gather a collection of related 
     * entities to a specified entity.
     * 
     * @return
     */
    public static String generateFindRelatedEntitiesQuery(Field relatedAttribute) throws ReflectionException {
    
        StringBuilder sb = new StringBuilder("SELECT ");
        
        try {
            // Get the foreign key
            String[] fks = relatedAttribute.getAnnotation(AssociatedEntityList.class).externalKey().split(",");
            
            // Get the type of the collection
            Class collectionType = LMLGlobalOperations.getCollectionType(relatedAttribute);
            
            ArrayList<Field> fields = getAllPersistentEntityFields(collectionType);
            for (Field f : fields) {
                sb.append(getColumnName(f));
                sb.append(", ");
            }
            sb.delete(sb.length() - 2, sb.length());
            sb.append(" FROM ");
            sb.append(getTableName(collectionType));
            sb.append(" WHERE ");
            for (int i = 0; i < fks.length; i++) {
                sb.append(fks[i]);
                sb.append("=? AND ");
            }
            int j = sb.lastIndexOf(" AND ");
            if (j != -1) {
                sb.delete(j, sb.length());
            }

        } catch (Exception exception) {
            throw new ReflectionException("ModelUtilities::generateFindRelatedEntitiesQuery -> Most likely this is an error when " +
                    "we try to get the type of the collection.", exception);
        }
        
        return sb.toString();
    }

    /**
     * This method is used to get all the attributes that must be persisted when the 
     * entity does.
     * 
     * @param entityClass
     * @return list of cascade attributes
     */
    public static List<Field> getCascadeAttributes(Class entityClass) {
    
        List<Field> result = new ArrayList<Field>();
        
        for (Field f : getAllAnnotatedEntityFields(entityClass, PersistentAttribute.class)) {
            if (f.getAnnotation(PersistentAttribute.class).entity() && 
                    !AssociationType.NONE.equals(f.getAnnotation(PersistentAttribute.class).saveAssociationType()) && 
                    !AssociationType.NONE.equals(f.getAnnotation(PersistentAttribute.class).removeAssociationType())) {
                result.add(f);
            }
        }
        
        for (Field f : getAllAnnotatedEntityFields(entityClass, AssociatedEntityList.class)) {
            if (!AssociationType.NONE.equals(f.getAnnotation(AssociatedEntityList.class).saveAssociationType()) && 
                    !AssociationType.NONE.equals(f.getAnnotation(AssociatedEntityList.class).removeAssociationType())) {
                result.add(f);
            }
        }
        
        return result;
    }

    /**
     * This method is used to gather the class of the primary key from an entity.
     * 
     * @param entityType
     * @return primary key class or <b>null</b> if there is no primary key or it is <b>compound</b>
     */
    public static Class getEntityIdType(Class entityType) {
    
        Class result = null;
        
        int pkCounter = 0;
        Field[] fs = entityType.getDeclaredFields();
        for (int i = 0; i < fs.length; i++) {
            PersistentAttribute fieldAT = fs[i].getAnnotation(PersistentAttribute.class);            
            if (fieldAT != null && fieldAT.primaryKey()) {
                fs[0].setAccessible(true);
                result = fs[0].getType();
                if (++pkCounter > 1) {
                    return null;
                }
            }
        }
        
        return result;
    }

    /**
     * This method gets the associationType for the fiel depending on the persistence persistenceMode 
     * provided.<br/>
     * (default value is AssociationType.NONE)
     * 
     * @param persistentField
     * @param persistenceMode
     * @return association type (default AssociationType.NONE)
     */
    public static AssociationType getAssociationType(Field persistentField, byte persistenceMode) {
    
        AssociationType result = AssociationType.NONE;
        
        if (PERSIST_MODE_DELETE == persistenceMode) {
            PersistentAttribute att = persistentField.getAnnotation(PersistentAttribute.class);
            if (att != null) {
                result = att.removeAssociationType();
            } else {
                result = persistentField.getAnnotation(AssociatedEntityList.class).removeAssociationType();
            }
        } else if (PERSIST_MODE_SAVE == persistenceMode) {
            PersistentAttribute att = persistentField.getAnnotation(PersistentAttribute.class);
            if (att != null) {
                result = att.saveAssociationType();
            } else {
                result = persistentField.getAnnotation(AssociatedEntityList.class).saveAssociationType();
            }
        } else if (PERSIST_MODE_UPDATE == persistenceMode) {
            PersistentAttribute att = persistentField.getAnnotation(PersistentAttribute.class);
            if (att != null) {
                result = att.updateAssociationType();
            } else {
                result = persistentField.getAnnotation(AssociatedEntityList.class).updateAssociationType();
            }
        } 
                
        return result;
    }

    /**
     * This method checks for a class to see if it's an entity class with a database 
     * generated key.
     * 
     * @param entityClass
     * @return <b>true</b> when entity's primary key is generated by the database
     */
    public static boolean isEntityPKGeneratedByDatabase(Class entityClass) {
        boolean result = true;
        PersistentEntity pr = (PersistentEntity) entityClass.getAnnotation(PersistentEntity.class);
        if (pr != null && pr.generateKey()) {
            result = false;
        }
        return result;
    }
 
    /**
     * This method is used to check wether provided field represents 
     * a primary key.
     * 
     * @param f
     * @return <b>true</b> when it represents a primary key
     */
    public static boolean isPKField(Field f) {
        boolean result = false;
        if (f != null) {
            PersistentAttribute pa = f.getAnnotation(PersistentAttribute.class);
            if (pa != null && pa.primaryKey()) {
                result = true;
            }
        }        
        return result;
    }
    
}
