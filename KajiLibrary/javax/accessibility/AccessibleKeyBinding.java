package javax.accessibility;

/**
 * The keyboard shortcuts that activate an object.
 *
 * <p>The shortcut is returned as {@code Object} and not as a concrete type because not all of them
 * are the same kind of thing: in AWT it is a {@code java.awt.MenuShortcut} and in Swing a
 * {@code KeyStroke}. It is one of those signatures that look loose and that are really avoiding
 * coupling the package to one of the two.
 */
public interface AccessibleKeyBinding {

    /** How many shortcuts there are. */
    int getAccessibleKeyBindingCount();

    /**
     * The `i`-th shortcut.
     *
     * @return the shortcut, or `null` if there are not that many
     */
    Object getAccessibleKeyBinding(int i);
}
