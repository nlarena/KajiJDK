package javax.swing;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

/**
 * It decides when a tool tip appears and when it goes away.
 *
 * <h2>Three delays, and all three have a reason</h2>
 *
 * <p>{@link #setInitialDelay} is how long one has to stay still over something for the tip to
 * appear: without that wait, moving the mouse across the screen would fill everything with
 * tips. {@link #setDismissDelay} is how long it stays before going away by itself, because a
 * tip that does not go away covers what is underneath. {@link #setReshowDelay} is the window
 * during which going to another component shows its tip <em>without</em> waiting again -- it is
 * what allows a tool bar to be walked reading each button without stopping at each one.
 *
 * <h2>A single one for the whole application</h2>
 *
 * <p>{@link #sharedInstance} is the only way of getting it, and the constructor is not public.
 * There has to be one: two tips at once make no sense, and the three delays are a decision of
 * the whole application.
 *
 * <h2>With no screen there is no tip</h2>
 *
 * <p>The state -- the delays, whether it is switched on, which components are registered -- is
 * kept and read. What does not happen is the appearing: showing a tip needs a window and a
 * mouse that moves, and with neither of the two there is nothing to show.
 * {@link #registerComponent} connects the listeners all the same, so as soon as there is a
 * screen it works.
 */
public final class ToolTipManager extends MouseAdapter implements MouseMotionListener {

    Timer enterTimer;
    Timer exitTimer;
    Timer insideTimer;
    String toolTipText;
    Point preferredLocation;
    JComponent insideComponent;
    MouseEvent mouseEvent;
    boolean showImmediately;
    transient Popup tipWindow;
    JToolTip tip;
    boolean enabled = true;

    /** Whether the tip may be drawn inside the window instead of in one of its own. */
    protected boolean lightWeightPopupEnabled = true;

    /** Whether a system window is allowed for the tip. */
    protected boolean heavyWeightPopupEnabled = false;

    private static final ToolTipManager SHARED = new ToolTipManager();

    /** It is not public; see the class note. */
    ToolTipManager() {
        enterTimer = new Timer(750, new InsideTimerAction(this));
        enterTimer.setRepeats(false);
        exitTimer = new Timer(500, new OutsideTimerAction(this));
        exitTimer.setRepeats(false);
        insideTimer = new Timer(4000, new StillInsideTimerAction(this));
        insideTimer.setRepeats(false);
    }

    /** It switches the whole application's tips on or off. */
    public void setEnabled(boolean flag) {
        enabled = flag;
        if (!flag) {
            hideTipWindow();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Whether the tip may be drawn inside the window.
     *
     * <p>It is cheaper, and it does not serve when the tip goes outside the window's edge or when
     * there is a heavyweight component underneath that would cover it.
     */
    public void setLightWeightPopupEnabled(boolean aFlag) {
        lightWeightPopupEnabled = aFlag;
    }

    public boolean isLightWeightPopupEnabled() {
        return lightWeightPopupEnabled;
    }

    /** How long one has to stay still for it to appear; see the class note. */
    public void setInitialDelay(int milliseconds) {
        enterTimer.setInitialDelay(milliseconds);
    }

    public int getInitialDelay() {
        return enterTimer.getInitialDelay();
    }

    /** How long it stays before going away by itself. */
    public void setDismissDelay(int milliseconds) {
        insideTimer.setInitialDelay(milliseconds);
    }

    public int getDismissDelay() {
        return insideTimer.getInitialDelay();
    }

    /** The window for going from one component to another without waiting again. */
    public void setReshowDelay(int milliseconds) {
        exitTimer.setInitialDelay(milliseconds);
    }

    public int getReshowDelay() {
        return exitTimer.getInitialDelay();
    }

    /**
     * It shows the tip.
     *
     * <p>With no screen there is nowhere to show it; see the class note. What does happen is the
     * state part: the tip is built and the timer that is going to hide it is started.
     */
    void showTipWindow() {
        if (insideComponent == null || !insideComponent.isShowing()) {
            return;
        }
        if (!enabled || !insideComponent.isEnabled()) {
            return;
        }
        if (tipWindow != null) {
            return;
        }
        tip = insideComponent.createToolTip();
        tip.setTipText(toolTipText);
        insideTimer.start();
    }

    /** It hides the tip and stops the timers. */
    void hideTipWindow() {
        if (tipWindow != null) {
            tipWindow.hide();
            tipWindow = null;
        }
        tip = null;
        insideTimer.stop();
    }

    /** The single tip manager; see the class note. */
    public static ToolTipManager sharedInstance() {
        return SHARED;
    }

    /**
     * It starts watching that component.
     *
     * <p>It is unregistered first: registering twice would leave two listeners and the tip would
     * appear twice.
     */
    public void registerComponent(JComponent component) {
        component.removeMouseListener(this);
        component.addMouseListener(this);
        component.removeMouseMotionListener(this);
        component.addMouseMotionListener(this);
    }

    /** It stops watching it. */
    public void unregisterComponent(JComponent component) {
        component.removeMouseListener(this);
        component.removeMouseMotionListener(this);
        if (component == insideComponent) {
            hideTipWindow();
            insideComponent = null;
            toolTipText = null;
            mouseEvent = null;
        }
    }

    /**
     * The mouse came in: it starts the wait, or shows at once if it comes from another component.
     */
    public void mouseEntered(MouseEvent event) {
        initiateToolTip(event);
    }

    private void initiateToolTip(MouseEvent event) {
        if (event.getSource() == tipWindow) {
            return;
        }
        JComponent component = (JComponent) event.getSource();
        component.getToolTipText(event);
        exitTimer.stop();
        Point location = event.getPoint();
        if (location.x < 0 || location.x >= component.getWidth()
                || location.y < 0 || location.y >= component.getHeight()) {
            return;
        }
        if (insideComponent != null) {
            enterTimer.stop();
        }
        insideComponent = component;
        mouseEvent = event;
        toolTipText = component.getToolTipText(event);
        preferredLocation = component.getToolTipLocation(event);
        if (showImmediately) {
            showTipWindow();
        } else {
            enterTimer.start();
        }
    }

    /** The mouse went out: it hides, but the reappearing window is left. */
    public void mouseExited(MouseEvent event) {
        if (insideComponent == null) {
            return;
        }
        if (window(event) == window(mouseEvent)) {
            enterTimer.stop();
        }
        hideTipWindow();
        insideComponent = null;
        toolTipText = null;
        mouseEvent = null;
        showImmediately = false;
        exitTimer.start();
    }

    private static java.awt.Window window(MouseEvent e) {
        if (e == null || !(e.getSource() instanceof Component)) {
            return null;
        }
        return SwingUtilities.getWindowAncestor((Component) e.getSource());
    }

    /** A click hides the tip: the user is no longer reading, they are doing something. */
    public void mousePressed(MouseEvent event) {
        hideTipWindow();
        enterTimer.stop();
        showImmediately = false;
        insideComponent = null;
        mouseEvent = null;
    }

    /** Dragging is not reading either. */
    public void mouseDragged(MouseEvent event) {
    }

    /**
     * Moving the mouse within the same component restarts the wait.
     *
     * <p>Unless the text has changed -- a table gives a text per cell --, in which case the tip is
     * rebuilt.
     */
    public void mouseMoved(MouseEvent event) {
        if (tipWindow != null) {
            return;
        }
        JComponent component = (JComponent) event.getSource();
        String newText = component.getToolTipText(event);
        Point newPreferredLocation = component.getToolTipLocation(event);
        boolean sameText = (newText == null) ? (toolTipText == null)
                : newText.equals(toolTipText);
        boolean sameLoc = (preferredLocation == null) ? (newPreferredLocation == null)
                : preferredLocation.equals(newPreferredLocation);
        if (sameText && sameLoc) {
            if (toolTipText != null) {
                if (enterTimer.isRunning()) {
                    enterTimer.restart();
                }
            }
        } else {
            toolTipText = newText;
            preferredLocation = newPreferredLocation;
            if (showImmediately) {
                hideTipWindow();
                showTipWindow();
                exitTimer.stop();
            } else {
                enterTimer.restart();
            }
        }
        mouseEvent = event;
    }

    /** The system window that contains that component, or null. */
    static Frame frameForComponent(Component component) {
        Component c = component;
        while (c != null && !(c instanceof Frame)) {
            c = c.getParent();
        }
        return (Frame) c;
    }

    /** The wait is up: the tip appears. */
    private static class InsideTimerAction implements ActionListener {

        private final ToolTipManager m;

        InsideTimerAction(ToolTipManager m) {
            this.m = m;
        }

        public void actionPerformed(ActionEvent e) {
            m.showImmediately = true;
            m.showTipWindow();
        }
    }

    /** The reappearing window has passed: next time it has to wait again. */
    private static class OutsideTimerAction implements ActionListener {

        private final ToolTipManager m;

        OutsideTimerAction(ToolTipManager m) {
            this.m = m;
        }

        public void actionPerformed(ActionEvent e) {
            m.showImmediately = false;
            m.insideComponent = null;
            m.mouseEvent = null;
        }
    }

    /** The tip has been up too long: it goes away. */
    private static class StillInsideTimerAction implements ActionListener {

        private final ToolTipManager m;

        StillInsideTimerAction(ToolTipManager m) {
            this.m = m;
        }

        public void actionPerformed(ActionEvent e) {
            m.hideTipWindow();
            m.enterTimer.stop();
            m.showImmediately = false;
            m.insideComponent = null;
            m.mouseEvent = null;
        }
    }
}
