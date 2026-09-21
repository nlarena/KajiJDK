package jdk.jfr;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The register of numeric type identifiers. It is not API.
 *
 * <p>JFR assigns a number to each type that appears in a recording --events, annotations, types of
 * field-- and the file refers to them by that number instead of by their name, which is what makes
 * a file with millions of events not repeat the string {@code "jdk.ObjectAllocation"} a million
 * times.
 *
 * <p>The contract of {@code getTypeId} is that the number identifies the type <strong>inside this
 * run of the VM</strong>; it is not stable between runs nor does it have meaning outside. That is
 * exactly what a counter fulfils, so these identifiers are real and not a filler: two different
 * types give different numbers and the same type always gives the same one.
 *
 * <p>It starts at 1 on purpose: the 0 is left free to mean "unassigned".
 */
final class Types {

    private static final AtomicLong NEXT = new AtomicLong(1);

    /**
     * The identifiers live in a {@link ClassValue} and not in a map so that they go away with the
     * class when it is unloaded. An ordinary map would keep alive any class that had appeared at
     * some point in an event.
     */
    private static final Registry REGISTRY = new Registry();

    private static final class Registry extends ClassValue<Long> {
        protected Long computeValue(final Class<?> type) {
            return Long.valueOf(NEXT.getAndIncrement());
        }
    }

    private Types() {
    }

    /**
     * The identifier of that type, always the same one.
     *
     * @param type the type, or {@code null}
     * @return the identifier, or {@code 0} if the type is {@code null}
     */
    static long id(final Class<?> type) {
        return type == null ? 0L : REGISTRY.get(type).longValue();
    }

    /** The identifiers of the types that have no Java class, by name. */
    private static final java.util.Map<String, Long> BY_NAME =
            new java.util.concurrent.ConcurrentHashMap<String, Long>();

    /**
     * The identifier of a type that <strong>has no Java class</strong>.
     *
     * <p>They exist: the {@code stackTrace} field of every event is of type
     * {@code jdk.types.StackTrace}, which is a type of the format of the recording and not a class.
     * Without this there would be no way of giving it an identifier.
     *
     * @param name the name of the type
     * @return the identifier, always the same one for the same name
     */
    static long idOfName(final String name) {
        Long v = BY_NAME.get(name);
        if (v == null) {
            // putIfAbsent and not put: two threads that ask for the same name at a time have to
            // receive the same number, not one each.
            final Long fresh = Long.valueOf(NEXT.getAndIncrement());
            v = BY_NAME.putIfAbsent(name, fresh);
            if (v == null) {
                v = fresh;
            }
        }
        return v.longValue();
    }

    /**
     * The name under which a type appears in the recording.
     *
     * <p>An array is named by its component: JFR marks the condition of being an array apart, in
     * {@link ValueDescriptor#isArray}, instead of putting it into the name.
     *
     * @param type the type
     * @return the name
     */
    static String name(final Class<?> type) {
        Class<?> t = type;
        while (t.isArray()) {
            t = t.getComponentType();
        }
        return t.getName();
    }
}
