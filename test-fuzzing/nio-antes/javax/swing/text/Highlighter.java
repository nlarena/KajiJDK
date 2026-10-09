package javax.swing.text;

import java.awt.Graphics;
import java.awt.Shape;

/**
 * Who paints the coloured backgrounds of the text: the selection, a search's results.
 *
 * <p>A highlight is a stretch plus a {@link HighlightPainter} that knows how to paint it.
 * Separating the two things is what allows having at once a blue selection and five yellow
 * matches without the component knowing about any of them.
 *
 * <p>{@link #addHighlight} returns an opaque tag, not an index: the stretches move when the
 * document changes, and an index would stop holding.
 */
public interface Highlighter {

    void install(JTextComponent c);

    void deinstall(JTextComponent c);

    void paint(Graphics g);

    /** It adds a highlight and returns what to remove it with later. */
    Object addHighlight(int p0, int p1, HighlightPainter p) throws BadLocationException;

    void removeHighlight(Object tag);

    void removeAllHighlights();

    /**
     * It changes the stretch of a highlight already there; it is how the selection follows the
     * cursor.
     */
    void changeHighlight(Object tag, int p0, int p1) throws BadLocationException;

    Highlight[] getHighlights();

    /** Who knows how to paint a highlight. */
    public interface HighlightPainter {

        /** It paints the stretch {@code [p0, p1)} inside that shape. */
        void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c);
    }

    /** A highlight in place: its stretch and who paints it. */
    public interface Highlight {

        int getStartOffset();

        int getEndOffset();

        HighlightPainter getPainter();
    }
}
