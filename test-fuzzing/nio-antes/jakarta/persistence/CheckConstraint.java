package jakarta.persistence;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The {@code @CheckConstraint} annotation of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary rather than from the specification
 * sources: the members come from the class file's method descriptors and the
 * {@code default} clauses from its {@code AnnotationDefault} attributes. What the
 * annotation <em>means</em> is defined by the Jakarta Persistence specification,
 * not here.
 */
@Retention(RUNTIME)
public @interface CheckConstraint {

    /**
     * The {@code name} member.
     *
     * @return the configured value, or the default shown
     */
    String name() default "";

    /**
     * The {@code constraint} member.
     *
     * @return the configured value
     */
    String constraint();

    /**
     * The {@code options} member.
     *
     * @return the configured value, or the default shown
     */
    String options() default "";
}
