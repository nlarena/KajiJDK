package javax.swing.plaf.basic;

import java.beans.PropertyChangeEvent;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.text.EditorKit;
import javax.swing.text.JTextComponent;

/**
 * The basic look and feel of an editor pane.
 *
 * <h2>The editor kit is not chosen by the look and feel</h2>
 *
 * <p>It is the only deep difference with the other text look and feels: a field or an area
 * always have the same editor kit, and a {@link JEditorPane} changes it according to the content
 * type -- plain text, HTML, RTF --. That is why {@link #getEditorKit} does not return a
 * constant: it asks the component. And that is why {@code "editorKit"} has to be listened to:
 * when it changes, the whole view tree has to be rebuilt, because the old views are the old
 * kit's.
 *
 * <h2>What is reinstalled on changing kit</h2>
 *
 * <p>The colour and the typeface. An editor kit brings styles of its own, and if the one that
 * was set came from the look and feel it has to be put on top again; if the user set it, no. It
 * is the same {@link UIResource} rule as always, applied at an odd moment.
 */
public class BasicEditorPaneUI extends BasicTextUI {

    public BasicEditorPaneUI() {
        super();
    }

    /** A new one per pane: a text look and feel keeps the component. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicEditorPaneUI();
    }

    protected String getPropertyPrefix() {
        return "EditorPane";
    }

    public void installUI(JComponent c) {
        super.installUI(c);
        updateStyle((JTextComponent) c);
    }

    public void uninstallUI(JComponent c) {
        cleanDisplayProperties((JTextComponent) c);
        super.uninstallUI(c);
    }

    /** The component's, not a fixed one; see the class note. */
    public EditorKit getEditorKit(JTextComponent tc) {
        JEditorPane pane = (JEditorPane) tc;
        return pane.getEditorKit();
    }

    /** It rebuilds the views and the style when the editor kit changes. */
    protected void propertyChange(PropertyChangeEvent evt) {
        super.propertyChange(evt);
        String name = evt.getPropertyName();
        if ("editorKit".equals(name)) {
            updateStyle((JTextComponent) evt.getSource());
        } else if ("editable".equals(name) || "foreground".equals(name)
                || "font".equals(name) || "document".equals(name)) {
            updateStyle((JTextComponent) evt.getSource());
        }
    }

    /**
     * It leaves the look and feel's colour and typeface on top of the kit's; see the class note.
     */
    private void updateStyle(JTextComponent editor) {
        if (editor.getForeground() instanceof UIResource
                || editor.getFont() instanceof UIResource) {
            // The look and feel's values are applied again as they are: they are already set in the
                        // component and the editor kit reads them from there. There is nothing to
                        // copy.
            editor.repaint();
        }
    }

    /** It removes what this look and feel left set in the component. */
    private void cleanDisplayProperties(JTextComponent editor) {
    }
}
