package jakarta.persistence;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import java.lang.annotation.Repeatable;

/**
 * The {@code @JoinColumn} annotation of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary rather than from the specification
 * sources: the members come from the class file's method descriptors and the
 * {@code default} clauses from its {@code AnnotationDefault} attributes. What the
 * annotation <em>means</em> is defined by the Jakarta Persistence specification,
 * not here.
 */
@Repeatable(JoinColumns.class)
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface JoinColumn {

    /**
     * The {@code name} member.
     *
     * @return the configured value, or the default shown
     */
    String name() default "";

    /**
     * The {@code referencedColumnName} member.
     *
     * @return the configured value, or the default shown
     */
    String referencedColumnName() default "";

    /**
     * The {@code unique} member.
     *
     * @return the configured value, or the default shown
     */
    boolean unique() default false;

    /**
     * The {@code nullable} member.
     *
     * @return the configured value, or the default shown
     */
    boolean nullable() default true;

    /**
     * The {@code insertable} member.
     *
     * @return the configured value, or the default shown
     */
    boolean insertable() default true;

    /**
     * The {@code updatable} member.
     *
     * @return the configured value, or the default shown
     */
    boolean updatable() default true;

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
     * The {@code table} member.
     *
     * @return the configured value, or the default shown
     */
    String table() default "";

    /**
     * The {@code foreignKey} member.
     *
     * @return the configured value, or the default shown
     */
    ForeignKey foreignKey() default @ForeignKey(value = ConstraintMode.PROVIDER_DEFAULT);

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
}
