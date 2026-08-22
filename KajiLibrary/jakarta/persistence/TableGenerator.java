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
 * The {@code @TableGenerator} annotation of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary rather than from the specification
 * sources: the members come from the class file's method descriptors and the
 * {@code default} clauses from its {@code AnnotationDefault} attributes. What the
 * annotation <em>means</em> is defined by the Jakarta Persistence specification,
 * not here.
 */
@Repeatable(TableGenerators.class)
@Target({TYPE, METHOD, FIELD, PACKAGE})
@Retention(RUNTIME)
public @interface TableGenerator {

    /**
     * The {@code name} member.
     *
     * @return the configured value, or the default shown
     */
    String name() default "";

    /**
     * The {@code table} member.
     *
     * @return the configured value, or the default shown
     */
    String table() default "";

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
     * The {@code pkColumnName} member.
     *
     * @return the configured value, or the default shown
     */
    String pkColumnName() default "";

    /**
     * The {@code valueColumnName} member.
     *
     * @return the configured value, or the default shown
     */
    String valueColumnName() default "";

    /**
     * The {@code pkColumnValue} member.
     *
     * @return the configured value, or the default shown
     */
    String pkColumnValue() default "";

    /**
     * The {@code initialValue} member.
     *
     * @return the configured value, or the default shown
     */
    int initialValue() default 0;

    /**
     * The {@code allocationSize} member.
     *
     * @return the configured value, or the default shown
     */
    int allocationSize() default 50;

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
     * The {@code options} member.
     *
     * @return the configured value, or the default shown
     */
    String options() default "";
}
