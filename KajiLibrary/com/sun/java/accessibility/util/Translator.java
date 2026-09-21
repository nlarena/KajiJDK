package com.sun.java.accessibility.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeListener;
import java.util.Locale;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleComponent;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleStateSet;

/**
 * It puts an accessible facade on a component that does not have one.
 *
 * <h2>The problem it resolves</h2>
 *
 * <p>Accessibility is optional: a component collaborates by implementing {@link Accessible}.
 * Swing's do; the old AWT ones and those written by third parties, many times do not -- and
 * then a screen reader sees nothing of them.
 *
 * <p>This class wraps one of those and derives what it can from what it <em>does</em> have: the
 * position, the size, whether it is enabled, whether it is seen. It is poor information
 * compared with the one a component that collaborates gives, and it is infinitely better than
 * nothing.
 *
 * <h2>How the translation is chosen</h2>
 *
 * <p>{@link #getAccessible} returns the object just as it is if it is already
 * {@link Accessible} -- there is nothing to translate -- and wraps it only if it is not.
 * {@link #getTranslatorClass} looks for whether there is a subclass specialized for that type,
 * which is how support for a known component is added without touching this class.
 *
 * <h2>What it cannot invent</h2>
 *
 * <p>{@link #getAccessibleRole} returns {@link AccessibleRole#UNKNOWN} and
 * {@link #getAccessibleName} comes out of the component's name. There is no way of deducing
 * that a grey rectangle is a button, and saying that it is would be worse than saying that it
 * is not known: a screen reader would announce a control that cannot be activated.
 */
public class Translator extends AccessibleContext implements Accessible, AccessibleComponent {

    /** The wrapped object. */
    protected Object source;

    /** With no source yet; it has to be given one with {@link #setSource}. */
    public Translator() {
    }

    /** Wrapping that object. */
    public Translator(Object o) {
        this.source = o;
    }

    /**
     * The subclass of {@code Translator} specialized for that type, or {@code null}.
     *
     * <p>It looks by a convention of name in this very package. Returning {@code null} -- the
     * normal thing -- means that this generic class is used.
     */
    protected static Class<?> getTranslatorClass(Class<?> c) {
        if (c == null) {
            return null;
        }
        try {
            return Class.forName("com.sun.java.accessibility.util." + c.getSimpleName()
                    + "Translator");
        } catch (ClassNotFoundException e) {
            return getTranslatorClass(c.getSuperclass());
        }
    }

    /**
     * The object seen as {@link Accessible}: the same one if it already was, or wrapped if not.
     *
     * @return {@code null} if {@code o} is {@code null}
     */
    public static Accessible getAccessible(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Accessible) {
            return (Accessible) o;
        }
        Class<?> specialized = getTranslatorClass(o.getClass());
        if (specialized != null) {
            try {
                Translator t = (Translator) specialized.getDeclaredConstructor().newInstance();
                t.setSource(o);
                return t;
            } catch (Exception e) {
                // A specialized translation that cannot be built does not invalidate the generic
                                // one: worse than a poor facade is none.
                return new Translator(o);
            }
        }
        return new Translator(o);
    }

    /** The wrapped object. */
    public Object getSource() {
        return this.source;
    }

    /** It changes the wrapped object. */
    public void setSource(Object o) {
        this.source = o;
    }

    /**
     * By the wrapped source, not by identity.
     *
     * <p>Two translators of the same component represent the same thing, and since they are
     * created on the fly on each query, comparing them by identity would always give different.
     */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Translator)) {
            return false;
        }
        Object other = ((Translator) o).getSource();
        return this.source == null ? other == null : this.source.equals(other);
    }

    public int hashCode() {
        return this.source == null ? 0 : this.source.hashCode();
    }

    /** This very class: it is at once the facade and its context. */
    public AccessibleContext getAccessibleContext() {
        return this;
    }

    private Component component() {
        return this.source instanceof Component ? (Component) this.source : null;
    }

    /** The component's name, which is the only thing there is to take it from. */
    public String getAccessibleName() {
        Component c = component();
        return c == null ? null : c.getName();
    }

    /** It changes the component's name. */
    public void setAccessibleName(String s) {
        Component c = component();
        if (c != null) {
            c.setName(s);
        }
    }

    /** {@code null}: a component that does not collaborate has no description to give. */
    public String getAccessibleDescription() {
        return null;
    }

    /** It does nothing: there is nowhere to keep it. */
    public void setAccessibleDescription(String s) {
    }

    /** {@link AccessibleRole#UNKNOWN}; see the class note about why it is not guessed. */
    public AccessibleRole getAccessibleRole() {
        return AccessibleRole.UNKNOWN;
    }

    /** The state that may be derived from the component: enabled, visible, with focus. */
    public AccessibleStateSet getAccessibleStateSet() {
        AccessibleStateSet s = new AccessibleStateSet();
        Component c = component();
        if (c == null) {
            return s;
        }
        if (c.isEnabled()) {
            s.add(javax.accessibility.AccessibleState.ENABLED);
        }
        if (c.isVisible()) {
            s.add(javax.accessibility.AccessibleState.VISIBLE);
        }
        if (c.isShowing()) {
            s.add(javax.accessibility.AccessibleState.SHOWING);
        }
        if (c.isFocusOwner()) {
            s.add(javax.accessibility.AccessibleState.FOCUSED);
        }
        return s;
    }

    /** The parent, also translated if it is needed. */
    public Accessible getAccessibleParent() {
        Component c = component();
        return c == null ? null : getAccessible(c.getParent());
    }

    /** The position among the siblings, or {@code -1}. */
    public int getAccessibleIndexInParent() {
        Component c = component();
        if (c == null || c.getParent() == null) {
            return -1;
        }
        Component[] siblings = c.getParent().getComponents();
        for (int i = 0; i < siblings.length; i++) {
            if (siblings[i] == c) {
                return i;
            }
        }
        return -1;
    }

    /** How many children it has, if it is a container. */
    public int getAccessibleChildrenCount() {
        return this.source instanceof java.awt.Container
                ? ((java.awt.Container) this.source).getComponentCount()
                : 0;
    }

    /** The child {@code i}, translated. */
    public Accessible getAccessibleChild(int i) {
        if (!(this.source instanceof java.awt.Container)) {
            return null;
        }
        java.awt.Container cont = (java.awt.Container) this.source;
        if (i < 0 || i >= cont.getComponentCount()) {
            return null;
        }
        return getAccessible(cont.getComponent(i));
    }

    /**
     * The component's locale.
     *
     * @throws IllegalComponentStateException if the component does not have one yet
     */
    public Locale getLocale() throws IllegalComponentStateException {
        Component c = component();
        return c == null ? Locale.getDefault() : c.getLocale();
    }

    /** It does nothing: a component that does not collaborate emits no accessible property
         * changes. */
    public void addPropertyChangeListener(PropertyChangeListener l) {
    }

    /** It does nothing, for the same reason. */
    public void removePropertyChangeListener(PropertyChangeListener l) {
    }

    /** The background colour. */
    public Color getBackground() {
        Component c = component();
        return c == null ? null : c.getBackground();
    }

    /** It changes the background colour. */
    public void setBackground(Color color) {
        Component c = component();
        if (c != null) {
            c.setBackground(color);
        }
    }

    /** The foreground colour. */
    public Color getForeground() {
        Component c = component();
        return c == null ? null : c.getForeground();
    }

    /** It changes the foreground colour. */
    public void setForeground(Color color) {
        Component c = component();
        if (c != null) {
            c.setForeground(color);
        }
    }

    /** The cursor. */
    public Cursor getCursor() {
        Component c = component();
        return c == null ? null : c.getCursor();
    }

    /** It changes the cursor. */
    public void setCursor(Cursor cursor) {
        Component c = component();
        if (c != null) {
            c.setCursor(cursor);
        }
    }

    /** The font. */
    public Font getFont() {
        Component c = component();
        return c == null ? null : c.getFont();
    }

    /** It changes the font. */
    public void setFont(Font f) {
        Component c = component();
        if (c != null) {
            c.setFont(f);
        }
    }

    /** That font's metrics. */
    public FontMetrics getFontMetrics(Font f) {
        Component c = component();
        return c == null ? null : c.getFontMetrics(f);
    }

    /** Whether it is enabled. */
    public boolean isEnabled() {
        Component c = component();
        return c != null && c.isEnabled();
    }

    /** It enables or disables it. */
    public void setEnabled(boolean b) {
        Component c = component();
        if (c != null) {
            c.setEnabled(b);
        }
    }

    /** Whether it is marked as visible. */
    public boolean isVisible() {
        Component c = component();
        return c != null && c.isVisible();
    }

    /** It shows it or hides it. */
    public void setVisible(boolean b) {
        Component c = component();
        if (c != null) {
            c.setVisible(b);
        }
    }

    /**
     * Whether it is really seen.
     *
     * <p>Different from {@link #isVisible}: a visible component inside a closed window is not
     * shown, and for an assistive technology that is the difference between reading it and not.
     */
    public boolean isShowing() {
        Component c = component();
        return c != null && c.isShowing();
    }

    /** Whether that point, relative to the component, falls inside. */
    public boolean contains(Point p) {
        Component c = component();
        return c != null && c.contains(p);
    }

    /** Where it is on the screen. */
    public Point getLocationOnScreen() {
        Component c = component();
        return c == null ? null : c.getLocationOnScreen();
    }

    /** Where it is inside its container. */
    public Point getLocation() {
        Component c = component();
        return c == null ? null : c.getLocation();
    }

    /** It moves it. */
    public void setLocation(Point p) {
        Component c = component();
        if (c != null) {
            c.setLocation(p);
        }
    }

    /** Its rectangle. */
    public Rectangle getBounds() {
        Component c = component();
        return c == null ? null : c.getBounds();
    }

    /** It changes its rectangle. */
    public void setBounds(Rectangle r) {
        Component c = component();
        if (c != null) {
            c.setBounds(r);
        }
    }

    /** Its size. */
    public Dimension getSize() {
        Component c = component();
        return c == null ? null : c.getSize();
    }

    /** It changes its size. */
    public void setSize(Dimension d) {
        Component c = component();
        if (c != null) {
            c.setSize(d);
        }
    }

    /** The accessible child that is at that point. */
    public Accessible getAccessibleAt(Point p) {
        if (!(this.source instanceof java.awt.Container)) {
            return null;
        }
        Component child = ((java.awt.Container) this.source).getComponentAt(p);
        return child == null || child == this.source ? null : getAccessible(child);
    }

    /** Whether it may receive the focus with the tab key. */
    public boolean isFocusTraversable() {
        Component c = component();
        return c != null && c.isFocusable();
    }

    /** It asks for the focus. */
    public void requestFocus() {
        Component c = component();
        if (c != null) {
            c.requestFocus();
        }
    }

    /** It listens to the component's focus changes. */
    public synchronized void addFocusListener(FocusListener l) {
        Component c = component();
        if (c != null) {
            c.addFocusListener(l);
        }
    }

    /** It stops listening to them. */
    public synchronized void removeFocusListener(FocusListener l) {
        Component c = component();
        if (c != null) {
            c.removeFocusListener(l);
        }
    }
}
