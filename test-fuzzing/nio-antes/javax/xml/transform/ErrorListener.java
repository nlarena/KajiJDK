package javax.xml.transform;

/**
 * KajiLibrary's javax.xml.transform.ErrorListener -- who receives the problems of a transformation.
 *
 * <p>It exists because an XSLT processor cannot decide on its own what to do with an error. A
 * stylesheet that references a nonexistent template is fatal for a server that generates invoices
 * and an ignorable warning for an editor that shows a preview while the user types. The policy is
 * set by the caller; the processor only **reports**.
 *
 * <p>The three levels are not told apart by severity but by **what can keep happening afterwards**,
 * which is the only thing the processor really knows:
 *
 * <ul>
 *   <li>{@link #warning} -- processing continues normally;
 *   <li>{@link #error} -- a recoverable violation was detected; the processor is going to go on so
 *       as to report more than one error per run, but the result is no longer reliable;
 *   <li>{@link #fatalError} -- it cannot continue; the result, if there is one, is incomplete.
 * </ul>
 *
 * <p>And here is the surprising part, because it inverts control: the three methods can **throw**
 * {@link TransformerException}, and throwing it is the way of telling the processor "stop".
 * Returning normally from {@link #error} is authorizing it to go on. Hence the rule the spec
 * insists on and that a listener written in a hurry always breaks: **an {@code ErrorListener} must
 * never return normally from {@link #fatalError}**, because the processor is then allowed to
 * continue on a state it itself declared unusable, and whatever comes out of there means nothing.
 */
public interface ErrorListener {

    /**
     * A warning. Processing goes on all the same.
     *
     * @param exception the warning, with its location if known
     * @throws TransformerException to abort the transformation
     */
    void warning(TransformerException exception) throws TransformerException;

    /**
     * A recoverable error. Returning normally authorizes going on.
     *
     * @param exception the error, with its location if known
     * @throws TransformerException to abort the transformation
     */
    void error(TransformerException exception) throws TransformerException;

    /**
     * An error there is no coming back from. One must not return normally from here.
     *
     * @param exception the error, with its location if known
     * @throws TransformerException to abort the transformation, which is what is due
     */
    void fatalError(TransformerException exception) throws TransformerException;
}
