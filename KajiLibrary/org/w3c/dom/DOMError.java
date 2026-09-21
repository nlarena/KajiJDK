package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.DOMError -- a problem found while processing a document.
 *
 * <p>It has nothing to do with {@link DOMException} and it is as well not to mix them. A
 * {@code DOMException} is **thrown** when the caller asked for something impossible and it cuts
 * right there; a {@code DOMError} is **reported** to a {@link DOMErrorHandler} during a long
 * operation --{@link Document#normalizeDocument}, a validation, a load-- that wants to carry on and
 * collect all the problems instead of dying at the first.
 *
 * <p>Hence the three severities, which as in any error report are not told apart by gravity but by
 * **what can happen next**: with {@link #SEVERITY_WARNING} the processing goes on normally; with
 * {@link #SEVERITY_ERROR} one can go on but the result is no longer reliable; and with
 * {@link #SEVERITY_FATAL_ERROR} one cannot continue.
 *
 * <p>The three values are 1, 2 and 3 and come from the specification.
 *
 * <p>The interface is declared whole.
 */
public interface DOMError {

    /** The processing goes on normally. */
    public static final short SEVERITY_WARNING = 1;

    /** One can continue, but the result is no longer reliable. */
    public static final short SEVERITY_ERROR = 2;

    /** One cannot continue. */
    public static final short SEVERITY_FATAL_ERROR = 3;

    /** One of the three {@code SEVERITY_*} constants. */
    public short getSeverity();

    /** The message to read, in the language of the implementation. */
    public String getMessage();

    /**
     * The type of the error, a string of the standard such as {@code "wf-invalid-character"} or
     * {@code "unbound-prefix-in-entity-reference"}.
     *
     * <p>It is what one has to look at to decide in code: the message is meant for a person, this
     * for a {@code switch}.
     */
    public String getType();

    /** The exception that caused it, if there was one. */
    public Object getRelatedException();

    /** The related datum --typically the offending node-- or {@code null}. */
    public Object getRelatedData();

    /** Where it happened. */
    public DOMLocator getLocation();
}
