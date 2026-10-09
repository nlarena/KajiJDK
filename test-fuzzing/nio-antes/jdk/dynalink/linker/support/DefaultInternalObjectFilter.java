package jdk.dynalink.linker.support;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import jdk.dynalink.linker.MethodHandleTransformer;

/**
 * Keeps a language's internal objects from escaping into its host.
 *
 * <h2>The problem</h2>
 *
 * <p>A language running on the JVM almost always represents its values with classes of its own — a
 * scripting string has no reason to be a Java {@code String}. While those objects circulate inside
 * the language that is fine. The problem appears at the border: a Java method taking {@code Object}
 * and returning {@code Object} may end up storing in a collection an object only the language knows
 * how to interpret.
 *
 * <p>The solution could be to remember to convert at every exit point. Nobody remembers every time.
 * This class does it once and for all method handles.
 *
 * <h2>Why it only touches the {@code Object} parameters</h2>
 *
 * <p>Because the rest are already typed: a parameter declared {@code String} cannot receive an
 * internal object of the language, the verifier would not allow it. {@code Object} is exactly the
 * place something untyped can get through, and that is why it is the only one that has to be
 * filtered. The same on the return side, and the same with {@code Object[]} for the variable
 * parameter.
 *
 * <h2>State in this VM</h2>
 *
 * <p>The decisions about what to filter are real and they happen. Applying them needs
 * {@code MethodHandles.filterArguments}, which cannot yet be built without VM support: if there is
 * anything to filter, the method ends in {@link UnsupportedOperationException}. A handle with nothing
 * to filter comes back untouched, without going near the factory.
 *
 * @since 9
 */
public class DefaultInternalObjectFilter implements MethodHandleTransformer {

    private final MethodHandle parameterFilter;
    private final MethodHandle returnFilter;

    /**
     * The two filters, either of them optional.
     *
     * @param parameterFilter what to apply to the incoming arguments, or {@code null}
     * @param returnFilter what to apply to the outgoing value, or {@code null}
     * @throws IllegalArgumentException if either is not of type {@code (Object)Object}
     */
    public DefaultInternalObjectFilter(final MethodHandle parameterFilter,
            final MethodHandle returnFilter) {
        this.parameterFilter = check(parameterFilter, "parameterFilter");
        this.returnFilter = check(returnFilter, "returnFilter");
    }

    /**
     * A filter has to be {@code (Object)Object}.
     *
     * <p>The requirement is not red tape: the filter is going to be inserted where an {@code Object}
     * was, so if it took or returned anything else the resulting handle's signature would not add up.
     * Failing here, with the argument's name, is more useful than failing in the middle of a link.
     */
    private static MethodHandle check(final MethodHandle filter, final String name) {
        if (filter == null) {
            return null;
        }
        final MethodType type = filter.type();
        if (type.parameterCount() != 1 || type.parameterType(0) != Object.class
                || type.returnType() != Object.class) {
            throw new IllegalArgumentException(name + " must be of type (Object)Object");
        }
        return filter;
    }

    /**
     * The handle with the filters put where they are needed.
     *
     * @param target the original handle
     * @return the filtered one, or the same one if there was nothing to filter
     */
    public MethodHandle transform(final MethodHandle target) {
        final MethodType type = target.type();
        final boolean varargs = target.isVarargsCollector();
        // The last parameter of a variable-arity handle is the array, and it is handled apart: what
        // has to be filtered is its elements, not the array.
        final int fixed = type.parameterCount() - (varargs ? 1 : 0);

        MethodHandle[] filters = null;
        if (parameterFilter != null) {
            for (int i = 0; i < fixed; i++) {
                if (type.parameterType(i) == Object.class) {
                    if (filters == null) {
                        filters = new MethodHandle[fixed];
                    }
                    filters[i] = parameterFilter;
                }
            }
        }

        MethodHandle result = target;
        if (filters != null) {
            result = MethodHandles.filterArguments(target, 0, filters);
        }
        if (returnFilter != null && type.returnType() == Object.class) {
            result = MethodHandles.filterReturnValue(result, returnFilter);
        }
        if (varargs && result != target) {
            // filterArguments and filterReturnValue return fixed-arity handles: it has to be marked
            // variable again or the site would stop being able to pass it loose arguments, which is
            // the only reason it was variable.
            result = result.asVarargsCollector(type.parameterType(fixed));
        }
        return result;
    }
}
