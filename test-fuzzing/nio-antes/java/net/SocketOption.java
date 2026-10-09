package java.net;

/**
 * KajiLibrary's java.net.SocketOption — a socket option, with its name and the type of its value.
 *
 * <p>The two questions it declares are exactly what is needed to make options **extensible and safe
 * at the same time**: the name identifies the option, and `type()` says what type its value is, so
 * that `setOption(option, value)` can check at compile time —through the type variable— and at run
 * time too that the value matches.
 *
 * <p>Without this there would have to be one method per option, or `Object` would have to be passed
 * and the error discovered late.
 *
 * @param <T> the type of the option's value
 */
public interface SocketOption<T> {

    /** The option's name. */
    String name();

    /** The type of its value. */
    Class<T> type();
}
