package io.piotrjastrzebski.bte;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark Task class or Task field with a userComment
 * Attribute edit fields will show this info
 * If Task already has a userComment in the tree file both will be shown
 * Created by PiotrJ on 16/10/15.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface TaskComment {
	String value() default "";
}
