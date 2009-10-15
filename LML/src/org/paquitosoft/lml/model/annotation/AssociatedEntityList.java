package org.paquitosoft.lml.model.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  This annotation indicates that an entity attribute is an entity that has a 
 *  foreign key to the main entity. It's commonly used to tag collections very 
 *  linked to the entity.
 *  
 * @author paquitosoft
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AssociatedEntityList {

    /**
     * Indicates the foreign key in the related entity table for 
     * the current entity.
     * <p>
     * List of strings separated by commas. (must be alphabetically sorted)
     * <p>
     * (default: emptyString)
     */
    String externalKey() default "";
 
    /**
     * When you're dealing with a n-m relationship you'll need to set the 
     * name of the table used to join both entities. 
     * <p>
     * It's expected the name of the column set in the <b>externalKey</b> 
     * attribute is the same in the associated entity table and the join table.
     * <p>
     * (default: empty String)
     * return join table name
     */
    String joinTableName() default "";

   /**
     * When this attribute is an associated entity, this parameter tells 
     * the library if this field is required when reading current entity.
     * (AssociationType.OPTIONAL | AssociationType.REQUIRED)
     * <br/>
     * (default: AssociationType.OPTIONAL)
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
     * (default: AssociationType.NONE)
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
     * (default: AssociationType.NONE)
     */
    AssociationType removeAssociationType() default AssociationType.NONE;
    
}
