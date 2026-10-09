package javax.swing;

import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;

/**
 * An action: what a button or a menu does, separate from the button or the menu.
 *
 * <p>It is an {@link ActionListener} with properties -- name, icon, mnemonic, whether it is
 * enabled -- and with notices when they change. The separation is what allows one and the same
 * "Save" to live in a button, in a menu and in a shortcut, and disabling it once to disable it
 * in all three: each component listens to the properties and settles itself.
 *
 * <p>The keys are strings because the properties are open: an application may keep its own
 * beside the standard ones.
 */
public interface Action extends ActionListener {

    /** A default property's key; nobody in the JDK uses it, it exists out of history. */
    String DEFAULT = "Default";

    /** The name: the button's or the menu's text. */
    String NAME = "Name";

    /** A short description: the floating tip's text. */
    String SHORT_DESCRIPTION = "ShortDescription";

    /** A long description, for contextual help. */
    String LONG_DESCRIPTION = "LongDescription";

    /** The small icon: a menu's, and a button's if there is no large one. */
    String SMALL_ICON = "SmallIcon";

    /** The command that goes in the {@code ActionEvent}. */
    String ACTION_COMMAND_KEY = "ActionCommandKey";

    /** The keyboard accelerator, a {@code KeyStroke}. */
    String ACCELERATOR_KEY = "AcceleratorKey";

    /** The mnemonic, an {@code Integer} with the virtual key. */
    String MNEMONIC_KEY = "MnemonicKey";

    /** Whether it is selected, for buttons with state; a {@code Boolean}. */
    String SELECTED_KEY = "SwingSelectedKey";

    /** Which character of the name to underline; an {@code Integer}. */
    String DISPLAYED_MNEMONIC_INDEX_KEY = "SwingDisplayedMnemonicIndexKey";

    /** The large icon: a button's, if it is there. */
    String LARGE_ICON_KEY = "SwingLargeIconKey";

    /** The property with that key, or {@code null}. */
    Object getValue(String key);

    /** It sets the property with that key, giving notice to the listeners if it changed. */
    void putValue(String key, Object value);

    /** It enables or disables; the components that use it learn about it and settle themselves. */
    void setEnabled(boolean b);

    boolean isEnabled();

    /**
     * Whether this action accepts being fired from that source.
     *
     * <p>By default it accepts anybody. It is the way for an action to say "not from this
     * component", without disabling itself for the others.
     */
    default boolean accept(Object sender) {
        return true;
    }

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);
}
