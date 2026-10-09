package javax.swing.plaf.multi;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Vector;
import javax.accessibility.Accessible;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.TextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.EditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.swing.text.View;

/**
 * The Text look and feel that shares out every call among several.
 *
 * <h2>What it is</h2>
 *
 * <p>It keeps a list of Text looks and feels and passes every operation to them all. What it
 * returns is what the first one answered, which is the main look and feel's; the rest
 * hear about it all the same, and that is the point.
 *
 * <h2>What having several is for</h2>
 *
 * <p>For hanging on a look and feel another one that does not draw: a screen reader, a logger of
 * what the user does, a help system that follows the focus. Those observers need the same calls as
 * the real look and feel --to install themselves, to hear about every drawing-- and have no reason
 * to know that there is another.
 *
 * <p>Without this mechanism each look and feel would have to be wrapped by hand. With it, they are
 * listed in a property and {@link MultiLookAndFeel} builds the list.
 *
 * <h2>Why the first one rules</h2>
 *
 * <p>A method returns a single value and there are several answers. Choosing the first --and not
 * combining them nor keeping the last-- is what keeps the main look and feel in charge: the
 * auxiliaries watch, they do not decide.
 */
public class MultiTextUI extends TextUI {

    /**
     * The looks and feels that are handled, in order.
     *
     * <p>The first one is the main one. The order is fixed by {@link MultiLookAndFeel#createUIs}
     * and is not a detail: it is what decides who answers.
     */
    protected Vector<ComponentUI> uis = new Vector<ComponentUI>();

    /** One with no look and feel; {@link #createUI} adds them. */
    public MultiTextUI() {
    }

    /**
     * The looks and feels that are handled.
     *
     * @return a new array, with the main one first
     */
    public ComponentUI[] getUIs() {
        return MultiLookAndFeel.uisToArray(uis);
    }

    /**
     * The look and feel for that component.
     *
     * <p>It returns one of these only if there is more than one look and feel configured. With a
     * single one it returns that one, without wrapping it: sharing out among one is not needed and
     * would cost one extra call per operation.
     *
     * @param a the component
     * @return the look and feel
     */
    public static ComponentUI createUI(JComponent a) {
        MultiTextUI mui = new MultiTextUI();
        return MultiLookAndFeel.createUIs(mui, mui.uis, a);
    }


    /**
     * It asks them all and answers with the first.
     *
     * @param jTextComponent the {@code JTextComponent}
     * @param point the {@code Point}
     * @return whatever the first one answered
     */
    public String getToolTipText(JTextComponent jTextComponent, Point point) {
        String returnValue = ((TextUI) uis.elementAt(0)).getToolTipText(jTextComponent, point);
        for (int i = 1; i < uis.size(); i++) {
            ((TextUI) uis.elementAt(i)).getToolTipText(jTextComponent, point);
        }
        return returnValue;
    }

    /**
     * It asks them all and answers with the first.
     *
     * @param jTextComponent the {@code JTextComponent}
     * @param i2 the {@code int}
     * @return whatever the first one answered
     * @throws BadLocationException if the position is not valid
     * @deprecated it is {@link #modelToView2D}
     */
    @Deprecated
    public Rectangle modelToView(JTextComponent jTextComponent, int i2)
            throws BadLocationException {
        Rectangle returnValue = ((TextUI) uis.elementAt(0)).modelToView(jTextComponent, i2);
        for (int i = 1; i < uis.size(); i++) {
            ((TextUI) uis.elementAt(i)).modelToView(jTextComponent, i2);
        }
        return returnValue;
    }

    /**
     * It asks them all and answers with the first.
     *
     * @param jTextComponent the {@code JTextComponent}
     * @param i2 the {@code int}
     * @param bias the {@code Position.Bias}
     * @return whatever the first one answered
     * @throws BadLocationException if the position is not valid
     * @deprecated it is {@link #modelToView2D}
     */
    @Deprecated
    public Rectangle modelToView(JTextComponent jTextComponent, int i2, Position.Bias bias)
            throws BadLocationException {
        Rectangle returnValue = ((TextUI) uis.elementAt(0)).modelToView(jTextComponent, i2, bias);
        for (int i = 1; i < uis.size(); i++) {
            ((TextUI) uis.elementAt(i)).modelToView(jTextComponent, i2, bias);
        }
        return returnValue;
    }

    /**
     * It asks them all and answers with the first.
     *
     * @param jTextComponent the {@code JTextComponent}
     * @param i2 the {@code int}
     * @param bias the {@code Position.Bias}
     * @return whatever the first one answered
     * @throws BadLocationException if the position is not valid
     */
    public Rectangle2D modelToView2D(JTextComponent jTextComponent, int i2, Position.Bias bias)
            throws BadLocationException {
        Rectangle2D returnValue = ((TextUI) uis.elementAt(0)).modelToView2D(jTextComponent, i2, bias);
        for (int i = 1; i < uis.size(); i++) {
            ((TextUI) uis.elementAt(i)).modelToView2D(jTextComponent, i2, bias);
        }
        return returnValue;
    }

    /**
     * It asks them all and answers with the first.
     *
     * @param jTextComponent the {@code JTextComponent}
     * @param i2 the {@code int}
     * @param bias the {@code Position.Bias}
     * @param i3 the {@code int}
     * @param biass the {@code Position.Bias[]}
     * @return whatever the first one answered
     * @throws BadLocationException if the position is not valid
     */
    public int getNextVisualPositionFrom(
            JTextComponent jTextComponent, int i2, Position.Bias bias, int i3,
            Position.Bias[] biass) throws BadLocationException {
        int returnValue =
                ((TextUI) uis.elementAt(0)).getNextVisualPositionFrom(jTextComponent, i2, bias, i3, biass);
        for (int i = 1; i < uis.size(); i++) {
            ((TextUI) uis.elementAt(i)).getNextVisualPositionFrom(jTextComponent, i2, bias, i3, biass);
        }
        return returnValue;
    }

    /**
     * It asks them all and answers with the first.
     *
     * @param jTextComponent the {@code JTextComponent}
     * @param point the {@code Point}
     * @return whatever the first one answered
     */
    public int viewToModel(JTextComponent jTextComponent, Point point) {
        int returnValue = ((TextUI) uis.elementAt(0)).viewToModel(jTextComponent, point);
        for (int i = 1; i < uis.size(); i++) {
            ((TextUI) uis.elementAt(i)).viewToModel(jTextComponent, point);
        }
        return returnValue;
    }

    /**
     * It asks them all and answers with the first.
     *
     * @param jTextComponent the {@code JTextComponent}
     * @param point the {@code Point}
     * @param biass the {@code Position.Bias[]}
     * @return whatever the first one answered
     */
    public int viewToModel(JTextComponent jTextComponent, Point point, Position.Bias[] biass) {
        int returnValue = ((TextUI) uis.elementAt(0)).viewToModel(jTextComponent, point, biass);
        for (int i = 1; i < uis.size(); i++) {
            ((TextUI) uis.elementAt(i)).viewToModel(jTextComponent, point, biass);
        }
        return returnValue;
    }

    /**
     * It asks them all and answers with the first.
     *
     * @param jTextComponent the {@code JTextComponent}
     * @param point2D the {@code Point2D}
     * @param biass the {@code Position.Bias[]}
     * @return whatever the first one answered
     */
    public int viewToModel2D(
            JTextComponent jTextComponent, Point2D point2D, Position.Bias[] biass) {
        int returnValue = ((TextUI) uis.elementAt(0)).viewToModel2D(jTextComponent, point2D, biass);
        for (int i = 1; i < uis.size(); i++) {
            ((TextUI) uis.elementAt(i)).viewToModel2D(jTextComponent, point2D, biass);
        }
        return returnValue;
    }

    /**
     * It asks them all and answers with the first.
     *
     * @param jTextComponent the {@code JTextComponent}
     * @param i2 the {@code int}
     * @param i3 the {@code int}
     */
    public void damageRange(JTextComponent jTextComponent, int i2, int i3) {
        for (int i = 0; i < uis.size(); i++) {
            ((TextUI) uis.elementAt(i)).damageRange(jTextComponent, i2, i3);
        }
    }

    /**
     * It asks them all and answers with the first.
     *
     * @param jTextComponent the {@code JTextComponent}
     * @param i2 the {@code int}
     * @param i3 the {@code int}
     * @param bias the {@code Position.Bias}
     * @param bias2 the {@code Position.Bias}
     */
    public void damageRange(
            JTextComponent jTextComponent, int i2, int i3, Position.Bias bias, Position.Bias bias2) {
        for (int i = 0; i < uis.size(); i++) {
            ((TextUI) uis.elementAt(i)).damageRange(jTextComponent, i2, i3, bias, bias2);
        }
    }

    /**
     * It asks them all and answers with the first.
     *
     * @param jTextComponent the {@code JTextComponent}
     * @return whatever the first one answered
     */
    public EditorKit getEditorKit(JTextComponent jTextComponent) {
        EditorKit returnValue = ((TextUI) uis.elementAt(0)).getEditorKit(jTextComponent);
        for (int i = 1; i < uis.size(); i++) {
            ((TextUI) uis.elementAt(i)).getEditorKit(jTextComponent);
        }
        return returnValue;
    }

    /**
     * It asks them all and answers with the first.
     *
     * @param jTextComponent the {@code JTextComponent}
     * @return whatever the first one answered
     */
    public View getRootView(JTextComponent jTextComponent) {
        View returnValue = ((TextUI) uis.elementAt(0)).getRootView(jTextComponent);
        for (int i = 1; i < uis.size(); i++) {
            ((TextUI) uis.elementAt(i)).getRootView(jTextComponent);
        }
        return returnValue;
    }

    /**
     * Whether the point falls inside the component.
     *
     * @param jComponent the {@code JComponent}
     * @param i2 the {@code int}
     * @param i3 the {@code int}
     * @return whatever the first one answered
     */
    public boolean contains(JComponent jComponent, int i2, int i3) {
        boolean returnValue = ((TextUI) uis.elementAt(0)).contains(jComponent, i2, i3);
        for (int i = 1; i < uis.size(); i++) {
            ((TextUI) uis.elementAt(i)).contains(jComponent, i2, i3);
        }
        return returnValue;
    }

    /**
     * It redraws the background and then the component.
     *
     * @param graphics the {@code Graphics}
     * @param jComponent the {@code JComponent}
     */
    public void update(Graphics graphics, JComponent jComponent) {
        for (int i = 0; i < uis.size(); i++) {
            ((TextUI) uis.elementAt(i)).update(graphics, jComponent);
        }
    }

    /**
     * It installs itself on the component.
     *
     * @param jComponent the {@code JComponent}
     */
    public void installUI(JComponent jComponent) {
        for (int i = 0; i < uis.size(); i++) {
            ((TextUI) uis.elementAt(i)).installUI(jComponent);
        }
    }

    /**
     * It uninstalls itself from the component.
     *
     * @param jComponent the {@code JComponent}
     */
    public void uninstallUI(JComponent jComponent) {
        for (int i = 0; i < uis.size(); i++) {
            ((TextUI) uis.elementAt(i)).uninstallUI(jComponent);
        }
    }

    /**
     * It draws the component.
     *
     * @param graphics the {@code Graphics}
     * @param jComponent the {@code JComponent}
     */
    public void paint(Graphics graphics, JComponent jComponent) {
        for (int i = 0; i < uis.size(); i++) {
            ((TextUI) uis.elementAt(i)).paint(graphics, jComponent);
        }
    }

    /**
     * The size it would prefer to have.
     *
     * @param jComponent the {@code JComponent}
     * @return whatever the first one answered
     */
    public Dimension getPreferredSize(JComponent jComponent) {
        Dimension returnValue = ((TextUI) uis.elementAt(0)).getPreferredSize(jComponent);
        for (int i = 1; i < uis.size(); i++) {
            ((TextUI) uis.elementAt(i)).getPreferredSize(jComponent);
        }
        return returnValue;
    }

    /**
     * The smallest size it can manage with.
     *
     * @param jComponent the {@code JComponent}
     * @return whatever the first one answered
     */
    public Dimension getMinimumSize(JComponent jComponent) {
        Dimension returnValue = ((TextUI) uis.elementAt(0)).getMinimumSize(jComponent);
        for (int i = 1; i < uis.size(); i++) {
            ((TextUI) uis.elementAt(i)).getMinimumSize(jComponent);
        }
        return returnValue;
    }

    /**
     * The largest size it accepts.
     *
     * @param jComponent the {@code JComponent}
     * @return whatever the first one answered
     */
    public Dimension getMaximumSize(JComponent jComponent) {
        Dimension returnValue = ((TextUI) uis.elementAt(0)).getMaximumSize(jComponent);
        for (int i = 1; i < uis.size(); i++) {
            ((TextUI) uis.elementAt(i)).getMaximumSize(jComponent);
        }
        return returnValue;
    }

    /**
     * How many accessible children it has.
     *
     * @param jComponent the {@code JComponent}
     * @return whatever the first one answered
     */
    public int getAccessibleChildrenCount(JComponent jComponent) {
        int returnValue = ((TextUI) uis.elementAt(0)).getAccessibleChildrenCount(jComponent);
        for (int i = 1; i < uis.size(); i++) {
            ((TextUI) uis.elementAt(i)).getAccessibleChildrenCount(jComponent);
        }
        return returnValue;
    }

    /**
     * The accessible child at that position.
     *
     * @param jComponent the {@code JComponent}
     * @param i2 the {@code int}
     * @return whatever the first one answered
     */
    public Accessible getAccessibleChild(JComponent jComponent, int i2) {
        Accessible returnValue = ((TextUI) uis.elementAt(0)).getAccessibleChild(jComponent, i2);
        for (int i = 1; i < uis.size(); i++) {
            ((TextUI) uis.elementAt(i)).getAccessibleChild(jComponent, i2);
        }
        return returnValue;
    }
}
