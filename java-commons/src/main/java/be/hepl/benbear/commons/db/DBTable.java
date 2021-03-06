package be.hepl.benbear.commons.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as mappable to a sql table.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DBTable {

    /**
     * The name of the sql table the annotated class is mappable to.
     */
    String value();

}
