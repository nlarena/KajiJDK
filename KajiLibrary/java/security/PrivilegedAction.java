package java.security;

// A block of code to run with the privileges of whoever defines it.
//
// Deprecated along with `AccessController`: since the `SecurityManager` is disabled, running "with
// privileges" and running normally are the same thing. It survives as a functional type because
// there are signatures in several libraries that name it.
@FunctionalInterface
@Deprecated
public interface PrivilegedAction<T> {

    T run();
}
