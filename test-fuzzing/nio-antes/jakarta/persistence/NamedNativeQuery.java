package jakarta.persistence;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Repeatable;

/**
 * The {@code @NamedNativeQuery} annotation of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary rather than from the specification
 * sources: the members come from the class file's method descriptors and the
 * {@code default} clauses from its {@code AnnotationDefault} attributes. What the
 * annotation <em>means</em> is defined by the Jakarta Persistence specification,
 * not here.
 */
@Repeatable(NamedNativeQueries.class)
@Target(TYPE)
@Retention(RUNTIME)
public @interface NamedNativeQuery {

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
     * The {@code hints} member.
     *
     * @return the configured value, or the default shown
     */
    QueryHint[] hints() default {};

    /**
     * The {@code resultClass} member.
     *
     * @return the configured value
     */
    Class<?> resultClass();

    /**
     * The {@code resultSetMapping} member.
     *
     * @return the configured value, or the default shown
     */
    String resultSetMapping() default "";

    /**
     * The {@code entities} member.
     *
     * @return the configured value, or the default shown
     */
    EntityResult[] entities() default {};

    /**
     * The {@code classes} member.
     *
     * @return the configured value, or the default shown
     */
    ConstructorResult[] classes() default {};

    /**
     * The {@code columns} member.
     *
     * @return the configured value, or the default shown
     */
    ColumnResult[] columns() default {};
}
