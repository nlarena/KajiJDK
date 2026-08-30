package java.lang;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * KajiLibrary's java.lang.StackWalker -- the API for walking the call stack lazily.
 *
 * <p>The <strong>surface</strong> of the JDK's class. {@link #getInstance()} and its overloads hand
 * back a walker, but the walk itself needs the VM to enumerate frames on demand, which KajiJDK does
 * not expose, so {@link #walk}, {@link #forEach} and {@link #getCallerClass()} throw {@link
 * UnsupportedOperationException}. The nested {@link Option} and {@link StackFrame} are the JDK's.
 */
public final class StackWalker {

    private StackWalker() {
    }

    /** A walker with no options. */
    public static StackWalker getInstance() {
        return new StackWalker();
    }

    /** A walker with a single option. */
    public static StackWalker getInstance(Option option) {
        return new StackWalker();
    }

    /** A walker with a set of options. */
    public static StackWalker getInstance(Set<Option> options) {
        return new StackWalker();
    }

    /** A walker with a set of options and an estimate of the stack depth to reserve. */
    public static StackWalker getInstance(Set<Option> options, int estimateDepth) {
        return new StackWalker();
    }

    /** Apply {@code function} to a lazy stream of the frames. */
    public <T> T walk(Function<? super Stream<StackFrame>, ? extends T> function) {
        throw new UnsupportedOperationException("StackWalker necesita la introspección de pila del VM");
    }

    /** Apply {@code action} to each frame. */
    public void forEach(Consumer<? super StackFrame> action) {
        throw new UnsupportedOperationException("StackWalker necesita la introspección de pila del VM");
    }

    /** The class that called the method calling this one (needs {@link Option#RETAIN_CLASS_REFERENCE}). */
    public Class<?> getCallerClass() {
        throw new UnsupportedOperationException("StackWalker necesita la introspección de pila del VM");
    }

    /** What a {@link StackWalker} may be asked to keep while walking. */
    public enum Option {
        /** Keep a live {@code Class} reference in each frame. */
        RETAIN_CLASS_REFERENCE,
        /** Drop the method name/type/bci to walk faster. */
        DROP_METHOD_INFO,
        /** Show reflection frames, normally hidden. */
        SHOW_REFLECT_FRAMES,
        /** Show every hidden frame (reflection and implementation). */
        SHOW_HIDDEN_FRAMES;
    }

    /** One frame of the call stack, as the walker sees it. */
    public interface StackFrame {
        /** The binary name of the declaring class. */
        String getClassName();

        /** The method name. */
        String getMethodName();

        /** The declaring class (needs {@link Option#RETAIN_CLASS_REFERENCE}). */
        Class<?> getDeclaringClass();

        /** The method's type, if it was retained. */
        default java.lang.invoke.MethodType getMethodType() {
            throw new UnsupportedOperationException("el MethodType no se retuvo");
        }

        /** The method's descriptor, if it was retained. */
        default String getDescriptor() {
            throw new UnsupportedOperationException("el descriptor no se retuvo");
        }

        /** The bytecode index of the execution point in the frame. */
        int getByteCodeIndex();

        /** The source file name, if known. */
        String getFileName();

        /** The source line number, if known. */
        int getLineNumber();

        /** Whether the method is native. */
        boolean isNativeMethod();

        /** This frame as a {@link StackTraceElement}. */
        StackTraceElement toStackTraceElement();
    }
}
