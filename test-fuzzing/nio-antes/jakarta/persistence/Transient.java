package jakarta.persistence;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

/**
 * The {@code @Transient} annotation of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary rather than from the specification
 * sources: the members come from the class file's method descriptors and the
 * {@code default} clauses from its {@code AnnotationDefault} attributes. What the
 * annotation <em>means</em> is defined by the Jakarta Persistence specification,
 * not here.
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface Transient {
}
