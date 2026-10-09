package jakarta.persistence;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Repeatable;

/**
 * The {@code @PersistenceContext} annotation of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary rather than from the specification
 * sources: the members come from the class file's method descriptors and the
 * {@code default} clauses from its {@code AnnotationDefault} attributes. What the
 * annotation <em>means</em> is defined by the Jakarta Persistence specification,
 * not here.
 */
@Repeatable(PersistenceContexts.class)
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface PersistenceContext {

    /**
     * The {@code name} member.
     *
     * @return the configured value, or the default shown
     */
    String name() default "";

    /**
     * The {@code unitName} member.
     *
     * @return the configured value, or the default shown
     */
    String unitName() default "";

    /**
     * The {@code type} member.
     *
     * @return the configured value, or the default shown
     */
    PersistenceContextType type() default PersistenceContextType.TRANSACTION;

    /**
     * The {@code synchronization} member.
     *
     * @return the configured value, or the default shown
     */
    SynchronizationType synchronization() default SynchronizationType.SYNCHRONIZED;

    /**
     * The {@code properties} member.
     *
     * @return the configured value, or the default shown
     */
    PersistenceProperty[] properties() default {};
}
