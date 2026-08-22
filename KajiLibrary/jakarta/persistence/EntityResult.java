package jakarta.persistence;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The {@code @EntityResult} annotation of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary rather than from the specification
 * sources: the members come from the class file's method descriptors and the
 * {@code default} clauses from its {@code AnnotationDefault} attributes. What the
 * annotation <em>means</em> is defined by the Jakarta Persistence specification,
 * not here.
 */
@Retention(RUNTIME)
public @interface EntityResult {

    /**
     * The {@code entityClass} member.
     *
     * @return the configured value
     */
    Class<?> entityClass();

    /**
     * The {@code lockMode} member.
     *
     * @return the configured value, or the default shown
     */
    LockModeType lockMode() default LockModeType.OPTIMISTIC;

    /**
     * The {@code fields} member.
     *
     * @return the configured value, or the default shown
     */
    FieldResult[] fields() default {};

    /**
     * The {@code discriminatorColumn} member.
     *
     * @return the configured value, or the default shown
     */
    String discriminatorColumn() default "";
}
