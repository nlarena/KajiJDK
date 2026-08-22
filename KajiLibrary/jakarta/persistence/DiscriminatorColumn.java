package jakarta.persistence;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.TYPE;

/**
 * The {@code @DiscriminatorColumn} annotation of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary rather than from the specification
 * sources: the members come from the class file's method descriptors and the
 * {@code default} clauses from its {@code AnnotationDefault} attributes. What the
 * annotation <em>means</em> is defined by the Jakarta Persistence specification,
 * not here.
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface DiscriminatorColumn {

    /**
     * The {@code name} member.
     *
     * @return the configured value, or the default shown
     */
    String name() default "DTYPE";

    /**
     * The {@code discriminatorType} member.
     *
     * @return the configured value, or the default shown
     */
    DiscriminatorType discriminatorType() default DiscriminatorType.STRING;

    /**
     * The {@code columnDefinition} member.
     *
     * @return the configured value, or the default shown
     */
    String columnDefinition() default "";

    /**
     * The {@code options} member.
     *
     * @return the configured value, or the default shown
     */
    String options() default "";

    /**
     * The {@code length} member.
     *
     * @return the configured value, or the default shown
     */
    int length() default 31;
}
