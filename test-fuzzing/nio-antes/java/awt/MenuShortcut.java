package java.awt;

/**
 * The keyboard shortcut of a menu item: a key and whether Shift is needed as well.
 *
 * <p>The key is kept as a {@code KeyEvent} code, not as a character. The difference matters: a
 * shortcut fires with the physical key, so the 'A' of one keyboard and that of another are the same
 * shortcut even if the character they produce changes with the layout.
 *
 * <p>The {@code hashCode()} is the key code, or its one's complement if it uses Shift. It is a
 * bijection --{@code ~k} never matches a valid key code, which is positive-- so Ctrl+A and
 * Ctrl+Shift+A never collide, which is exactly what the shortcut map of a menu bar needs.
 *
 * <h2>What is missing and why</h2>
 *
 * <p>{@code paramString()} is here: against what this note used to say, it does not use
 * {@code KeyEvent} --it builds the string with the raw key code-- and can be written whole.
 *
 * <p>{@code toString()} <b>is not here</b>. The JDK builds it with
 * {@code KeyEvent.getKeyModifiersText()} and {@code KeyEvent.getKeyText()} --which turn a key code
 * into the name the system gives it-- and it also asks the {@code Toolkit} which is the platform's
 * menu modifier key, which on macOS is not Ctrl. This note used to say that neither
 * {@code java.awt.event} nor {@code Toolkit} existed in KajiLibrary: both do, and so do those three
 * methods, so nothing is stopping it from being written. It simply is not written, and while it is
 * not, {@code Object}'s is inherited, which asserts nothing.
 */
public class MenuShortcut implements java.io.Serializable {

    private static final long serialVersionUID = 143448358473180225L;

    int key;

    boolean usesShift;

    public MenuShortcut(int key) {
        this(key, false);
    }

    public MenuShortcut(int key, boolean useShiftModifier) {
        this.key = key;
        this.usesShift = useShiftModifier;
    }

    public int getKey() {
        return key;
    }

    public boolean usesShiftModifier() {
        return usesShift;
    }

    /**
     * Typed overload: the one the menu bar uses, which already knows it compares against another
     * shortcut.
     */
    public boolean equals(MenuShortcut s) {
        return (s != null && (s.getKey() == key)
                && (s.usesShiftModifier() == usesShift));
    }

    public boolean equals(Object obj) {
        if (obj instanceof MenuShortcut) {
            return equals((MenuShortcut) obj);
        }
        return false;
    }

    public int hashCode() {
        return (usesShift) ? (~key) : key;
    }

    /**
     * The description of the shortcut, without the class name.
     *
     * <p>It uses the **code** of the key and not its name, which is exactly what makes it writable
     * without {@code KeyEvent} or {@code Toolkit}: the readable name is put by {@code toString},
     * which is the one that does need them.
     */
    protected String paramString() {
        String out = "key=" + this.getKey();
        if (this.usesShiftModifier()) {
            out = out + ",usesShiftModifier";
        }
        return out;
    }
}
