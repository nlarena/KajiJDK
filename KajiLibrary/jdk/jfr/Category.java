package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Where to place the event in the tree of categories of a tool.
 *
 * <p>It is an array because the tree has levels: {@code {"Java Application", "Statistics"}} puts
 * the event under "Statistics", which hangs from "Java Application".
 *
 * <p>Without this, an event of one's own is left loose among hundreds, which is the difference
 * between a navigable list and one nobody finds.
 *
 * @since 9
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
public @interface Category {

    /**
     * The value of the annotation.
     *
     * @return the value
     */
    String[] value();
}
