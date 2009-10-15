package org.paquitosoft.lml.model.util;

import java.lang.reflect.Field;
import java.util.Comparator;

/**
 *  This class is intended to compare objects of type <b>Field</b> by its name to 
 * sort them.
 * 
 * @author paquitosoft
 */
public class FieldNameComparator implements Comparator {

    public int compare(Object o1, Object o2) {
        
        int result = 0;
        
        if (o1 instanceof Field && o2 instanceof Field) {            
            ((Field) o1).getName().compareTo(((Field) o2).getName());            
        }
        
        return result;
    }

}
