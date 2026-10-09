package javax.swing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * It says whether a component class is a container, for the visual tools.
 *
 * <h2>Java's container is not the tool's container</h2>
 *
 * <p>Almost every Swing component inherits from {@code Container}, but very few accept one
 * dropping things inside them: a {@link JButton} <em>is</em> a Java container and it makes no
 * sense at all to drop a text field into it. This annotation is how a class tells a screen
 * builder which of the two things it is.
 *
 * <p>{@link #delegate} exists for those that do accept, but not directly: in a
 * {@link JScrollPane} what is added goes to its view, not to it. The name put there is that of
 * the method that returns the real container.
 *
 * <p>It is kept at run time because whoever reads it -- the tool -- sees the class already
 * compiled, not its source.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SwingContainer {

    /** Whether it accepts things being added to it. */
    boolean value() default true;

    /** The method that returns the real container; see the annotation's note. */
    String delegate() default "";
}
