package javax.swing.plaf;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.text.BadLocationException;
import javax.swing.text.EditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.swing.text.View;

/**
 * A text component's look and feel: the bridge between the component and its view tree.
 *
 * <p>It is the Swing UI that adds the most methods, and for a reason: the component knows
 * nothing about how its text looks. Where a position falls, which position is at a point, what
 * has to be repainted when a stretch changes, all of that is known by the view tree, and the UI
 * is the one that has it.
 *
 * <p>The pairs {@code modelToView}/{@code modelToView2D} and
 * {@code viewToModel}/{@code viewToModel2D} are the same thing with integer or fractional
 * coordinates; the fractional ones came later, for screens where a logical pixel is not a
 * physical one.
 */
public abstract class TextUI extends ComponentUI {

    protected TextUI() {
    }

    /** @deprecated it is {@link #modelToView2D}. */
    @Deprecated
    public abstract Rectangle modelToView(JTextComponent t, int pos) throws BadLocationException;

    /** @deprecated it is {@link #modelToView2D}. */
    @Deprecated
    public abstract Rectangle modelToView(JTextComponent t, int pos, Position.Bias bias)
            throws BadLocationException;

    /** Where that document position falls, in the component's coordinates. */
    public Rectangle2D modelToView2D(JTextComponent t, int pos, Position.Bias bias)
            throws BadLocationException {
        return modelToView(t, pos, bias);
    }

    /** @deprecated it is {@link #viewToModel2D}. */
    @Deprecated
    public abstract int viewToModel(JTextComponent t, Point pt);

    /** @deprecated it is {@link #viewToModel2D}. */
    @Deprecated
    public abstract int viewToModel(JTextComponent t, Point pt, Position.Bias[] biasReturn);

    /** Which document position is at that point. */
    public int viewToModel2D(JTextComponent t, Point2D pt, Position.Bias[] biasReturn) {
        return viewToModel(t, new Point((int) pt.getX(), (int) pt.getY()), biasReturn);
    }

    /** Where the cursor goes from that position in that direction. */
    public abstract int getNextVisualPositionFrom(JTextComponent t, int pos, Position.Bias b,
            int direction, Position.Bias[] biasRet) throws BadLocationException;

    /** Marks that stretch as needing a repaint. */
    public abstract void damageRange(JTextComponent t, int p0, int p1);

    public abstract void damageRange(JTextComponent t, int p0, int p1, Position.Bias firstBias,
            Position.Bias secondBias);

    /** The editor kit the component uses. */
    public abstract EditorKit getEditorKit(JTextComponent t);

    /** The root of the view tree. */
    public abstract View getRootView(JTextComponent t);

    /** @deprecated it is {@link #getToolTipText2D}. */
    @Deprecated
    public String getToolTipText(JTextComponent t, Point pt) {
        return null;
    }

    /** The tooltip text at that point, if the content has one. */
    public String getToolTipText2D(JTextComponent t, Point2D pt) {
        return getToolTipText(t, new Point((int) pt.getX(), (int) pt.getY()));
    }
}
