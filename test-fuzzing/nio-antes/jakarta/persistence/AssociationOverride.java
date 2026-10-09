package jakarta.persistence;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Repeatable;

/**
 * The {@code @AssociationOverride} annotation of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary rather than from the specification
 * sources: the members come from the class file's method descriptors and the
 * {@code default} clauses from its {@code AnnotationDefault} attributes. What the
 * annotation <em>means</em> is defined by the Jakarta Persistence specification,
 * not here.
 */
@Repeatable(AssociationOverrides.class)
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface AssociationOverride {

    /**
     * The {@code name} member.
     *
     * @return the configured value
     */
    String name();

    /**
     * The {@code joinColumns} member.
     *
     * @return the configured value, or the default shown
     */
    JoinColumn[] joinColumns() default {};

    /**
     * The {@code foreignKey} member.
     *
     * @return the configured value, or the default shown
     */
    ForeignKey foreignKey() default @ForeignKey(value = ConstraintMode.PROVIDER_DEFAULT);

    /**
     * The {@code joinTable} member.
     *
     * @return the configured value, or the default shown
     */
    JoinTable joinTable() default @JoinTable;
}
