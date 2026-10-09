package jakarta.persistence;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Repeatable;

/**
 * The {@code @NamedQuery} annotation of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary rather than from the specification
 * sources: the members come from the class file's method descriptors and the
 * {@code default} clauses from its {@code AnnotationDefault} attributes. What the
 * annotation <em>means</em> is defined by the Jakarta Persistence specification,
 * not here.
 */
@Repeatable(NamedQueries.class)
@Target(TYPE)
@Retention(RUNTIME)
public @interface NamedQuery {

    /**
     * The {@code name} member.
     *
     * @return the configured value
     */
    String name();

    /**
     * The {@code query} member.
     *
     * @return the configured value
     */
    String query();

    /**
     * The {@code resultClass} member.
     *
     * @return the configured value
     */
    Class<?> resultClass();

    /**
     * The {@code lockMode} member.
     *
     * @return the configured value, or the default shown
     */
    LockModeType lockMode() default LockModeType.NONE;

    /**
     * The {@code hints} member.
     *
     * @return the configured value, or the default shown
     */
    QueryHint[] hints() default {};
}
