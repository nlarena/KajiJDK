package javax.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;

/**
 * The graphical look and feel's global registry: who rules and with what values.
 *
 * <h2>What it keeps</h2>
 *
 * <p>Three things. The current graphical look and feel, its table of values, and the list of
 * auxiliary looks and feels -- those that do not draw but want to learn about everything, such
 * as a screen reader --.
 *
 * <p>Everything is static because a process has one graphical look and feel and not several:
 * two windows of the same application with buttons of a different look would be a mistake, not
 * a feature.
 *
 * <h2>Why almost every method is a shortcut</h2>
 *
 * <p>{@link #getColor}, {@link #getFont} and company are the same as asking
 * {@link #getDefaults}. They are there because the code that uses them uses them a lot, and
 * {@code UIManager.getColor("Button.background")} reads better than the long version.
 *
 * <h2>The auxiliary looks and feels</h2>
 *
 * <p>They are added with {@link #addAuxiliaryLookAndFeel} and from then on each component
 * receives a graphical interface that hands out between the main one and them; see
 * {@code javax.swing.plaf.multi}. The list starts at {@code null} and not empty, and
 * {@link #getAuxiliaryLookAndFeels} returns {@code null} while there is none: it is what allows
 * whoever asks to skip the work of multiplexing in the normal case, which is that there is
 * none.
 *
 * <h2>State in this library</h2>
 *
 * <p>The registry works: a look and feel can be fixed, its values read, auxiliary ones added
 * and the changes listened to. What there is not is any graphical look and feel implemented, so
 * {@link #setLookAndFeel(String)} with any of the names {@link #getInstalledLookAndFeels}
 * returns fails on loading the class. Those names are the JDK's and are correct as names; what
 * is missing are the classes.
 *
 * @since 1.2
 */
public class UIManager implements Serializable {

    private static final long serialVersionUID = -5547977484831201933L;

    private static final PropertyChangeSupport CHANGES = new PropertyChangeSupport(UIManager.class);

    private static LookAndFeelInfo[] installed = {
        new LookAndFeelInfo("Metal", "javax.swing.plaf.metal.MetalLookAndFeel"),
        new LookAndFeelInfo("Nimbus", "javax.swing.plaf.nimbus.NimbusLookAndFeel"),
        new LookAndFeelInfo("CDE/Motif", "com.sun.java.swing.plaf.motif.MotifLookAndFeel"),
        new LookAndFeelInfo("Windows", "com.sun.java.swing.plaf.windows.WindowsLookAndFeel"),
        new LookAndFeelInfo("Windows Classic",
                "com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel"),
    };

    private static LookAndFeel current;
    private static UIDefaults values = new UIDefaults();
    private static List<LookAndFeel> auxiliary;

    /** A registry; everything useful is static. */
    public UIManager() {
    }

    /**
     * The graphical looks and feels that can be chosen.
     *
     * @return a new array with those there are
     */
    public static LookAndFeelInfo[] getInstalledLookAndFeels() {
        return installed.clone();
    }

    /**
     * It replaces the list of available looks and feels.
     *
     * @param infos the looks and feels
     * @throws NullPointerException if the array or any of its elements is {@code null}
     */
    public static void setInstalledLookAndFeels(LookAndFeelInfo[] infos) {
        if (infos == null) {
            throw new NullPointerException("infos");
        }
        for (int i = 0; i < infos.length; i++) {
            if (infos[i] == null) {
                throw new NullPointerException("infos[" + i + "]");
            }
        }
        installed = infos.clone();
    }

    /**
     * It adds a look and feel to the list of available ones.
     *
     * @param info the look and feel
     */
    public static void installLookAndFeel(LookAndFeelInfo info) {
        final LookAndFeelInfo[] added = new LookAndFeelInfo[installed.length + 1];
        System.arraycopy(installed, 0, added, 0, installed.length);
        added[installed.length] = info;
        installed = added;
    }

    /**
     * It adds a look and feel to the list of available ones.
     *
     * @param name the name to show
     * @param className the class that implements it
     */
    public static void installLookAndFeel(String name, String className) {
        installLookAndFeel(new LookAndFeelInfo(name, className));
    }

    /**
     * The current graphical look and feel.
     *
     * @return the look and feel, or {@code null} if none was fixed
     */
    public static LookAndFeel getLookAndFeel() {
        return current;
    }

    /**
     * It builds that class's graphical look and feel.
     *
     * @param className the class
     * @return the look and feel
     * @throws UnsupportedLookAndFeelException if it could not be built
     */
    public static LookAndFeel createLookAndFeel(String className)
            throws UnsupportedLookAndFeelException {
        try {
            return (LookAndFeel) Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new UnsupportedLookAndFeelException(className + ": " + e);
        }
    }

    /**
     * It fixes the graphical look and feel.
     *
     * <p>The order is what matters: first the previous one is uninstalled, then the new one is
     * installed, then its table of values is taken, and only at the end is notice given. Giving
     * notice earlier would make whoever listens redraw with the old table.
     *
     * @param newLookAndFeel the look and feel, or {@code null} to stop having one
     * @throws UnsupportedLookAndFeelException if that look and feel does not serve on this
     *     platform
     */
    public static void setLookAndFeel(LookAndFeel newLookAndFeel)
            throws UnsupportedLookAndFeelException {
        if (newLookAndFeel != null && !newLookAndFeel.isSupportedLookAndFeel()) {
            throw new UnsupportedLookAndFeelException(newLookAndFeel + " is not supported");
        }
        final LookAndFeel previous = current;
        if (previous != null) {
            previous.uninitialize();
        }
        current = newLookAndFeel;
        if (newLookAndFeel != null) {
            newLookAndFeel.initialize();
            values = newLookAndFeel.getDefaults();
        } else {
            values = new UIDefaults();
        }
        CHANGES.firePropertyChange("lookAndFeel", previous, newLookAndFeel);
    }

    /**
     * It fixes the graphical look and feel by its class name.
     *
     * @param className the class
     * @throws ClassNotFoundException if the class is not there
     * @throws InstantiationException if it could not be built
     * @throws IllegalAccessException if the constructor could not be accessed
     * @throws UnsupportedLookAndFeelException if that look and feel does not serve on this
     *     platform
     */
    public static void setLookAndFeel(String className) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
        final Class<?> c = Class.forName(className);
        final Object o;
        try {
            o = c.getDeclaredConstructor().newInstance();
        } catch (InstantiationException e) {
            throw e;
        } catch (IllegalAccessException e) {
            throw e;
        } catch (Exception e) {
            throw new InstantiationException(className + ": " + e);
        }
        setLookAndFeel((LookAndFeel) o);
    }

    /**
     * This platform's own graphical look and feel.
     *
     * @return the class's name
     */
    public static String getSystemLookAndFeelClassName() {
        final String so = System.getProperty("os.name");
        if (so != null && so.startsWith("Windows")) {
            return "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
        }
        return getCrossPlatformLookAndFeelClassName();
    }

    /**
     * The graphical look and feel that looks the same on every platform.
     *
     * @return the class's name
     */
    public static String getCrossPlatformLookAndFeelClassName() {
        return "javax.swing.plaf.metal.MetalLookAndFeel";
    }

    /**
     * The table of values in use.
     *
     * @return the table
     */
    public static UIDefaults getDefaults() {
        return values;
    }

    /**
     * The current graphical look and feel's table of values.
     *
     * @return the table
     */
    public static UIDefaults getLookAndFeelDefaults() {
        return values;
    }

    /**
     * That key's typeface.
     *
     * @param key the key
     * @return the typeface, or {@code null}
     */
    public static Font getFont(Object key) {
        return getDefaults().getFont(key);
    }

    /**
     * That key's typeface in that language.
     *
     * @param key the key
     * @param l the language
     * @return the typeface, or {@code null}
     */
    public static Font getFont(Object key, Locale l) {
        return getDefaults().getFont(key, l);
    }

    /**
     * That key's colour.
     *
     * @param key the key
     * @return the colour, or {@code null}
     */
    public static Color getColor(Object key) {
        return getDefaults().getColor(key);
    }

    /**
     * That key's colour in that language.
     *
     * @param key the key
     * @param l the language
     * @return the colour, or {@code null}
     */
    public static Color getColor(Object key, Locale l) {
        return getDefaults().getColor(key, l);
    }

    /**
     * That key's icon.
     *
     * @param key the key
     * @return the icon, or {@code null}
     */
    public static Icon getIcon(Object key) {
        return getDefaults().getIcon(key);
    }

    /**
     * That key's icon in that language.
     *
     * @param key the key
     * @param l the language
     * @return the icon, or {@code null}
     */
    public static Icon getIcon(Object key, Locale l) {
        return getDefaults().getIcon(key, l);
    }

    /**
     * That key's border.
     *
     * @param key the key
     * @return the border, or {@code null}
     */
    public static Border getBorder(Object key) {
        return getDefaults().getBorder(key);
    }

    /**
     * That key's border in that language.
     *
     * @param key the key
     * @param l the language
     * @return the border, or {@code null}
     */
    public static Border getBorder(Object key, Locale l) {
        return getDefaults().getBorder(key, l);
    }

    /**
     * That key's text.
     *
     * @param key the key
     * @return the text, or {@code null}
     */
    public static String getString(Object key) {
        return getDefaults().getString(key);
    }

    /**
     * That key's text in that language.
     *
     * @param key the key
     * @param l the language
     * @return the text, or {@code null}
     */
    public static String getString(Object key, Locale l) {
        return getDefaults().getString(key, l);
    }

    /**
     * That key's integer number.
     *
     * @param key the key
     * @return the number, or zero
     */
    public static int getInt(Object key) {
        return getDefaults().getInt(key);
    }

    /**
     * That key's integer number in that language.
     *
     * @param key the key
     * @param l the language
     * @return the number, or zero
     */
    public static int getInt(Object key, Locale l) {
        return getDefaults().getInt(key, l);
    }

    /**
     * That key's truth value.
     *
     * @param key the key
     * @return the value, or false
     */
    public static boolean getBoolean(Object key) {
        return getDefaults().getBoolean(key);
    }

    /**
     * That key's truth value in that language.
     *
     * @param key the key
     * @param l the language
     * @return the value, or false
     */
    public static boolean getBoolean(Object key, Locale l) {
        return getDefaults().getBoolean(key, l);
    }

    /**
     * That key's margins.
     *
     * @param key the key
     * @return the margins, or {@code null}
     */
    public static Insets getInsets(Object key) {
        return getDefaults().getInsets(key);
    }

    /**
     * That key's margins in that language.
     *
     * @param key the key
     * @param l the language
     * @return the margins, or {@code null}
     */
    public static Insets getInsets(Object key, Locale l) {
        return getDefaults().getInsets(key, l);
    }

    /**
     * That key's size.
     *
     * @param key the key
     * @return the size, or {@code null}
     */
    public static Dimension getDimension(Object key) {
        return getDefaults().getDimension(key);
    }

    /**
     * That key's size in that language.
     *
     * @param key the key
     * @param l the language
     * @return the size, or {@code null}
     */
    public static Dimension getDimension(Object key, Locale l) {
        return getDefaults().getDimension(key, l);
    }

    /**
     * That key's value.
     *
     * @param key the key
     * @return the value, or {@code null}
     */
    public static Object get(Object key) {
        return getDefaults().get(key);
    }

    /**
     * That key's value in that language.
     *
     * @param key the key
     * @param l the language
     * @return the value, or {@code null}
     */
    public static Object get(Object key, Locale l) {
        return getDefaults().get(key, l);
    }

    /**
     * It keeps a value.
     *
     * @param key the key
     * @param value the value
     * @return the previous value, or {@code null}
     */
    public static Object put(Object key, Object value) {
        return getDefaults().put(key, value);
    }

    /**
     * The graphical interface that falls to that component.
     *
     * @param target the component
     * @return the graphical interface, or {@code null}
     */
    public static ComponentUI getUI(JComponent target) {
        return getDefaults().getUI(target);
    }

    /**
     * It adds an auxiliary look and feel.
     *
     * <p>An auxiliary one does not draw: it receives the same calls as the main one so as to be
     * able to learn about things. Screen readers and contextual helps live on that.
     *
     * @param laf the auxiliary look and feel
     */
    public static void addAuxiliaryLookAndFeel(LookAndFeel laf) {
        if (laf == null) {
            return;
        }
        if (!laf.isSupportedLookAndFeel()) {
            return;
        }
        synchronized (UIManager.class) {
            if (auxiliary == null) {
                auxiliary = new ArrayList<LookAndFeel>();
            }
            if (!auxiliary.contains(laf)) {
                auxiliary.add(laf);
                laf.initialize();
            }
        }
    }

    /**
     * It removes an auxiliary look and feel.
     *
     * @param laf the auxiliary look and feel
     * @return true if it was there
     */
    public static boolean removeAuxiliaryLookAndFeel(LookAndFeel laf) {
        synchronized (UIManager.class) {
            if (auxiliary == null || !auxiliary.remove(laf)) {
                return false;
            }
            laf.uninitialize();
            if (auxiliary.isEmpty()) {
                // It goes back to null and is not left empty: `getAuxiliaryLookAndFeels` promises
                // null
                                // when there is none, and that is what allows whoever asks to skip
                                // the multiplexing in the normal case.
                auxiliary = null;
            }
            return true;
        }
    }

    /**
     * The auxiliary looks and feels.
     *
     * @return a new array, or {@code null} if there is none
     */
    public static LookAndFeel[] getAuxiliaryLookAndFeels() {
        synchronized (UIManager.class) {
            if (auxiliary == null || auxiliary.isEmpty()) {
                return null;
            }
            return auxiliary.toArray(new LookAndFeel[auxiliary.size()]);
        }
    }

    /**
     * It registers a listener of the registry's changes.
     *
     * @param listener the listener
     */
    public static void addPropertyChangeListener(PropertyChangeListener listener) {
        CHANGES.addPropertyChangeListener(listener);
    }

    /**
     * It removes a listener.
     *
     * @param listener the listener
     */
    public static void removePropertyChangeListener(PropertyChangeListener listener) {
        CHANGES.removePropertyChangeListener(listener);
    }

    /**
     * The registered listeners.
     *
     * @return the listeners
     */
    public static PropertyChangeListener[] getPropertyChangeListeners() {
        return CHANGES.getPropertyChangeListeners();
    }

    /**
     * An available graphical look and feel's name and class.
     *
     * <p>It keeps the <strong>class's name</strong> and not the class: the list is built at
     * start-up and loading every look and feel in order to be able to offer them would cost much
     * more than offering those nobody is going to choose is worth.
     *
     * @since 1.2
     */
    public static class LookAndFeelInfo {

        private final String name;
        private final String className;

        /**
         * With that name and that class.
         *
         * @param name the name to show
         * @param className the class that implements it
         */
        public LookAndFeelInfo(String name, String className) {
            this.name = name;
            this.className = className;
        }

        /**
         * The name to show.
         *
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * The class that implements it.
         *
         * @return the class's name
         */
        public String getClassName() {
            return className;
        }

        /**
         * A description, for the record.
         *
         * @return the name and the class
         */
        @Override
        public String toString() {
            return getClass().getName() + "[" + getName() + " " + getClassName() + "]";
        }
    }
}
