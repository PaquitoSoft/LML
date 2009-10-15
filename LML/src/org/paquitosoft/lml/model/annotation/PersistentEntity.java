package org.paquitosoft.lml.model.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  This annotation is used to tell the library that an entity is persistent.
 * 
 * @author paquitosoft
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PersistentEntity {

    /**
     * The name of the table for this entity.
     * (default: empty String)
     */
    String tableName() default "";
 
    /**
     * The name of the attribute of this entity used to control 
     * the update of the entity.
     * (default: empty String)
     */
    String updateControlAttribute() default "";

    /**
     * This atrtibute indicates that the library must generate 
     * the keys for this entity
     * (default: false)
     */
    boolean generateKey() default false;
}
