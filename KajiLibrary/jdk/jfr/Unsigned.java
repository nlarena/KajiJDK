package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The field is an <strong>unsigned</strong> integer, even though its Java type has a sign.
 *
 * <p>Java has no unsigned integers, so a large value kept in a {@code long} comes out negative.
 * This annotation tells the one who reads to interpret it the other way round, and it is the only
 * way of showing properly a value that came from native code.
 *
 * @since 9
 */
@Target({ ElementType.FIELD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
@ContentType
@Label("Unsigned Value")
@Description("Value should be interpreted as unsigned data type")
public @interface Unsigned {
}
