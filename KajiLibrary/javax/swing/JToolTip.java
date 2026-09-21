package javax.swing;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.ToolTipUI;

/**
 * The little tip that appears when the pointer is left still.
 *
 * <h2>Almost nobody builds it</h2>
 *
 * <p>The usual thing is {@code component.setToolTipText("...")} and that is that: the
 * {@code ToolTipManager} builds the tip, shows it and hides it. This class exists for the case
 * in which something else is needed -- a tip with several lines, with an icon, with a border of
 * its own --, and then it is extended and the subclass is returned from
 * {@code createToolTip()}.
 *
 * <h2>It keeps who it describes</h2>
 *
 * <p>{@link #setComponent} tells it which component this tip belongs to. It is not decoration:
 * the look and feel uses it in order to borrow its typeface and colours, so that the tip looks
 * like what it describes.
 */
public class JToolTip extends JComponent implements Accessible {

    private static final String uiClassID = "ToolTipUI";

    String tipText;
    JComponent component;

    /** An empty tip. */
    public JToolTip() {
        setOpaque(true);
        updateUI();
    }

    public ToolTipUI getUI() {
        return (ToolTipUI) ui;
    }

    public void updateUI() {
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** The text; changing it measures it again, because the tip fits what it says. */
    public void setTipText(String tipText) {
        String oldValue = this.tipText;
        this.tipText = tipText;
        firePropertyChange("tiptext", oldValue, tipText);
        if (tipText == null ? oldValue != null : !tipText.equals(oldValue)) {
            revalidate();
            repaint();
        }
    }

    public String getTipText() {
        return tipText;
    }

    /** Which component this tip belongs to; see the class note. */
    public void setComponent(JComponent c) {
        JComponent oldValue = this.component;
        component = c;
        firePropertyChange("component", oldValue, c);
    }

    public JComponent getComponent() {
        return component;
    }

    /** Always true: a tool tip covered by what it describes would be of no use. */
    boolean alwaysOnTop() {
        return true;
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
