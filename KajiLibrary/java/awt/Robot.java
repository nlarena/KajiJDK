package java.awt;

import java.awt.image.BufferedImage;
import java.awt.image.MultiResolutionImage;

/**
 * Generates mouse and keyboard events **at the system level**, as though a person had made them.
 *
 * <p>That is the difference from building a {@link java.awt.event.KeyEvent} and dispatching it by
 * hand: that one reaches this program only, and a `Robot` reaches the windowing system, so it can
 * drive any window and also **read the screen**. That is why it is the basis of automated interface
 * testing.
 *
 * <p><strong>Here it cannot be built.</strong> Both constructors throw {@link AWTException}, which
 * is what the JDK does without a screen: with no windowing system there is nobody to send the
 * events to and no screen to read. The instance methods are declared because they are part of the
 * class, but there is no way to reach them: no instance exists.
 *
 * <p>The {@code autoDelay} and the {@code autoWaitForIdle} are what make this class usable: without
 * them the events are generated faster than the interface processes them and the test measures
 * nothing in particular.
 */
public class Robot {

    /** How long it waits after each generated event, in milliseconds. */
    private int autoDelay;

    /** Whether it waits for the event queue to drain after each one. */
    private boolean autoWaitForIdle;

    /**
     * A robot over the main screen.
     *
     * @throws AWTException always: with no screen there is no windowing system to send events to
     */
    public Robot() throws AWTException {
        throw new AWTException("headless environment");
    }

    /**
     * A robot over that screen.
     *
     * @throws AWTException always, for the same reason
     * @throws IllegalArgumentException if the device is not a screen
     * @throws NullPointerException if the device is `null`
     */
    public Robot(GraphicsDevice screen) throws AWTException {
        if (screen == null) {
            throw new NullPointerException("screen");
        }
        if (screen.getType() != GraphicsDevice.TYPE_RASTER_SCREEN) {
            throw new IllegalArgumentException("not a screen device");
        }
        throw new AWTException("headless environment");
    }

    /** Moves the pointer to that point of the screen. */
    public synchronized void mouseMove(int x, int y) {
        this.afterEvent();
    }

    /**
     * Presses those mouse buttons.
     *
     * @param buttons a combination of the {@code BUTTONn_DOWN_MASK} masks of
     *     {@link java.awt.event.InputEvent}
     * @throws IllegalArgumentException if there is no button mask at all
     */
    public synchronized void mousePress(int buttons) {
        this.checkButtons(buttons);
        this.afterEvent();
    }

    /**
     * Releases those buttons.
     *
     * @throws IllegalArgumentException if there is no button mask at all
     */
    public synchronized void mouseRelease(int buttons) {
        this.checkButtons(buttons);
        this.afterEvent();
    }

    /**
     * Turns the wheel that many notches.
     *
     * @param wheelAmt negative upwards, positive downwards
     */
    public synchronized void mouseWheel(int wheelAmt) {
        this.afterEvent();
    }

    /**
     * Presses that key.
     *
     * @param keycode one of the {@code VK_} constants of {@link java.awt.event.KeyEvent}
     * @throws IllegalArgumentException if the code is not valid
     */
    public synchronized void keyPress(int keycode) {
        this.afterEvent();
    }

    /**
     * Releases that key.
     *
     * @throws IllegalArgumentException if the code is not valid
     */
    public synchronized void keyRelease(int keycode) {
        this.afterEvent();
    }

    /**
     * What colour that pixel of the screen is.
     *
     * @throws IllegalStateException always —though nothing gets here: there are no instances
     */
    public synchronized Color getPixelColor(int x, int y) {
        throw new IllegalStateException("there is no screen to read");
    }

    /**
     * A snapshot of that rectangle of the screen.
     *
     * @throws IllegalArgumentException if the rectangle is empty
     */
    public synchronized BufferedImage createScreenCapture(Rectangle screenRect) {
        this.checkRect(screenRect);
        throw new IllegalStateException("there is no screen to read");
    }

    /**
     * The same, but with one image per screen resolution.
     *
     * <p>It exists because of high-density screens: the snapshot has more pixels than the rectangle
     * that was asked for, and a {@link MultiResolutionImage} lets one choose which to use.
     *
     * @throws IllegalArgumentException if the rectangle is empty
     */
    public synchronized MultiResolutionImage createMultiResolutionScreenCapture(
            Rectangle screenRect) {
        this.checkRect(screenRect);
        throw new IllegalStateException("there is no screen to read");
    }

    /** Whether it waits for the event queue to drain after each one. */
    public synchronized boolean isAutoWaitForIdle() {
        return this.autoWaitForIdle;
    }

    /** Says whether to wait for the queue to drain after each event. */
    public synchronized void setAutoWaitForIdle(boolean isOn) {
        this.autoWaitForIdle = isOn;
    }

    /** How long it waits after each event. */
    public synchronized int getAutoDelay() {
        return this.autoDelay;
    }

    /**
     * Changes how long to wait after each event.
     *
     * @throws IllegalArgumentException if it is not between 0 and 60000
     */
    public synchronized void setAutoDelay(int ms) {
        if (ms < 0 || ms > 60000) {
            throw new IllegalArgumentException("Delay must be to 0 to 60,000ms");
        }
        this.autoDelay = ms;
    }

    /**
     * Sleeps for that long.
     *
     * <p>It is the only thing in this class that needs no screen, and that is why it is the only
     * one that really does something. It does not let the interruption out, just as the JDK does
     * not: instead of printing the trace, as the JDK does, it restores the interrupt flag, so
     * whoever uses it in a test does not have to catch an `InterruptedException` at every step.
     *
     * @throws IllegalArgumentException if it is not between 0 and 60000
     */
    public void delay(int ms) {
        if (ms < 0 || ms > 60000) {
            throw new IllegalArgumentException("Delay must be to 0 to 60,000ms");
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Waits for the event queue to drain. */
    public synchronized void waitForIdle() {
        try {
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                }
            });
        } catch (Exception e) {
            // That the queue cannot be drained is not the robot's fault.
        }
    }

    public synchronized String toString() {
        return this.getClass().getName() + "[ autoDelay = " + this.getAutoDelay()
                + ", autoWaitForIdle = " + this.isAutoWaitForIdle() + " ]";
    }

    /** What goes after each generated event. */
    private void afterEvent() {
        if (this.autoWaitForIdle) {
            this.waitForIdle();
        }
        if (this.autoDelay > 0) {
            this.delay(this.autoDelay);
        }
    }

    /** That there is at least one button mask. */
    private void checkButtons(int buttons) {
        int mask = java.awt.event.InputEvent.BUTTON1_DOWN_MASK
                | java.awt.event.InputEvent.BUTTON2_DOWN_MASK
                | java.awt.event.InputEvent.BUTTON3_DOWN_MASK;
        if ((buttons & mask) == 0) {
            throw new IllegalArgumentException("Invalid combination of button flags");
        }
    }

    /** That the rectangle has some surface. */
    private void checkRect(Rectangle r) {
        if (r == null) {
            throw new NullPointerException("screenRect");
        }
        if (r.width <= 0 || r.height <= 0) {
            throw new IllegalArgumentException("width and height must be > 0");
        }
    }
}
