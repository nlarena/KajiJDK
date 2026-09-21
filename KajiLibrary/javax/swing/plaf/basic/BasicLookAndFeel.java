package javax.swing.plaf.basic;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;

/**
 * The base every graphical look and feel in Swing inherits from.
 *
 * <h2>What it contributes</h2>
 *
 * <p>The table of default values: which class draws each component, what colours the system has,
 * and the hundreds of values a component consults on installing itself. A concrete look and feel
 * inherits from here and redefines what it wants to change, which is usually a fraction.
 *
 * <p>This class is not used directly -- its constructor is {@code protected} -- because on its
 * own it defines no complete look and feel. It is the scaffolding.
 *
 * <h2>{@link #getDefaults}' three steps</h2>
 *
 * <p>First the graphical interfaces' classes, then the system's colours, then everything else.
 * The order matters: {@link #initComponentDefaults}' values are written in terms of the system's
 * colours, so those have to be there first.
 *
 * <h2>The sounds</h2>
 *
 * <p>A graphical look and feel may have sounds -- a menu's click, a dialog's warning -- and
 * {@link #getAudioActionMap} is where they are declared. They are here and not in the concrete
 * look and feel because the mechanism for playing them is the same for all.
 *
 * <h2>State in this library</h2>
 *
 * <p>The structure is there and works: it can be inherited from, the table populated and
 * consulted. What there is not is content: {@link #initClassDefaults} registers no graphical
 * interface because this library does not yet have a concrete look and feel that provides them.
 * A look and feel that inherits from here and fills the table works.
 *
 * @since 1.2
 */
public abstract class BasicLookAndFeel extends LookAndFeel {

    /** One; only for the subclasses. */
    protected BasicLookAndFeel() {
    }

    /**
     * This look and feel's table of values.
     *
     * @return the table, already populated
     */
    @Override
    public UIDefaults getDefaults() {
        final UIDefaults table = new UIDefaults();
        initClassDefaults(table);
        initSystemColorDefaults(table);
        initComponentDefaults(table);
        return table;
    }

    /** It installs itself. */
    @Override
    public void initialize() {
    }

    /** It uninstalls itself. */
    @Override
    public void uninitialize() {
    }

    /**
     * It registers which class draws each component.
     *
     * <p>The keys are the identifiers {@code JComponent.getUIClassID} returns, such as
     * {@code "ButtonUI"}, and the values are class names. They go as text and not as a
     * {@code Class} so as not to load a look and feel's hundred classes at start-up; see
     * {@link UIDefaults}.
     *
     * @param table the table to populate
     */
    protected void initClassDefaults(UIDefaults table) {
    }

    /**
     * It sets the system's colours.
     *
     * @param table the table to populate
     */
    protected void initSystemColorDefaults(UIDefaults table) {
    }

    /**
     * It loads the system's colours from a list of name-value pairs.
     *
     * <p>The values are integers in hexadecimal written as text. The switch decides whether those
     * are used or the ones the desktop reports: a look and feel that wants to look the same
     * everywhere uses its own, and one that wants to fit in uses the system's.
     *
     * @param table the table to populate
     * @param systemColors the pairs, alternating
     * @param useNative whether the desktop's are to be preferred
     */
    protected void loadSystemColors(UIDefaults table, String[] systemColors, boolean useNative) {
        for (int i = 0; i < systemColors.length - 1; i += 2) {
            table.put(systemColors[i], new java.awt.Color(
                    (int) Long.parseLong(systemColors[i + 1].substring(1), 16)));
        }
    }

    /**
     * It sets everything else: colours, typefaces, borders, margins and keyboard shortcuts.
     *
     * @param table the table to populate
     */
    protected void initComponentDefaults(UIDefaults table) {
    }

    /**
     * This look and feel's sounds.
     *
     * @return the map of sound actions, or {@code null} if it has none
     */
    protected ActionMap getAudioActionMap() {
        return null;
    }

    /**
     * It makes the action that plays a sound.
     *
     * @param key the sound's key in the table
     * @return the action, or {@code null} if there is no sound for that key
     */
    protected Action createAudioAction(Object key) {
        return null;
    }

    /**
     * It plays that action's sound.
     *
     * <p>It does nothing if the action is {@code null}: the caller has no reason to check whether
     * the look and feel defines that sound, and doing it at every call site would be the same
     * check repeated twenty times.
     *
     * @param audioAction the action, or {@code null}
     */
    protected void playSound(Action audioAction) {
    }
}
