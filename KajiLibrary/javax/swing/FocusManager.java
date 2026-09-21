package javax.swing;

import java.awt.DefaultKeyboardFocusManager;
import java.awt.KeyboardFocusManager;

/**
 * Swing's focus manager.
 *
 * <h2>A class left over from another time</h2>
 *
 * <p>Before Java 1.4, Swing had its own focus manager and this was the way in. Since then the
 * focus is handled by AWT with {@link KeyboardFocusManager}, and this class stayed as a facade:
 * {@link #getCurrentManager} and {@link #setCurrentManager} are AWT's manager seen through an
 * older type.
 *
 * <p>{@link #disableSwingFocusManager} and {@link #isFocusManagerEnabled} no longer do anything
 * useful -- they are marked obsolete in the JDK and are kept because they are public --. It is
 * worth knowing before writing code that depends on them.
 */
public abstract class FocusManager extends DefaultKeyboardFocusManager {

    /**
     * The key another manager was asked for with.
     *
     * <p>Nobody reads it any more; see the class note.
     */
    public static final String FOCUS_MANAGER_CLASS_PROPERTY = "FocusManagerClassName";

    /** For the subclasses. */
    protected FocusManager() {
    }

    /**
     * This context's focus manager.
     *
     * <p>If the one there is is not of this type -- which is the usual thing, because AWT's is not
     * -- it is returned wrapped: the method promises a {@code FocusManager} and that has to be
     * kept.
     */
    public static FocusManager getCurrentManager() {
        KeyboardFocusManager m = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        if (m instanceof FocusManager) {
            return (FocusManager) m;
        }
        return new AwtDelegate();
    }

    /**
     * It changes the manager.
     *
     * <p>A wrapped one goes back to being AWT's: passing AWT its own wrapper would leave it
     * pointing at itself.
     *
     * @throws SecurityException if the context does not allow it
     */
    public static void setCurrentManager(FocusManager aFocusManager) {
        KeyboardFocusManager toSet = aFocusManager;
        if (aFocusManager instanceof AwtDelegate) {
            toSet = null;
        }
        KeyboardFocusManager.setCurrentKeyboardFocusManager(toSet);
    }

    /**
     * It does nothing.
     *
     * @deprecated As in the JDK: Swing's manager no longer exists, so there is nothing to switch
     *     off.
     */
    @Deprecated
    public static void disableSwingFocusManager() {
    }

    /**
     * Always true.
     *
     * @deprecated Ver {@link #disableSwingFocusManager}.
     */
    @Deprecated
    public static boolean isFocusManagerEnabled() {
        return true;
    }

    /**
     * It wraps AWT's manager so as to be able to return it with this type.
     *
     * <p>It adds no behaviour: it inherits everything from
     * {@link DefaultKeyboardFocusManager}.
     */
    private static class AwtDelegate extends FocusManager {

        AwtDelegate() {
            super();
        }
    }
}
