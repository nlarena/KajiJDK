package jdk.dynalink.linker;

import java.lang.invoke.MethodHandle;

/**
 * Wraps a method handle with something added to all of them alike.
 *
 * <p>The intended use is the internal-object filter: a language that represents its values with
 * classes of its own does not want those classes escaping to its host, and instead of remembering to
 * convert at every exit point it installs a transformation that does it at all of them.
 *
 * @since 9
 */
@FunctionalInterface
public interface MethodHandleTransformer {

    /**
     * The transformed method handle.
     *
     * @param target the original method handle
     * @return the transformed one; never {@code null}
     */
    MethodHandle transform(MethodHandle target);
}
