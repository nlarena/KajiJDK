package jakarta.persistence;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Repeatable;

/**
 * The {@code @Convert} annotation of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary rather than from the specification
 * sources: the members come from the class file's method descriptors and the
 * {@code default} clauses from its {@code AnnotationDefault} attributes. What the
 * annotation <em>means</em> is defined by the Jakarta Persistence specification,
 * not here.
 */
@Repeatable(Converts.class)
@Target({METHOD, FIELD, TYPE})
@Retention(RUNTIME)
public @interface Convert {

    /**
     * The {@code converter} member.
     *
     * @return the configured value, or the default shown
     */
    Class<? extends AttributeConverter> converter() default AttributeConverter.class;

    /**
     * The {@code attributeName} member.
     *
     * @return the configured value, or the default shown
     */
    String attributeName() default "";

    /**
     * The {@code disableConversion} member.
     *
     * @return the configured value, or the default shown
     */
    boolean disableConversion() default false;
}
