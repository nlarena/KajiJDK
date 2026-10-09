package jakarta.persistence;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

/**
 * The {@code @JoinTable} annotation of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary rather than from the specification
 * sources: the members come from the class file's method descriptors and the
 * {@code default} clauses from its {@code AnnotationDefault} attributes. What the
 * annotation <em>means</em> is defined by the Jakarta Persistence specification,
 * not here.
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface JoinTable {

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
     * The {@code joinColumns} member.
     *
     * @return the configured value, or the default shown
     */
    JoinColumn[] joinColumns() default {};

    /**
     * The {@code inverseJoinColumns} member.
     *
     * @return the configured value, or the default shown
     */
    JoinColumn[] inverseJoinColumns() default {};

    /**
     * The {@code foreignKey} member.
     *
     * @return the configured value, or the default shown
     */
    ForeignKey foreignKey() default @ForeignKey(value = ConstraintMode.PROVIDER_DEFAULT);

    /**
     * The {@code inverseForeignKey} member.
     *
     * @return the configured value, or the default shown
     */
    ForeignKey inverseForeignKey() default @ForeignKey(value = ConstraintMode.PROVIDER_DEFAULT);

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
