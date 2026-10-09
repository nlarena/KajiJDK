package java.io;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * KajiLibrary's java.io.Serial -- it marks a field or a method as part of the serialization
 * mechanism, so that the compiler checks it.
 *
 * <p>It exists because the serialization members are declared <b>by name and signature
 * convention</b>, not by implementing anything: {@code writeObject} is private, it neither
 * overrides nor implements a method, and if it is written with the wrong signature nobody complains
 * -- it simply never gets called, and the object is serialized differently from how its author
 * believed. A {@code serialVersionUID} that is not {@code private static final long} has the same
 * problem: it is ignored silently.
 *
 * <p>This annotation turns that silent mistake into a compilation error. It is the same treatment
 * {@code @Override} gives inherited methods, and for the same reason: where the contract is a
 * convention and not a type, something is needed to say so out loud.
 *
 * <p>It applies to {@code serialVersionUID}, {@code serialPersistentFields}, {@code writeObject},
 * {@code readObject}, {@code readObjectNoData}, {@code writeReplace} and {@code readResolve}.
 *
 * <p>It has no effect at run time: it is {@code SOURCE}, the compiler checks it and it never
 * reaches the {@code .class}.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface Serial {
}
