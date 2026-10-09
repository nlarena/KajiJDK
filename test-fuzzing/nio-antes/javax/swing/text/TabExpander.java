package javax.swing.text;

/**
 * Who knows where the next tab falls.
 *
 * <p>A single question, and with two arguments that look unnecessary: the position in pixels and
 * the tab character's <em>model offset</em>. The second is needed because the stops may be
 * different according to the paragraph, and the paragraph is known from the offset.
 */
public interface TabExpander {

    /**
     * Where the tab that starts at {@code x} ends.
     *
     * @param x the current position, in pixels
     * @param tabOffset the offset of the tab character in the document
     */
    float nextTabStop(float x, int tabOffset);
}
