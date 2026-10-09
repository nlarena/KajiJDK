package javax.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;

/**
 * The table where a graphical look and feel keeps all its values.
 *
 * <h2>What there is inside</h2>
 *
 * <p>Colours, typefaces, borders, icons, margins and -- most importantly -- which class
 * implements each component's graphical interface. The keys are texts such as
 * {@code "Button.background"} or {@code "ButtonUI"}.
 *
 * <h2>The three kinds of value</h2>
 *
 * <p>A value may be the object directly, or one of two wrappers that make it only when it is
 * asked for.
 *
 * <p>{@link LazyValue} makes it <strong>once</strong> and replaces the entry with the result.
 * It exists for a concrete reason: a graphical look and feel defines thousands of entries and a
 * session uses a few. Building every icon and every typeface at start-up would be paying for
 * what is not going to be looked at.
 *
 * <p>{@link ActiveValue} makes it <strong>each time</strong> and it is not kept. It is for what
 * cannot be shared: if two components receive the same object with state, one overwrites the
 * other's state.
 *
 * <p>The difference between the two is not of performance but of correctness, and confusing
 * them gives errors that appear only when there are two components of the same type on the
 * screen.
 *
 * <h2>The language</h2>
 *
 * <p>Each operation has a version with a {@link Locale}. The texts the user sees -- the names
 * of a dialog's buttons, for instance -- come from here, and an application may have two
 * windows open in two languages. Without the parameter there would be a single language per
 * process.
 *
 * <p>The texts that are not in the table are looked up in the {@link ResourceBundle}s that were
 * added, from the last to the first: the one that is added later covers the previous one, which
 * is what allows an application to change one text without rewriting the whole bundle.
 *
 * @since 1.2
 */
public class UIDefaults extends Hashtable<Object, Object> {

    private static final long serialVersionUID = 7341222528856548117L;

    private final PropertyChangeSupport changes = new PropertyChangeSupport(this);
    private final List<String> packages = new ArrayList<String>();

    private Locale defaultLocale = Locale.getDefault();

    /** An empty table. */
    public UIDefaults() {
        super(700, 0.75f);
    }

    /**
     * An empty table with that capacity.
     *
     * @param initialCapacity how many entries are expected
     * @param loadFactor how full it gets before enlarging itself
     */
    public UIDefaults(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    /**
     * A table with those key-value pairs.
     *
     * <p>The array goes flat: key, value, key, value. It is awkward to read and it is how every
     * graphical look and feel's definition is written, which are lists of hundreds of pairs.
     *
     * @param keyValueList the pairs, alternating
     */
    public UIDefaults(Object[] keyValueList) {
        super(keyValueList.length / 2 + 1, 0.75f);
        putDefaults(keyValueList);
    }

    /**
     * That key's value, with the default language.
     *
     * @param key the key
     * @return the value, or {@code null}
     */
    @Override
    public Object get(Object key) {
        return get(key, getDefaultLocale());
    }

    /**
     * That key's value in that language.
     *
     * <p>Here is where the two wrappers are resolved: a {@link LazyValue} is made and kept in its
     * place, and an {@link ActiveValue} is made and not kept.
     *
     * @param key the key
     * @param l the language, or {@code null} for the table's
     * @return the value, or {@code null}
     */
    public Object get(Object key, Locale l) {
        Object v = super.get(key);
        if (v == null) {
            v = fromPackages(key, l);
        }
        if (v instanceof LazyValue) {
            final Object done = ((LazyValue) v).createValue(this);
            // The entry is replaced, which is what makes it be made only once. If the maker
            // returned
                        // null the key is removed: leaving the wrapper would make it be attempted
                        // on every query, without success, for ever.
            if (done == null) {
                super.remove(key);
            } else {
                super.put(key, done);
            }
            return done;
        }
        if (v instanceof ActiveValue) {
            return ((ActiveValue) v).createValue(this);
        }
        return v;
    }

    /**
     * It keeps a value and gives notice of the change.
     *
     * <p>With a {@code null} value the key is removed. It is not the same as keeping
     * {@code null}: a hash table of this kind does not admit null values, and besides "there is
     * no value" is precisely what is meant.
     *
     * @param key the key
     * @param value the value, or {@code null} to remove it
     * @return the previous value, or {@code null}
     */
    @Override
    public Object put(Object key, Object value) {
        final Object old = value == null ? super.remove(key) : super.put(key, value);
        if (key instanceof String) {
            changes.firePropertyChange((String) key, old, value);
        }
        return old;
    }

    /**
     * It keeps several pairs at once.
     *
     * <p>The change notices all come out at the end, not one per pair: whoever listens usually
     * redraws, and with one notice per entry it would redraw hundreds of times for the same
     * change.
     *
     * @param keyValueList the pairs, alternating
     */
    public void putDefaults(Object[] keyValueList) {
        for (int i = 0; i < keyValueList.length - 1; i += 2) {
            final Object k = keyValueList[i];
            final Object v = keyValueList[i + 1];
            if (v == null) {
                super.remove(k);
            } else {
                super.put(k, v);
            }
        }
        changes.firePropertyChange("UIDefaults", null, null);
    }

    /**
     * That key's typeface.
     *
     * @param key the key
     * @return the typeface, or {@code null}
     */
    public Font getFont(Object key) {
        return getFont(key, getDefaultLocale());
    }

    /**
     * That key's typeface in that language.
     *
     * @param key the key
     * @param l the language
     * @return the typeface, or {@code null}
     */
    public Font getFont(Object key, Locale l) {
        final Object v = get(key, l);
        return v instanceof Font ? (Font) v : null;
    }

    /**
     * That key's colour.
     *
     * @param key the key
     * @return the colour, or {@code null}
     */
    public Color getColor(Object key) {
        return getColor(key, getDefaultLocale());
    }

    /**
     * That key's colour in that language.
     *
     * @param key the key
     * @param l the language
     * @return the colour, or {@code null}
     */
    public Color getColor(Object key, Locale l) {
        final Object v = get(key, l);
        return v instanceof Color ? (Color) v : null;
    }

    /**
     * That key's icon.
     *
     * @param key the key
     * @return the icon, or {@code null}
     */
    public Icon getIcon(Object key) {
        return getIcon(key, getDefaultLocale());
    }

    /**
     * That key's icon in that language.
     *
     * @param key the key
     * @param l the language
     * @return the icon, or {@code null}
     */
    public Icon getIcon(Object key, Locale l) {
        final Object v = get(key, l);
        return v instanceof Icon ? (Icon) v : null;
    }

    /**
     * That key's border.
     *
     * @param key the key
     * @return the border, or {@code null}
     */
    public Border getBorder(Object key) {
        return getBorder(key, getDefaultLocale());
    }

    /**
     * That key's border in that language.
     *
     * @param key the key
     * @param l the language
     * @return the border, or {@code null}
     */
    public Border getBorder(Object key, Locale l) {
        final Object v = get(key, l);
        return v instanceof Border ? (Border) v : null;
    }

    /**
     * That key's text.
     *
     * @param key the key
     * @return the text, or {@code null}
     */
    public String getString(Object key) {
        return getString(key, getDefaultLocale());
    }

    /**
     * That key's text in that language.
     *
     * @param key the key
     * @param l the language
     * @return the text, or {@code null}
     */
    public String getString(Object key, Locale l) {
        final Object v = get(key, l);
        return v instanceof String ? (String) v : null;
    }

    /**
     * That key's integer number.
     *
     * @param key the key
     * @return the number, or zero if there is none
     */
    public int getInt(Object key) {
        return getInt(key, getDefaultLocale());
    }

    /**
     * That key's integer number in that language.
     *
     * @param key the key
     * @param l the language
     * @return the number, or zero if there is none
     */
    public int getInt(Object key, Locale l) {
        final Object v = get(key, l);
        return v instanceof Integer ? ((Integer) v).intValue() : 0;
    }

    /**
     * That key's truth value.
     *
     * @param key the key
     * @return the value, or false if there is none
     */
    public boolean getBoolean(Object key) {
        return getBoolean(key, getDefaultLocale());
    }

    /**
     * That key's truth value in that language.
     *
     * @param key the key
     * @param l the language
     * @return the value, or false if there is none
     */
    public boolean getBoolean(Object key, Locale l) {
        final Object v = get(key, l);
        return v instanceof Boolean && ((Boolean) v).booleanValue();
    }

    /**
     * That key's margins.
     *
     * @param key the key
     * @return the margins, or {@code null}
     */
    public Insets getInsets(Object key) {
        return getInsets(key, getDefaultLocale());
    }

    /**
     * That key's margins in that language.
     *
     * @param key the key
     * @param l the language
     * @return the margins, or {@code null}
     */
    public Insets getInsets(Object key, Locale l) {
        final Object v = get(key, l);
        return v instanceof Insets ? (Insets) v : null;
    }

    /**
     * That key's size.
     *
     * @param key the key
     * @return the size, or {@code null}
     */
    public Dimension getDimension(Object key) {
        return getDimension(key, getDefaultLocale());
    }

    /**
     * That key's size in that language.
     *
     * @param key the key
     * @param l the language
     * @return the size, or {@code null}
     */
    public Dimension getDimension(Object key, Locale l) {
        final Object v = get(key, l);
        return v instanceof Dimension ? (Dimension) v : null;
    }

    /**
     * The class that implements that component's graphical interface.
     *
     * @param uiClassID the identifier, such as {@code "ButtonUI"}
     * @param uiClassLoader the loader to look it up with, or {@code null}
     * @return the class, or {@code null} if it could not be found
     */
    @SuppressWarnings("unchecked")
    public Class<? extends ComponentUI> getUIClass(String uiClassID, ClassLoader uiClassLoader) {
        final Object v = get(uiClassID);
        if (!(v instanceof String)) {
            return null;
        }
        final String name = (String) v;
        // The resolved class is kept under its own name. It is not only for speed: the table is
                // public, and the JDK leaves the `Class` there so that whoever wants may look at
                // it.
        final Object cached = get(name);
        if (cached instanceof Class) {
            return (Class<? extends ComponentUI>) cached;
        }
        // With no loader the thread's context one is used, not this class's. The difference is not
                // theoretical: the graphical interface's class is brought by the graphical look and
                // feel, which lives where the application lives, and this library's loader does not
                // reach there.
        ClassLoader cl = uiClassLoader;
        if (cl == null) {
            cl = Thread.currentThread().getContextClassLoader();
        }
        try {
            final Class<?> c = cl == null ? Class.forName(name) : cl.loadClass(name);
            put(name, c);
            // The cast goes unchecked, and that is on purpose: the only thing asked of this class
            // is
                        // to have a static `createUI`. Requiring it also to be a ComponentUI would
                        // leave the factories out, which is a legitimate -- and used -- way of
                        // writing a look and feel.
            return (Class<? extends ComponentUI>) c;
        } catch (ClassNotFoundException e) {
            return null;
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * The class that implements that component's graphical interface.
     *
     * @param uiClassID the identifier
     * @return the class, or {@code null}
     */
    public Class<? extends ComponentUI> getUIClass(String uiClassID) {
        return getUIClass(uiClassID, null);
    }

    /**
     * Notice that a graphical interface could not be found or built.
     *
     * <p>It may be redefined in order to record it somewhere else. That it throws nothing is on
     * purpose: a component with no graphical interface looks wrong, and an application that falls
     * over looks worse.
     *
     * @param msg what happened
     */
    protected void getUIError(String msg) {
        System.err.println("UIDefaults.getUI() failed: " + msg);
    }

    /**
     * It builds that component's graphical interface.
     *
     * <p>It looks the class up by the identifier the component declares and asks it for its
     * {@code createUI}. That the making goes through a static method and not through the
     * constructor is what allows an implementation to return an instance shared between
     * components, which is what almost all of them do.
     *
     * @param target the component
     * @return the graphical interface, or {@code null} if it could not be done
     */
    public ComponentUI getUI(JComponent target) {
        final String id = target.getUIClassID();
        final Class<? extends ComponentUI> clazz = getUIClass(id, null);
        if (clazz == null) {
            getUIError("no class for " + id);
            return null;
        }
        try {
            final Method m = clazz.getMethod("createUI", new Class<?>[] {JComponent.class});
            return (ComponentUI) m.invoke(null, new Object[] {target});
        } catch (Exception e) {
            getUIError("could not create " + clazz.getName() + ": " + e);
            return null;
        }
    }

    /**
     * It registers a listener of the table's changes.
     *
     * @param listener the listener
     */
    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        changes.addPropertyChangeListener(listener);
    }

    /**
     * It removes a listener.
     *
     * @param listener the listener
     */
    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        changes.removePropertyChangeListener(listener);
    }

    /**
     * The registered listeners.
     *
     * @return the listeners
     */
    public synchronized PropertyChangeListener[] getPropertyChangeListeners() {
        return changes.getPropertyChangeListeners();
    }

    /**
     * It gives notice of a change.
     *
     * @param propertyName the key that changed
     * @param oldValue what was there
     * @param newValue what is there
     */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        changes.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * It adds a bundle of texts to look up what is not in the table in.
     *
     * @param bundleName the bundle's name
     */
    public synchronized void addResourceBundle(String bundleName) {
        if (bundleName != null && !packages.contains(bundleName)) {
            packages.add(bundleName);
        }
    }

    /**
     * It removes a bundle of texts.
     *
     * @param bundleName the bundle's name
     */
    public synchronized void removeResourceBundle(String bundleName) {
        packages.remove(bundleName);
    }

    /**
     * It fixes the language that is used when none is passed.
     *
     * @param l the language
     */
    public void setDefaultLocale(Locale l) {
        defaultLocale = l;
    }

    /**
     * The language that is used when none is passed.
     *
     * @return the language
     */
    public Locale getDefaultLocale() {
        return defaultLocale;
    }

    /**
     * It looks the key up in the bundles of texts, from the last added to the first.
     *
     * <p>From the last to the first so that the one that is added later covers the previous one:
     * it is what allows an application to change one text without rewriting the whole bundle.
     */
    private synchronized Object fromPackages(Object key, Locale l) {
        if (!(key instanceof String)) {
            return null;
        }
        for (int i = packages.size() - 1; i >= 0; i--) {
            try {
                final ResourceBundle b = ResourceBundle.getBundle(packages.get(i),
                        l == null ? getDefaultLocale() : l);
                return b.getObject((String) key);
            } catch (MissingResourceException e) {
                // Neither the bundle nor the key: it goes on with the previous one. That it is
                // missing is
                                // normal -- that is why there are several -- and it only matters if
                                // it is in none.
                continue;
            }
        }
        return null;
    }

    /**
     * A value that is made each time it is asked for and is not kept.
     *
     * <p>It is for what cannot be shared between components: if two receive the same object with
     * state, one overwrites the other's state.
     *
     * @since 1.2
     */
    public interface ActiveValue {

        /**
         * It makes the value.
         *
         * @param table the table that asks for it
         * @return the value
         */
        Object createValue(UIDefaults table);
    }

    /**
     * A value that is made the first time it is asked for and afterwards stays kept.
     *
     * <p>A graphical look and feel defines thousands of entries and a session uses a few: building
     * them all at start-up would be paying for what is not going to be looked at.
     *
     * @since 1.2
     */
    public interface LazyValue {

        /**
         * It makes the value.
         *
         * @param table the table that asks for it
         * @return the value
         */
        Object createValue(UIDefaults table);
    }

    /**
     * A {@link LazyValue} that makes its value by calling a method by reflection.
     *
     * <p>It serves to name in a table of data something that has to be built with code, without
     * the table having to load the class in order to be able to name it. That is the part that
     * matters: loading it would be precisely what one wants to postpone.
     *
     * @since 1.2
     */
    public static class ProxyLazyValue implements LazyValue {

        private final String className;
        private final String methodName;
        private final Object[] args;

        /**
         * It builds with that class's no-argument constructor.
         *
         * @param c the class's name
         */
        public ProxyLazyValue(String c) {
            this(c, (String) null, null);
        }

        /**
         * It calls that static method with no arguments.
         *
         * @param c the class's name
         * @param m the method's name
         */
        public ProxyLazyValue(String c, String m) {
            this(c, m, null);
        }

        /**
         * It builds with the constructor that accepts those arguments.
         *
         * @param c the class's name
         * @param o the arguments
         */
        public ProxyLazyValue(String c, Object[] o) {
            this(c, null, o);
        }

        /**
         * It calls that static method with those arguments.
         *
         * @param c the class's name
         * @param m the method's name, or {@code null} for the constructor
         * @param o the arguments
         */
        public ProxyLazyValue(String c, String m, Object[] o) {
            this.className = c;
            this.methodName = m;
            this.args = o == null ? null : o.clone();
        }

        /**
         * It makes the value.
         *
         * @param table the table that asks for it
         * @return the value, or {@code null} if it could not be made
         */
        @Override
        public Object createValue(UIDefaults table) {
            try {
                final Class<?> c = Class.forName(className);
                final Class<?>[] types = typesOf(args);
                if (methodName == null) {
                    return c.getConstructor(types).newInstance(args == null ? new Object[0] : args);
                }
                return c.getMethod(methodName, types)
                        .invoke(null, args == null ? new Object[0] : args);
            } catch (Exception e) {
                return null;
            }
        }

        private static Class<?>[] typesOf(Object[] o) {
            if (o == null) {
                return new Class<?>[0];
            }
            final Class<?>[] t = new Class<?>[o.length];
            for (int i = 0; i < o.length; i++) {
                t[i] = o[i] == null ? Object.class : o[i].getClass();
            }
            return t;
        }
    }

    /**
     * A {@link LazyValue} that builds a key map.
     *
     * <p>A graphical look and feel's keyboard shortcuts are hundreds and almost none is used in a
     * given session; building them on demand is the difference between starting fast and not.
     *
     * @since 1.3
     */
    public static class LazyInputMap implements LazyValue {

        private final Object[] bindings;

        /**
         * With those key and action pairs, alternating.
         *
         * @param bindings the pairs
         */
        public LazyInputMap(Object[] bindings) {
            this.bindings = bindings == null ? null : bindings.clone();
        }

        /**
         * It builds the map.
         *
         * @param table the table that asks for it
         * @return the map, or {@code null} if there are no bindings
         */
        @Override
        public Object createValue(UIDefaults table) {
            if (bindings == null) {
                return null;
            }
            final InputMap map = new InputMap();
            for (int i = 0; i < bindings.length - 1; i += 2) {
                // The key may come already resolved or as text: a graphical look and feel's table
                // is
                                // written with texts -- "ctrl C" -- because that is how it reads,
                                // and they are resolved when building it.
                final Object k = bindings[i];
                final KeyStroke key = k instanceof KeyStroke
                        ? (KeyStroke) k : KeyStroke.getKeyStroke(String.valueOf(k));
                if (key != null) {
                    map.put(key, bindings[i + 1]);
                }
            }
            return map;
        }
    }
}
