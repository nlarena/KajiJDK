package javax.swing.plaf.basic;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ButtonModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.ComponentInputMapUIResource;
import javax.swing.plaf.RootPaneUI;

/**
 * The basic look and feel of a root pane: it draws nothing, and ties Enter to the default
 * button.
 *
 * <h2>The only thing it does is keyboard</h2>
 *
 * <p>A root pane has no look: it is the layer that holds the menu, the content and the glass,
 * and what is seen are its children. So this look and feel does not install a single colour.
 * What it does install is the shortcut that makes Enter press the frame's default button -- the
 * one marked with a different border in a dialog --, which is the only thing the root pane has
 * to know how to do.
 *
 * <h2>The empty map that is not useless</h2>
 *
 * <p>The key map is installed <em>always</em>, but it starts out empty: the four combinations
 * -- Enter and ctrl-Enter, pressed and released -- are added when the frame has a default
 * button and removed when it stops having one. {@link #propertyChange} is the one that notices
 * it. It is measured: a newly created root pane has the map set and with no key in it.
 *
 * <p>The map is of the class {@code RootPaneInputMap}, which contributes nothing save being
 * recognizable: {@link #updateDefaultButtonBindings} goes up the chain of parents until it
 * finds it, and that way it knows which of all the maps is its own and which the user set.
 */
public class BasicRootPaneUI extends RootPaneUI implements PropertyChangeListener {

    private static RootPaneUI rootPaneUI = new BasicRootPaneUI();

    /** The four combinations; see the class note. */
    private static final Object[] DEFAULT_BUTTON_BINDINGS = {
        "ENTER", "press",
        "released ENTER", "release",
        "ctrl ENTER", "press",
        "ctrl released ENTER", "release",
    };

    public BasicRootPaneUI() {
    }

    /** The shared look and feel: it keeps nothing of the pane. */
    public static ComponentUI createUI(JComponent c) {
        return rootPaneUI;
    }

    public void installUI(JComponent c) {
        installDefaults((JRootPane) c);
        installComponents((JRootPane) c);
        installListeners((JRootPane) c);
        installKeyboardActions((JRootPane) c);
    }

    public void uninstallUI(JComponent c) {
        uninstallDefaults((JRootPane) c);
        uninstallComponents((JRootPane) c);
        uninstallListeners((JRootPane) c);
        uninstallKeyboardActions((JRootPane) c);
    }

    /** Nothing: see the class note. */
    protected void installDefaults(JRootPane c) {
    }

    /** Nothing. */
    protected void uninstallDefaults(JRootPane c) {
    }

    /** Nothing: the children are set by {@link JRootPane} in its constructor. */
    protected void installComponents(JRootPane root) {
    }

    /** Nothing. */
    protected void uninstallComponents(JRootPane root) {
    }

    /** It listens to the change of default button and to the ancestor one. */
    protected void installListeners(JRootPane root) {
        root.addPropertyChangeListener(this);
    }

    protected void uninstallListeners(JRootPane root) {
        root.removePropertyChangeListener(this);
    }

    /** The key map and the three actions; see the class note. */
    protected void installKeyboardActions(JRootPane root) {
        InputMap km = new RootPaneInputMap(root);
        SwingUtilities.replaceUIInputMap(root, JComponent.WHEN_IN_FOCUSED_WINDOW, km);
        ActionMap am = createActionMap();
        SwingUtilities.replaceUIActionMap(root, am);
        updateDefaultButtonBindings(root);
    }

    protected void uninstallKeyboardActions(JRootPane root) {
        SwingUtilities.replaceUIInputMap(root, JComponent.WHEN_IN_FOCUSED_WINDOW, null);
        SwingUtilities.replaceUIActionMap(root, null);
    }

    private ActionMap createActionMap() {
        ActionMap map = new ActionMapUIResource();
        map.put("press", new DefaultButtonAction(true));
        map.put("release", new DefaultButtonAction(false));
        map.put("postPopup", new PopupMenuAction());
        return map;
    }

    /**
     * It puts in or takes out the four combinations according to whether there is a default
     * button.
     *
     * <p>It goes up the chain of parents as far as its own map; see the class note.
     */
    private void updateDefaultButtonBindings(JRootPane root) {
        InputMap km = SwingUtilities.getUIInputMap(root, JComponent.WHEN_IN_FOCUSED_WINDOW);
        while (km != null && !(km instanceof RootPaneInputMap)) {
            km = km.getParent();
        }
        if (km != null) {
            km.clear();
            if (root.getDefaultButton() != null) {
                LookAndFeel.loadKeyBindings(km, DEFAULT_BUTTON_BINDINGS);
            }
        }
    }

    /** It rebuilds the shortcuts when the default button changes. */
    public void propertyChange(PropertyChangeEvent e) {
        if (e.getPropertyName().equals("defaultButton")) {
            JRootPane rootpane = (JRootPane) e.getSource();
            updateDefaultButtonBindings(rootpane);
            if (rootpane.getClientProperty("temporaryDefaultButton") == null) {
                rootpane.repaint();
            }
        }
    }

    /** The recognizable map; see the class note. */
    static class RootPaneInputMap extends ComponentInputMapUIResource {

        public RootPaneInputMap(JComponent c) {
            super(c);
        }
    }

    /**
     * It presses or releases the default button.
     *
     * <p>It presses the model instead of calling {@code doClick}: that way the button looks sunken
     * while the key is down, which is what a real button does.
     */
    private static class DefaultButtonAction extends AbstractAction {

        private final boolean press;

        DefaultButtonAction(boolean press) {
            this.press = press;
        }

        public void actionPerformed(ActionEvent e) {
            JRootPane root = (JRootPane) e.getSource();
            JButton owner = root.getDefaultButton();
            if (owner != null && SwingUtilities.getRootPane(owner) == root) {
                ButtonModel model = owner.getModel();
                if (press) {
                    model.setArmed(true);
                    model.setPressed(true);
                } else {
                    model.setPressed(false);
                    model.setArmed(false);
                }
            }
        }

        public boolean isEnabled() {
            return true;
        }
    }

    /** It shows the context menu of the component that has the focus, if it has one. */
    private static class PopupMenuAction extends AbstractAction {

        public void actionPerformed(ActionEvent e) {
            JRootPane root = (JRootPane) e.getSource();
            java.awt.Component focus = java.awt.KeyboardFocusManager
                    .getCurrentKeyboardFocusManager().getFocusOwner();
            if (!(focus instanceof JComponent)) {
                return;
            }
            JPopupMenu menu = ((JComponent) focus).getComponentPopupMenu();
            if (menu != null) {
                java.awt.Point p = ((JComponent) focus).getPopupLocation(null);
                if (p == null) {
                    p = new java.awt.Point(focus.getWidth() / 2, focus.getHeight() / 2);
                }
                menu.show(focus, p.x, p.y);
            }
        }
    }
}
