package jakarta.persistence;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Repeatable;

/**
 * The {@code @SequenceGenerator} annotation of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary rather than from the specification
 * sources: the members come from the class file's method descriptors and the
 * {@code default} clauses from its {@code AnnotationDefault} attributes. What the
 * annotation <em>means</em> is defined by the Jakarta Persistence specification,
 * not here.
 */
@Repeatable(SequenceGenerators.class)
@Target({TYPE, METHOD, FIELD, PACKAGE})
@Retention(RUNTIME)
public @interface SequenceGenerator {

    /**
     * The {@code name} member.
     *
     * @return the configured value, or the default shown
     */
    String name() default "";

    /**
     * The {@code sequenceName} member.
     *
     * @return the configured value, or the default shown
     */
    String sequenceName() default "";

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
     * The {@code initialValue} member.
     *
     * @return the configured value, or the default shown
     */
    int initialValue() default 1;

    /**
     * The {@code allocationSize} member.
     *
     * @return the configured value, or the default shown
     */
    int allocationSize() default 50;

    /**
     * The {@code options} member.
     *
     * @return the configured value, or the default shown
     */
    String options() default "";
}
