package p;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** An annotation type compiled in the same invocation as the one that names it. */
@Retention(RetentionPolicy.RUNTIME)
public @interface Marca {
    String value();
}
