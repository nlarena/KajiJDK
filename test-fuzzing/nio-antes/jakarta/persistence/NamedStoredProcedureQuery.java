package jakarta.persistence;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Repeatable;

/**
 * The {@code @NamedStoredProcedureQuery} annotation of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary rather than from the specification
 * sources: the members come from the class file's method descriptors and the
 * {@code default} clauses from its {@code AnnotationDefault} attributes. What the
 * annotation <em>means</em> is defined by the Jakarta Persistence specification,
 * not here.
 */
@Repeatable(NamedStoredProcedureQueries.class)
@Target(TYPE)
@Retention(RUNTIME)
public @interface NamedStoredProcedureQuery {

    /**
     * The {@code name} member.
     *
     * @return the configured value
     */
    String name();

    /**
     * The {@code procedureName} member.
     *
     * @return the configured value
     */
    String procedureName();

    /**
     * The {@code parameters} member.
     *
     * @return the configured value, or the default shown
     */
    StoredProcedureParameter[] parameters() default {};

    /**
     * The {@code resultClasses} member.
     *
     * @return the configured value, or the default shown
     */
    Class[] resultClasses() default {};

    /**
     * The {@code resultSetMappings} member.
     *
     * @return the configured value, or the default shown
     */
    String[] resultSetMappings() default {};

    /**
     * The {@code hints} member.
     *
     * @return the configured value, or the default shown
     */
    QueryHint[] hints() default {};
}
