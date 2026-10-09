package java.lang.instrument;

/**
 * KajiLibrary's java.lang.instrument.UnmodifiableModuleException -- that module cannot be redefined.
 *
 * <p>The equivalent of {@link UnmodifiableClassException} for modules, with one difference worth
 * looking at: this one is <b>not checked</b>.
 *
 * <p>The reason is that it is known in advance. A module either is modifiable or is not, and that
 * does not change while the program runs; {@code isModifiableModule} answers it without ambiguity.
 * Redefining one that is not is a programming error, not a condition to be handled.
 */
public class UnmodifiableModuleException extends RuntimeException {

    private static final long serialVersionUID = 6912511912351080644L;

    /** With no detail. */
    public UnmodifiableModuleException() {
        super();
    }

    /** With a message saying which one. */
    public UnmodifiableModuleException(String msg) {
        super(msg);
    }
}
