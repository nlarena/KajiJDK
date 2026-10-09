package java.security;

// The same as `PrivilegedAction`, but for actions that can throw a checked exception.
//
// They are two interfaces and not one with `throws Exception` because the common case —an action
// that throws nothing— has no reason to force its caller to write an empty `catch`. The one that
// throws is wrapped in `PrivilegedActionException`; the one that does not needs no wrapping.
@FunctionalInterface
@Deprecated
public interface PrivilegedExceptionAction<T> {

    T run() throws Exception;
}
