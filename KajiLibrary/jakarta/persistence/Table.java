package jakarta.persistence;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.TYPE;

/**
 * The {@code @Table} annotation of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary rather than from the specification
 * sources: the members come from the class file's method descriptors and the
 * {@code default} clauses from its {@code AnnotationDefault} attributes. What the
 * annotation <em>means</em> is defined by the Jakarta Persistence specification,
 * not here.
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Table {

    /**
     * The {@code name} member.
     *
     * @return the configured value, or the default shown
     */
    String name() default "";

    /**
     * The {@code catalog} member.
     *
     * @return the configured value, or the default shown
     */
    String catalog() default "";

    /**
     * The {@code schema} member.
     *
     * @return the configured value, or the default shown
     */
    String schema() default "";

    /**
     * The {@code uniqueConstraints} member.
     *
     * @return the configured value, or the default shown
     */
    UniqueConstraint[] uniqueConstraints() default {};

    /**
     * The {@code indexes} member.
     *
     * @return the configured value, or the default shown
     */
    Index[] indexes() default {};

    /**
     * The {@code check} member.
     *
     * @return the configured value, or the default shown
     */
    CheckConstraint[] check() default {};

    /**
     * The {@code comment} member.
     *
     * @return the configured value, or the default shown
     */
    String comment() default "";

    /**
     * The {@code options} member.
     *
     * @return the configured value, or the default shown
     */
    String options() default "";
}
