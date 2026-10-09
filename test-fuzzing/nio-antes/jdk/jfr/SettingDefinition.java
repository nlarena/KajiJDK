package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * It marks a method as the one that defines a <strong>setting</strong> of the event.
 *
 * <p>The method takes a {@link SettingControl} and returns {@code boolean}: it is the filter that
 * decides, on each emission, whether the event is recorded. It is what allows an event to have a
 * setting of its own --not only the ones JFR brings-- configurable from outside just like
 * {@code threshold} or {@code stackTrace}.
 *
 * @since 9
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface SettingDefinition {
}
