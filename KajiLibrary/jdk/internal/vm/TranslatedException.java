package jdk.internal.vm;

/**
 * KajiLibrary's jdk.internal.vm.TranslatedException -- an exception that crossed a border.
 *
 * <p>It exists for the JIT compiler written in Java (JVMCI): when an exception is born on the other
 * side of that border, the original object **cannot be brought over** --it lives in another heap,
 * or its class is not loaded on this side-- so it is encoded to bytes, passed, and rebuilt. What
 * cannot be rebuilt is represented by one of these, which keeps the name of the class and the
 * message.
 *
 * <p>The constructor is package-private: nobody from outside manufactures one. They appear only
 * when decoding.
 */
public final class TranslatedException extends Exception {

    private final String originalClass;

    TranslatedException(Throwable original) {
        super(original == null ? null : original.toString());
        this.originalClass = original == null ? null : original.getClass().getName();
    }

    /**
     * It does not capture the stack, and returns `this`.
     *
     * <p>It is the part of the whole class with an intention. The stack of a translated exception
     * would be that of the **decoder** --the place where it was rebuilt-- and not that of the point
     * where the original exception happened, which is the only thing anybody would care about. A
     * stack that points to the wrong place is worse than none: it reads as if it were the real one.
     */
    public Throwable fillInStackTrace() {
        return this;
    }

    /**
     * The name of the original class, or `null`. Package-private: it is a detail of the
     * translation.
     */
    String originalClass() {
        return this.originalClass;
    }
}
