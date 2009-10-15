package org.paquitosoft.lml.model.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  This annotation is used to tell the library that an attribute from an entity 
 *  is persistent. It also says the kind of attrinbute it is.
 * 
 * @author paquitosoft
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PersistentAttribute {

    /**
     * The name of the column of this attribute is the table.
     * (default: empty String)
     */
    String columnName() default "";
    
    /**
     * Whether this attribute reflects a table primary key or not 
     * (default <b>false</b>)
     */
    boolean primaryKey() default false;
    
    /**
     * Whether  this attribute is an entity entity or not 
     * (default <b>false</b>)
     */
    boolean entity() default false;
    
    /**
     * When this attribute is an associated entity, this parameter tells 
     * the library if this field is required when reading current entity.
     * (AssociationType.OPTIONAL | AssociationType.REQUIRED)
     * <br/>
     * (default: <b>AssociationType.OPTIONAL</b>)
     */
    AssociationType readAssociationType() default AssociationType.OPTIONAL;
    
    /**
     * When this attribute is an associated entity, this parameter tells 
     * the library if this field is required when saving current entity.
     * <ul>
     *  <li>
     *      AssociationType.NONE: library won't do anything with this associated entity
     *  </li>
     * <li>
     *      AssociationType.OPTIONAL: library will try to save this associated entity AFTER saving 
     *      current one, but it won't complaint if associated entity saving fails.
     *  </li>
     * <li>
     *      AssociationType.REQUIRED: library will try to save this associated entity AFTER saving 
     *      current one and it will throw an exception if there is any error.
     *  </li>
     * </ul>
     * <br/>
     * (default: <b>AssociationType.NONE</b>)
     */
    AssociationType saveAssociationType() default AssociationType.NONE;
    
    /**
     * When this attribute is an associated entity, this parameter tells 
     * the library if this field is required when updating current entity.
     * <ul>
     *  <li>
     *      AssociationType.NONE: library won't do anything with this associated entity
     *  </li>
     * <li>
     *      AssociationType.OPTIONAL: library will try to update this associated entity AFTER updating 
     *      current one, but it won't complaint if associated entity updating fails.
     *  </li>
     * <li>
     *      AssociationType.REQUIRED: library will try to update this associated entity AFTER updating 
     *      current one and it will throw an exception if there is any error.
     *  </li>
     * </ul>
     * <br/>
     * (default: <b>AssociationType.OPTIONAL</b>)
     */
    AssociationType updateAssociationType() default AssociationType.OPTIONAL;
    
    /**
     * When this attribute is an associated entity, this parameter tells 
     * the library if this field is required when removing current entity.
     * <ul>
     *  <li>
     *      AssociationType.NONE: library won't do anything with this associated entity
     *  </li>
     * <li>
     *      AssociationType.OPTIONAL: library will try to remove this associated entity BEFORE removing 
     *      current one, but it won't complaint if associated entity removing fails.
     *  </li>
     * <li>
     *      AssociationType.REQUIRED: library will try to remove this associated entity BEFORE removing 
     *      current one and it will throw an exception if there is any error.
     *  </li>
     * </ul>
     * <br/>
     * (default: <b>AssociationType.NONE</b>)
     */
    AssociationType removeAssociationType() default AssociationType.NONE;
    
}
