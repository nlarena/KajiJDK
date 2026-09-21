package javax.swing.plaf.basic;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * The listener that turns mouse, focus and property changes into changes of the button's
 * model.
 *
 * <p>It is the "input" half of the basic look and feel: {@code BasicButtonUI} paints what the
 * model says, and this object writes into the model what the mouse does. It never paints nor
 * decides what is seen; it only arms, presses, releases and disarms, and the model does the
 * rest, the action included.
 *
 * <h2>What is not there</h2>
 *
 * <p>The keyboard actions -- space presses, the mnemonic with Alt -- live in
 * {@code InputMap}/{@code ActionMap}, which are not there; {@link #installKeyboardActions},
 * {@link #uninstallKeyboardActions} and {@link #updateMnemonicBinding} have nowhere to register
 * them, and {@code getInputMap} is not there because there is nothing to return. Nor does the
 * focus touch the dialog's default button, because there is no {@code JRootPane}.
 */
public class BasicButtonListener implements MouseListener, MouseMotionListener, FocusListener,
        ChangeListener, PropertyChangeListener {

    private long lastPress = -1;
    private boolean discardDrop = false;

    public BasicButtonListener(AbstractButton b) {
    }

    /** A property of the button changed: it only matters if it stopped filling its area. */
    public void propertyChange(PropertyChangeEvent e) {
        String property = e.getPropertyName();
        if (AbstractButton.CONTENT_AREA_FILLED_CHANGED_PROPERTY.equals(property)) {
            checkOpacity((AbstractButton) e.getSource());
        } else if (AbstractButton.MNEMONIC_CHANGED_PROPERTY.equals(property)) {
            updateMnemonicBinding((AbstractButton) e.getSource());
        }
    }

    /**
     * A button that fills its area is opaque, and one that does not, is not: the two properties go
     * together.
     */
    protected void checkOpacity(AbstractButton b) {
        b.setOpaque(b.isContentAreaFilled());
    }

    /** Nothing to install; see the class note. */
    public void installKeyboardActions(JComponent c) {
    }

    public void uninstallKeyboardActions(JComponent c) {
    }

    /** Nothing to renew; see the class note. */
    public void updateMnemonicBinding(AbstractButton b) {
    }

    /** The model changed: the button repaints itself. */
    public void stateChanged(ChangeEvent e) {
        AbstractButton b = (AbstractButton) e.getSource();
        b.repaint();
    }

    public void focusGained(FocusEvent e) {
        AbstractButton b = (AbstractButton) e.getSource();
        b.repaint();
    }

    /** Losing the focus releases and disarms: a button with no focus cannot stay half pressed. */
    public void focusLost(FocusEvent e) {
        AbstractButton b = (AbstractButton) e.getSource();
        ButtonModel model = b.getModel();
        model.setPressed(false);
        model.setArmed(false);
        b.repaint();
    }

    public void mouseMoved(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
    }

    /**
     * The left button went down: it arms and presses, and asks for the focus.
     *
     * <p>Two presses closer together than the button's threshold count as one: the second is
     * ignored, and so is the release that follows it. It is the defence against an accidental
     * double click on a button that does something irreversible.
     */
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            AbstractButton b = (AbstractButton) e.getSource();
            if (b.contains(e.getX(), e.getY())) {
                long threshold = b.getMultiClickThreshhold();
                long previous = lastPress;
                long now = e.getWhen();
                lastPress = now;
                if (previous != -1 && now - previous < threshold) {
                    discardDrop = true;
                    return;
                }
                ButtonModel model = b.getModel();
                if (!model.isEnabled()) {
                    return;
                }
                if (!model.isArmed()) {
                    model.setArmed(true);
                }
                model.setPressed(true);
                if (!b.hasFocus() && b.isRequestFocusEnabled()) {
                    b.requestFocus();
                }
            }
        }
    }

    /**
     * The left button was released: it releases and disarms; if it was still armed, the model fires
     * the action.
     */
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            if (discardDrop) {
                discardDrop = false;
                return;
            }
            AbstractButton b = (AbstractButton) e.getSource();
            ButtonModel model = b.getModel();
            model.setPressed(false);
            model.setArmed(false);
        }
    }

    /** The cursor came in: rollover if the button shows it, and rearm if it came in pressed. */
    public void mouseEntered(MouseEvent e) {
        AbstractButton b = (AbstractButton) e.getSource();
        ButtonModel model = b.getModel();
        if (b.isRolloverEnabled() && !SwingUtilities.isLeftMouseButton(e)) {
            model.setRollover(true);
        }
        if (model.isPressed()) {
            model.setArmed(true);
        }
    }

    /** The cursor left: it disarms; releasing outside no longer fires anything. */
    public void mouseExited(MouseEvent e) {
        AbstractButton b = (AbstractButton) e.getSource();
        ButtonModel model = b.getModel();
        if (b.isRolloverEnabled()) {
            model.setRollover(false);
        }
        model.setArmed(false);
    }
}
