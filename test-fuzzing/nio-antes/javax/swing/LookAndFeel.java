package javax.swing;

import javax.swing.plaf.UIResource;

/**
 * A look and feel: the set of {@code ComponentUI}s and default values that give Swing a face.
 *
 * <h2>What there is and what there is not</h2>
 *
 * <p>This library has <strong>a single look and feel</strong>: the basic one, with the default
 * values measured in Metal, installed directly by each {@code updateUI}.
 *
 * <p>This note used to say that the methods that consult {@link UIManager}'s tables were not
 * there either, because there was no {@code UIManager} to register them in and with no tables
 * they could only lie. There is one now, along with {@link UIDefaults}, so they are all there:
 * those that install colours, typeface and borders, those that build key maps, and
 * {@link #getLayoutStyle}.
 *
 * <p>What is there is what does not depend on tables: {@link #installProperty}, which is how a
 * look and feel sets a property <em>without overwriting what the user set</em>, and
 * {@link #uninstallBorder}, which takes a border away only if it is the look and feel's. Both
 * rest on {@link UIResource}, which is the way of telling one from the other.
 */
public abstract class LookAndFeel {

    public LookAndFeel() {
    }

    /**
     * It sets a property in the component, unless the user has already set it.
     *
     * <p>It is the rule of coexistence between look and feel and programmer: the look and feel
     * proposes, the programmer disposes. The component remembers which properties the programmer
     * set, and this call respects those. The properties that are admitted depend on the component;
     * one that is not admitted is an {@code IllegalArgumentException}.
     */
    public static void installProperty(JComponent c, String propertyName, Object propertyValue) {
        c.setUIProperty(propertyName, propertyValue);
    }

    /** It takes the component's border away if a look and feel set it; one of the user's stays. */
    public static void uninstallBorder(JComponent c) {
        if (c.getBorder() instanceof UIResource) {
            c.setBorder(null);
        }
    }

    /**
     * The disabled icon that corresponds to that icon: none.
     *
     * <p>The JDK makes a greyed one when the icon is an {@code ImageIcon}, and {@code null} for any
     * other. With no {@code ImageIcon}, the answer is always the second, and whoever receives it
     * -- {@code AbstractButton}, {@code JLabel} -- paints the ordinary icon.
     */
    public Icon getDisabledIcon(JComponent component, Icon icon) {
        return null;
    }

    /** The disabled and selected icon: none, for the same reason as {@link #getDisabledIcon}. */
    public Icon getDisabledSelectedIcon(JComponent component, Icon icon) {
        return null;
    }

    /** A short name to show, such as "Metal". */
    public abstract String getName();

    /** A stable identifier, such as "Metal"; the name may change, this one does not. */
    public abstract String getID();

    /** A line that describes it. */
    public abstract String getDescription();

    /** Whether this look and feel can decorate the windows itself: no, this one cannot. */
    public boolean getSupportsWindowDecorations() {
        return false;
    }

    /** Whether it is the platform's native look and feel. */
    public abstract boolean isNativeLookAndFeel();

    /** Whether this look and feel can be used on this platform. */
    public abstract boolean isSupportedLookAndFeel();

    /** It is called on installing it; there is nothing to prepare. */
    public void initialize() {
    }

    /**
     * This look and feel's table of values.
     *
     * <p>It returns {@code null} unless the subclass builds it. It is not an omission: a look and
     * feel that defines no values of its own uses those that are already in {@link UIManager}, and
     * returning an empty table instead of {@code null} would erase them all on installing.
     *
     * @return the table, or {@code null} if this look and feel does not have one of its own
     */
    public UIDefaults getDefaults() {
        return null;
    }

    /** It is called on uninstalling it; there is nothing to let go of. */
    public void uninitialize() {
    }

    public String toString() {
        return "[" + getDescription() + " - " + getClass().getName() + "]";
    }

    // -- installing the look and feel's values ---------------------------------------------------

    /**
     * It gives the component the foreground and the background from the table, if it does not
     * have them set by hand.
     *
     * <p>"Set by hand" is decided by {@link UIResource}: a colour that is a look and feel
     * resource was set by the previous look and feel and may be overwritten; one that is not was
     * set by the program and is respected. It is the whole logic of these four methods, and it is
     * what keeps changing the look and feel from erasing what the program configured.
     */
    public static void installColors(JComponent c, String defaultBgName,
            String defaultFgName) {
        java.awt.Color bg = c.getBackground();
        if (bg == null || bg instanceof UIResource) {
            c.setBackground(UIManager.getColor(defaultBgName));
        }
        java.awt.Color fg = c.getForeground();
        if (fg == null || fg instanceof UIResource) {
            c.setForeground(UIManager.getColor(defaultFgName));
        }
    }

    /** The same, and also the typeface; see {@link #installColors}. */
    public static void installColorsAndFont(JComponent c, String defaultBgName,
            String defaultFgName, String defaultFontName) {
        java.awt.Font f = c.getFont();
        if (f == null || f instanceof UIResource) {
            c.setFont(UIManager.getFont(defaultFontName));
        }
        installColors(c, defaultBgName, defaultFgName);
    }

    /**
     * It gives it the border from the table, if it does not have one set by hand.
     *
     * @throws NullPointerException if the component is null
     */
    public static void installBorder(JComponent c, String defaultBorderName) {
        javax.swing.border.Border b = c.getBorder();
        if (b == null || b instanceof UIResource) {
            c.setBorder(UIManager.getBorder(defaultBorderName));
        }
    }

    // -- key maps --------------------------------------------------------------------------------

    /**
     * It turns a flat list of pairs into bindings from key to action.
     *
     * <p>The array goes two at a time: a key -- a {@link KeyStroke} or its text -- and the
     * action's name. It is written like that because a look and feel defines a hundred bindings
     * and a literal array is shorter and more readable than a hundred calls.
     *
     * @throws IllegalArgumentException if the array is null or has an odd number of elements
     */
    public static javax.swing.text.JTextComponent.KeyBinding[] makeKeyBindings(
            Object[] keyBindingList) {
        javax.swing.text.JTextComponent.KeyBinding[] rv =
                new javax.swing.text.JTextComponent.KeyBinding[keyBindingList.length / 2];
        for (int i = 0; i < rv.length; i++) {
            Object o = keyBindingList[2 * i];
            KeyStroke keystroke;
            if (o instanceof KeyStroke) {
                keystroke = (KeyStroke) o;
            } else {
                keystroke = KeyStroke.getKeyStroke((String) o);
            }
            String action = (String) keyBindingList[2 * i + 1];
            rv[i] = new javax.swing.text.JTextComponent.KeyBinding(keystroke, action);
        }
        return rv;
    }

    /**
     * A key map built with that flat list; see {@link #makeKeyBindings}.
     *
     * <p>The map that comes out is a look and feel resource, and that matters: it is what allows
     * it to be replaced whole on changing look and feel without touching the bindings the program
     * set.
     */
    public static InputMap makeInputMap(Object[] keys) {
        InputMap retMap = new javax.swing.plaf.InputMapUIResource();
        loadKeyBindings(retMap, keys);
        return retMap;
    }

    /** The same, for the bindings that hold while the window has the focus. */
    public static ComponentInputMap makeComponentInputMap(JComponent c, Object[] keys) {
        ComponentInputMap retMap = new javax.swing.plaf.ComponentInputMapUIResource(c);
        loadKeyBindings(retMap, keys);
        return retMap;
    }

    /**
     * It loads that flat list into a map that already exists.
     *
     * <p>A null list does nothing -- which is what the JDK does --: a look and feel that defines
     * no bindings for a component is not an error.
     */
    public static void loadKeyBindings(InputMap retMap, Object[] keys) {
        if (keys != null) {
            for (int counter = 0; counter < keys.length; counter = counter + 2) {
                Object keyStrokeO = keys[counter];
                KeyStroke key = (keyStrokeO instanceof KeyStroke)
                        ? (KeyStroke) keyStrokeO : KeyStroke.getKeyStroke((String) keyStrokeO);
                retMap.put(key, keys[counter + 1]);
            }
        }
    }

    /**
     * An icon that is loaded only when somebody draws it.
     *
     * <p>The delay matters: a look and feel's table names dozens of icons and a screen uses a few.
     * Loading them all on installing the look and feel would be reading dozens of files for
     * nothing.
     */
    public static Object makeIcon(final Class<?> baseClass, final String gifFile) {
        return new LazyIcon(baseClass, gifFile);
    }

    // -- what comes from the desktop -------------------------------------------------------------

    /**
     * A configuration value from the desktop, or the fallback one if there is none.
     *
     * <p>They are things like the caret's speed or whether the system asks for the shortcuts to be
     * underlined. This library does not consult the desktop, so it always returns the fallback one
     * -- and it says so, instead of inventing a number that looks like the system's.
     */
    public static Object getDesktopPropertyValue(String systemPropertyName,
            Object fallbackValue) {
        Object value = java.awt.Toolkit.getDefaultToolkit()
                .getDesktopProperty(systemPropertyName);
        if (value == null) {
            return fallbackValue;
        }
        if (value instanceof java.awt.Color) {
            return new javax.swing.plaf.ColorUIResource((java.awt.Color) value);
        }
        if (value instanceof java.awt.Font) {
            return new javax.swing.plaf.FontUIResource((java.awt.Font) value);
        }
        return value;
    }

    /**
     * How the user is told they did something invalid.
     *
     * <p>The usual thing is a beep. Here nothing sounds: there is nothing to sound with. A real
     * look and feel overrides it.
     */
    public void provideErrorFeedback(java.awt.Component component) {
        java.awt.Toolkit toolkit = java.awt.Toolkit.getDefaultToolkit();
        if (toolkit != null) {
            toolkit.beep();
        }
    }

    /**
     * The spacing this look and feel recommends between components.
     *
     * <p>Null -- which is the default -- lets {@link LayoutStyle#getInstance} use the usual one;
     * see that class's note.
     */
    public LayoutStyle getLayoutStyle() {
        return null;
    }

    /** The icon that is loaded on the first drawing; see {@link LookAndFeel#makeIcon}. */
    private static class LazyIcon implements Icon, UIResource, java.io.Serializable {

        private final Class<?> baseClass;
        private final String gifFile;
        private Icon icon;

        LazyIcon(Class<?> baseClass, String gifFile) {
            this.baseClass = baseClass;
            this.gifFile = gifFile;
        }

        private Icon load() {
            if (icon == null) {
                java.net.URL url = baseClass.getResource(gifFile);
                icon = (url == null) ? new EmptyIcon() : new ImageIcon(url);
            }
            return icon;
        }

        public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
            load().paintIcon(c, g, x, y);
        }

        public int getIconWidth() {
            return load().getIconWidth();
        }

        public int getIconHeight() {
            return load().getIconHeight();
        }
    }

    /** What is left when the icon's file is not there: nothing, but of size zero. */
    private static class EmptyIcon implements Icon, UIResource, java.io.Serializable {

        public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
        }

        public int getIconWidth() {
            return 0;
        }

        public int getIconHeight() {
            return 0;
        }
    }
}
