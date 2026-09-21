package javax.swing.text;

import java.awt.Graphics;
import java.awt.Shape;

/**
 * A highlighter that paints <em>underneath</em> the text, view by view.
 *
 * <p>The difference from plain {@link Highlighter} is the order: the one above paints everything
 * after the text, so an opaque background would cover it; this one gets into each view's drawing
 * and paints before. It is what allows the selection to have a solid background and the text to
 * go on being readable.
 */
public abstract class LayeredHighlighter implements Highlighter {

    protected LayeredHighlighter() {
    }

    /** It paints that stretch's highlights inside that view, before its text. */
    public abstract void paintLayeredHighlights(Graphics g, int p0, int p1, Shape viewBounds,
            JTextComponent editor, View view);

    /**
     * A painter that works by layers.
     *
     * <p>It returns the region it painted, which is what the component uses to know what to repaint
     * afterwards; an ordinary painter does not know it.
     */
    public abstract static class LayerPainter implements Highlighter.HighlightPainter {

        protected LayerPainter() {
        }

        public abstract Shape paintLayer(Graphics g, int p0, int p1, Shape viewBounds,
                JTextComponent editor, View view);
    }
}
