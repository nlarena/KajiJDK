package jdk.dynalink.beans;

import java.io.Serializable;
import java.util.Objects;

/**
 * The object that stands for a class as the <strong>carrier of its static members</strong>.
 *
 * <h2>Why {@code Class} is not enough</h2>
 *
 * <p>Because a {@code Class} already means something else: it is an instance of
 * {@code java.lang.Class}, with methods of its own. If a dynamic language wrote
 * {@code String.valueOf(1)} and the object on the left were {@code String.class}, looking up
 * {@code valueOf} would find it... in {@code Class}, which does not have it, and would never reach
 * {@code String}'s.
 *
 * <p>Worse still: {@code String.class.getName()} would answer {@code "java.lang.String"} when what
 * was meant was the static {@code getName} method of the class. The two sets of members collide.
 *
 * <p>{@code StaticClass} separates the two. A {@code StaticClass} of {@code String} exposes
 * {@code String}'s <strong>static</strong> members and its constructor; the {@code Class} of
 * {@code String} goes on exposing its own.
 *
 * <h2>There is one per class</h2>
 *
 * <p>{@link #forClass} always returns the same instance for the same class, so they can be compared
 * by identity. That is exactly why it has no public constructor.
 *
 * @since 9
 */
public final class StaticClass implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The cache, named rather than anonymous because of #482: the bytecode generator does not emit
     * an anonymous class sitting in a field initializer. It also reads better in a stack dump than
     * a {@code StaticClass$1}.
     */
    private static final class Cache extends ClassValue<StaticClass> {
        protected StaticClass computeValue(final Class<?> type) {
            return new StaticClass(type);
        }
    }

    private static final Cache CACHE = new Cache();

    private final Class<?> clazz;

    private StaticClass(final Class<?> clazz) {
        this.clazz = clazz;
    }

    /**
     * The stand-in for that class.
     *
     * <p>The cache is a {@link ClassValue} and not a map: it hangs off the class itself and goes
     * away with it when it is unloaded. An ordinary map would keep any class that ever passed
     * through here from being unloaded, and in a scripting language those are many.
     *
     * @param clazz the class
     * @return its stand-in, always the same one
     * @throws NullPointerException if the class is {@code null}
     */
    public static StaticClass forClass(final Class<?> clazz) {
        return CACHE.get(Objects.requireNonNull(clazz));
    }

    /**
     * The class being stood in for.
     *
     * @return the class
     */
    public Class<?> getRepresentedClass() {
        return clazz;
    }

    /** {@inheritDoc} */
    public String toString() {
        return "JavaClass[" + clazz.getName() + "]";
    }
}
