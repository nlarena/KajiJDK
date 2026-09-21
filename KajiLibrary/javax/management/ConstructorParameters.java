package javax.management;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Names the parameters of a constructor so that the object can be rebuilt.
 *
 * <p>The problem it solves: to go back from a {@code CompositeData} to the Java object you need to
 * know which item corresponds to which constructor parameter, and bytecode does not keep parameter
 * names unless compiled with {@code -parameters}. The annotation declares them by hand, in the same
 * order as the signature.
 *
 * <p>Version note: it is the successor of {@code java.beans.ConstructorProperties} and exists
 * precisely so as not to drag {@code java.desktop} into {@code java.management}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
public @interface ConstructorParameters {

    /** The names, in the order of the constructor's parameters. */
    String[] value();
}
