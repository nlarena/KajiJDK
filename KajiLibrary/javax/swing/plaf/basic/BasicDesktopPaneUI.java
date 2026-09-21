package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.DefaultDesktopManager;
import javax.swing.DesktopManager;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.DesktopPaneUI;
import javax.swing.plaf.UIResource;

/**
 * The basic look and feel of a desktop of internal frames.
 *
 * <h2>It draws nothing, and what it does is not little</h2>
 *
 * <p>What is seen of a desktop are its internal frames; the desktop itself is a plain
 * background. What this look and feel contributes are two things that are not seen: the
 * {@link DesktopManager} -- who decides what happens on moving, maximizing, iconifying or
 * closing a frame -- and the table of keyboard actions, which are nineteen.
 *
 * <h2>Nineteen actions and not one key</h2>
 *
 * <p>The actions are all there -- moving between frames, moving and resizing with the arrows,
 * closing, minimizing, maximizing, restoring -- and the key map is <em>empty</em>. It is not an
 * oversight: which key fires which action is decided by the look and feel's table, and with no
 * table there is none tied. It is measured: the JDK does not have a single key in the desktop's
 * map either.
 *
 * <h2>The five key fields that were left null</h2>
 *
 * <p>{@link #closeKey}, {@link #navigateKey}, {@link #navigateKey2}, {@link #minimizeKey} and
 * {@link #maximizeKey} are protected, they exist, and nobody writes them. They come from when
 * the look and feel tied the keys by hand; now they are tied by the table and they stayed for
 * compatibility, just like {@code shadow} and {@code highlight} in {@link BasicSeparatorUI}.
 * Measured: all five null.
 *
 * <h2>No preferred size</h2>
 *
 * <p>{@link #getPreferredSize} returns {@code null}. A desktop takes up whatever it is given:
 * there is no size it "prefers", and answering the union of its frames would be worse -- the
 * desktop grows every time somebody drags a frame towards the edge.
 */
public class BasicDesktopPaneUI extends DesktopPaneUI {

    protected JDesktopPane desktop;
    protected DesktopManager desktopManager;

    /** Unused; see the class note. */
    @Deprecated
    protected KeyStroke minimizeKey;

    /** Unused; see the class note. */
    @Deprecated
    protected KeyStroke maximizeKey;

    /** Unused; see the class note. */
    @Deprecated
    protected KeyStroke closeKey;

    /** Unused; see the class note. */
    @Deprecated
    protected KeyStroke navigateKey;

    /** Unused; see the class note. */
    @Deprecated
    protected KeyStroke navigateKey2;

    private PropertyChangeListener pcl;

    private static final ColorUIResource BACKGROUND = new ColorUIResource(255, 255, 255);

    public BasicDesktopPaneUI() {
    }

    /** A new one per desktop: it keeps the component and its manager. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicDesktopPaneUI();
    }

    public void installUI(JComponent c) {
        desktop = (JDesktopPane) c;
        installDefaults();
        installDesktopManager();
        installListeners();
        installKeyboardActions();
    }

    public void uninstallUI(JComponent c) {
        uninstallKeyboardActions();
        uninstallListeners();
        uninstallDesktopManager();
        uninstallDefaults();
        desktop = null;
    }

    /** Background and opacity; the value is that of {@code Desktop.background} in Metal. */
    protected void installDefaults() {
        Color background = desktop.getBackground();
        if (background == null || background instanceof UIResource) {
            desktop.setBackground(BACKGROUND);
        }
        LookAndFeel.installProperty(desktop, "opaque", Boolean.TRUE);
    }

    /** It removes nothing; see {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults() {
    }

    /** It sets the manager, unless the program has set its own. */
    protected void installDesktopManager() {
        desktopManager = desktop.getDesktopManager();
        if (desktopManager == null) {
            desktopManager = new BasicDesktopManager();
            desktop.setDesktopManager(desktopManager);
        }
    }

    /** And it removes it only if it is the one this look and feel set. */
    protected void uninstallDesktopManager() {
        if (desktop.getDesktopManager() instanceof UIResource) {
            desktop.setDesktopManager(null);
        }
        desktopManager = null;
    }

    protected void installListeners() {
        pcl = createPropertyChangeListener();
        desktop.addPropertyChangeListener(pcl);
    }

    protected void uninstallListeners() {
        desktop.removePropertyChangeListener(pcl);
        pcl = null;
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new Handler();
    }

    /** The nineteen actions; see the class note. */
    protected void installKeyboardActions() {
        registerKeyboardActions();
        SwingUtilities.replaceUIActionMap(desktop, createActionMap());
    }

    protected void uninstallKeyboardActions() {
        unregisterKeyboardActions();
        SwingUtilities.replaceUIActionMap(desktop, null);
    }

    /** The hook for a subclass to tie keys of its own; the basic one ties none. */
    protected void registerKeyboardActions() {
    }

    protected void unregisterKeyboardActions() {
    }

    private ActionMap createActionMap() {
        ActionMap map = new ActionMapUIResource();
        String[] names = {
            "restore", "close", "move", "resize",
            "right", "shrinkRight", "left", "shrinkLeft",
            "up", "shrinkUp", "down", "shrinkDown",
            "escape", "minimize", "maximize",
            "selectNextFrame", "selectPreviousFrame",
            "navigateNext", "navigatePrevious",
        };
        for (int i = 0; i < names.length; i++) {
            map.put(names[i], new DesktopAction(names[i]));
        }
        return map;
    }

    /**
     * Nothing: the background is filled by {@code ComponentUI.update} and the frames paint
     * themselves.
     */
    public void paint(Graphics g, JComponent c) {
    }

    /** {@code null}; see the class note. */
    public Dimension getPreferredSize(JComponent c) {
        return null;
    }

    /** Zero: a desktop can be shrunk until it disappears. */
    public Dimension getMinimumSize(JComponent c) {
        return new Dimension(0, 0);
    }

    /** No cap. */
    public Dimension getMaximumSize(JComponent c) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /**
     * The manager this look and feel sets.
     *
     * <p>It is a {@link DefaultDesktopManager} marked as the look and feel's; the mark is the only
     * thing it adds, and it is what lets {@link #uninstallDesktopManager} know whether it was set
     * by it or by the program.
     */
    private static class BasicDesktopManager extends DefaultDesktopManager implements UIResource {
    }

    /** Each keyboard action; the name says which. */
    private class DesktopAction extends AbstractAction {

        private final String what;

        DesktopAction(String what) {
            super(what);
            this.what = what;
        }

        public void actionPerformed(ActionEvent e) {
            JInternalFrame f = desktop.getSelectedFrame();
            if ("selectNextFrame".equals(what) || "navigateNext".equals(what)) {
                move(1);
            } else if ("selectPreviousFrame".equals(what) || "navigatePrevious".equals(what)) {
                move(-1);
            } else if (f == null) {
                return;
            } else if ("close".equals(what)) {
                if (f.isClosable()) {
                    f.doDefaultCloseAction();
                }
            } else if ("minimize".equals(what)) {
                if (f.isIconifiable() && !f.isIcon()) {
                    attempt(f, "icon", true);
                }
            } else if ("maximize".equals(what)) {
                if (f.isMaximizable() && !f.isMaximum()) {
                    attempt(f, "maximum", true);
                }
            } else if ("restore".equals(what)) {
                if (f.isIcon()) {
                    attempt(f, "icon", false);
                } else if (f.isMaximum()) {
                    attempt(f, "maximum", false);
                }
            } else {
                // move, resize, escape and the six arrow ones are gestures that need a keyboard and
                // a
                                // screen: with neither of the two there is nothing to do. See the
                                // class note.
                return;
            }
        }

        /** It goes to the next frame or to the previous one, in the order they are in. */
        private void move(int paso) {
            JInternalFrame[] frames = desktop.getAllFrames();
            if (frames.length == 0) {
                return;
            }
            JInternalFrame current = desktop.getSelectedFrame();
            int i = 0;
            for (int k = 0; k < frames.length; k++) {
                if (frames[k] == current) {
                    i = k;
                    break;
                }
            }
            int next = ((i + paso) % frames.length + frames.length) % frames.length;
            attempt(frames[next], "selected", true);
        }

        /** An internal frame's changes of state may be vetoed. */
        private void attempt(JInternalFrame f, String property, boolean value) {
            try {
                if ("icon".equals(property)) {
                    f.setIcon(value);
                } else if ("maximum".equals(property)) {
                    f.setMaximum(value);
                } else {
                    f.setSelected(value);
                }
            } catch (java.beans.PropertyVetoException ex) {
                // Somebody said no. It is a valid answer, not an error.
            }
        }
    }

    /** It rebuilds the manager when the desktop changes look and feel. */
    private class Handler implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent e) {
            if ("desktopManager".equals(e.getPropertyName())) {
                desktopManager = desktop.getDesktopManager();
            }
        }
    }
}
