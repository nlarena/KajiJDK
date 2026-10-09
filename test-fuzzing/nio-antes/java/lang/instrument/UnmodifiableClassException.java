package java.lang.instrument;

/**
 * KajiLibrary's java.lang.instrument.UnmodifiableClassException -- that class cannot be redefined.
 *
 * <p>It comes out of {@code redefineClasses} and {@code retransformClasses}. There are classes the
 * virtual machine does not allow touching --the primitives, the arrays, and in practice a good part
 * of whatever was already running when the agent started--.
 *
 * <p>It is checked because which ones they are depends on the implementation and on the moment: a
 * serious agent asks {@code isModifiableClass} first, and catches this all the same in case
 * something changed between the question and the redefinition.
 */
public class UnmodifiableClassException extends Exception {

    private static final long serialVersionUID = 1716652643585309178L;

    /** With no detail. */
    public UnmodifiableClassException() {
        super();
    }

    /** With a message saying which one. */
    public UnmodifiableClassException(String s) {
        super(s);
    }
}
