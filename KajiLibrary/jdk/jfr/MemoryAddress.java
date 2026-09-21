package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The field is a <strong>memory address</strong>.
 *
 * <p>It is shown in hexadecimal, which is the only way in which one address can be compared with
 * another at a glance.
 *
 * @since 9
 */
@Target({ ElementType.FIELD, ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
@ContentType
@Label("Memory Address")
@Description("Represents a physical memory address")
public @interface MemoryAddress {
}
