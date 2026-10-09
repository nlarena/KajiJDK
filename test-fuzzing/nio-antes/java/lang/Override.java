package java.lang;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * KajiLibrary's java.lang.Override — asserts that the method below it overrides one it
 * inherits. It carries no data and survives only until compilation: its whole job is to
 * make a *typo* in a method name a compile error instead of a silently unused method.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Override {
}
