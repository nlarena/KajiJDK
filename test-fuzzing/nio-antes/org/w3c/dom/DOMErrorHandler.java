package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.DOMErrorHandler -- the one who receives the {@link DOMError}s.
 *
 * <p>It is registered in the {@link DOMConfiguration} of the document under the {@code
 * "error-handler"} parameter. One single method, and the value it returns **inverts the control**:
 * with {@code true} the processor carries on, with {@code false} it stops. It is the only way the
 * caller has of imposing its policy --what for a server is fatal for an editor is a warning--
 * because the processor cannot know it.
 *
 * <p>The rule that always gets broken: returning {@code true} on a {@link
 * DOMError#SEVERITY_FATAL_ERROR} does **not** make the processor continue. A fatal error is fatal;
 * the standard says the processor may ignore the answer, and whatever comes out of carrying on over
 * a state it itself declared unusable means nothing.
 *
 * <p>The interface is declared whole.
 */
public interface DOMErrorHandler {

    /**
     * @param error the problem, which is only valid during this call: keeping the reference and
     *     reading it later is not guaranteed
     * @return {@code true} to continue, {@code false} to stop
     */
    public boolean handleError(DOMError error);
}
