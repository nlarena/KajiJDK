package jdk.swing.interop;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowFocusListener;

/**
 * The window nobody sees, inside which Swing draws when another toolkit is hosting it.
 *
 * <h2>What a window that is never shown is for</h2>
 *
 * <p>Swing cannot draw without a window: the component hierarchy has to end in one, and that is
 * where focus, repainting and event dispatch come from. When the one showing the interface is
 * another toolkit, the window still has to exist -- otherwise nothing in Swing works -- but it never
 * appears on screen. It is drawn in memory and the host copies the result.
 *
 * <h2>Events the other way round</h2>
 *
 * <p>The four {@code create...Event} methods run in the opposite direction to the usual one: the
 * mouse and the keyboard reach the host, not AWT, so the matching AWT event has to be built and
 * injected. {@link #createUngrabEvent} is the one that says a popup menu must close because the user
 * clicked outside.
 *
 * <p>{@link #emulateActivation} is of the same kind: the window never gets focus from the system
 * because it is not on screen, so it has to be told it has focus for Swing to draw the caret and the
 * focus borders.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>One cannot be built. The constructor puts together a window, and on a machine with no screen
 * that throws {@link HeadlessException} -- in the JDK too -- and this implementation talks to no
 * windowing system. The methods are here because the class declares them; none is reachable, and
 * they all throw the same thing in case that ever stops being true.
 *
 * @since 9
 */
public class LightweightFrameWrapper {

    /**
     * One.
     *
     * @throws HeadlessException always: a window is needed, and there is no screen
     */
    public LightweightFrameWrapper() {
        throw new HeadlessException();
    }

    /**
     * Says the screen's density changed.
     *
     * @param scale the new scale
     */
    public void notifyDisplayChanged(int scale) {
        throw new HeadlessException();
    }

    /**
     * Says the screen's density changed, with a different scale per axis.
     *
     * @param scaleX the horizontal scale
     * @param scaleY the vertical scale
     */
    public void notifyDisplayChanged(double scaleX, double scaleY) {
        throw new HeadlessException();
    }

    /**
     * Tells it where the host's window is.
     *
     * <p>It is needed for popup menus, which are real windows and have to appear at the right place
     * on the screen, not on the component.
     *
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void setHostBounds(int x, int y, int w, int h) {
        throw new HeadlessException();
    }

    /** Closes it and releases whatever it holds. */
    public void dispose() {
        throw new HeadlessException();
    }

    /**
     * Adds whoever wants to hear that the window gained or lost focus.
     *
     * @param listener whom to tell
     */
    public void addWindowFocusListener(WindowFocusListener listener) {
        throw new HeadlessException();
    }

    /**
     * Shows it or hides it.
     *
     * @param visible whether it is shown
     */
    public void setVisible(boolean visible) {
        throw new HeadlessException();
    }

    /**
     * Moves and resizes it.
     *
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public void setBounds(int x, int y, int w, int h) {
        throw new HeadlessException();
    }

    /**
     * Gives it the content it will host.
     *
     * @param content the content
     */
    public void setContent(LightweightContentWrapper content) {
        throw new HeadlessException();
    }

    /**
     * Makes it believe it gained or lost the system's focus.
     *
     * @param activate whether it is taken to be active
     */
    public void emulateActivation(boolean activate) {
        throw new HeadlessException();
    }

    /**
     * Builds a mouse event to inject.
     *
     * @param frame the window
     * @param id which event it is
     * @param when when it happened
     * @param modifiers the keys and buttons held down
     * @param x where, relative to the window
     * @param y where, relative to the window
     * @param xAbs where, on screen
     * @param yAbs where, on screen
     * @param clickCount how many clicks in a row
     * @param popupTrigger whether it is the gesture that opens the context menu
     * @param button which button
     * @return the event
     */
    public MouseEvent createMouseEvent(LightweightFrameWrapper frame, int id, long when,
            int modifiers, int x, int y, int xAbs, int yAbs, int clickCount, boolean popupTrigger,
            int button) {
        throw new HeadlessException();
    }

    /**
     * Builds a wheel event to inject.
     *
     * @param frame the window
     * @param scrollType whether the scroll is by units or by screens
     * @param scrollAmount how many units
     * @param wheelRotation how far the wheel turned
     * @param clickCount how many clicks in a row
     * @return the event
     */
    public MouseWheelEvent createMouseWheelEvent(LightweightFrameWrapper frame, int scrollType,
            int scrollAmount, int wheelRotation, int clickCount) {
        throw new HeadlessException();
    }

    /**
     * Builds a keyboard event to inject.
     *
     * @param frame the window
     * @param id which event it is
     * @param when when it happened
     * @param modifiers the keys held down
     * @param keyCode which key
     * @param keyChar which character it produced
     * @return the event
     */
    public KeyEvent createKeyEvent(LightweightFrameWrapper frame, int id, long when, int modifiers,
            int keyCode, char keyChar) {
        throw new HeadlessException();
    }

    /**
     * Builds the event that closes popup menus.
     *
     * @param frame the window
     * @return the event
     */
    public AWTEvent createUngrabEvent(LightweightFrameWrapper frame) {
        throw new HeadlessException();
    }

    /**
     * Which component falls at that point.
     *
     * @param frame the window
     * @param x where, relative to the window
     * @param y where, relative to the window
     * @param ignoreEnabled whether disabled components count too
     * @return the component
     */
    public Component findComponentAt(LightweightFrameWrapper frame, int x, int y,
            boolean ignoreEnabled) {
        throw new HeadlessException();
    }

    /**
     * Whether that component is this window.
     *
     * @param comp the component
     * @param frame the window
     * @return true if they are the same
     */
    public boolean isCompEqual(Component comp, LightweightFrameWrapper frame) {
        throw new HeadlessException();
    }
}
