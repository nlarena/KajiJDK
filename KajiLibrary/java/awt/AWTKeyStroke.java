package java.awt;

import java.awt.event.KeyEvent;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * A keystroke described **without having happened**.
 *
 * <p>A {@link KeyEvent} says something happened; this describes something that could happen. It
 * serves to declare shortcuts: "Ctrl+S saves" is a description, not an event.
 *
 * <p>The instances are **shared**: asking twice for the same shortcut returns the same object. That
 * is why there is no public constructor and why {@link #equals} is `final`. A program that builds
 * thousands of equal shortcuts spends one object, and comparing them is comparing references.
 *
 * <p>Hence {@link #readResolve} as well: a deserialized shortcut has to become **the same object**
 * as the one that was already in the cache, or two equal shortcuts would stop being so after going
 * through disk.
 *
 * <p>A shortcut can be described by **key** —{@code VK_S}— or by **character** —the letter `s`—,
 * and they are not the same: the first is a physical key and the second what was typed. The two
 * cases are told apart by whether the key code is {@code VK_UNDEFINED}.
 */
public class AWTKeyStroke implements Serializable {

    private static final long serialVersionUID = -6430539691155757144L;

    private static final Map<AWTKeyStroke, AWTKeyStroke> cache =
            new HashMap<AWTKeyStroke, AWTKeyStroke>();

    private char keyChar = KeyEvent.CHAR_UNDEFINED;
    private int keyCode = KeyEvent.VK_UNDEFINED;
    private int modifiers;
    private boolean onKeyRelease;

    /** An empty one, for deserializing. */
    protected AWTKeyStroke() {
    }

    /** With everything given; one gets here through the factories. */
    protected AWTKeyStroke(char keyChar, int keyCode, int modifiers, boolean onKeyRelease) {
        this.keyChar = keyChar;
        this.keyCode = keyCode;
        this.modifiers = modifiers;
        this.onKeyRelease = onKeyRelease;
    }

    /**
     * Checks that the class is a subclass of this one.
     *
     * <p>Against what this note used to claim, it does not make the factories return instances of
     * that subclass —they keep returning {@code AWTKeyStroke}— and it does not check for a
     * no-argument constructor either. In the JDK the method is empty and does not even check this
     * much; here at least what could never work is rejected.
     *
     * @throws IllegalArgumentException if the class is `null`
     * @throws ClassCastException if the class does not derive from this one
     */
    protected static void registerSubclass(Class<?> subclass) {
        if (subclass == null) {
            throw new IllegalArgumentException("subclass cannot be null");
        }
        if (!AWTKeyStroke.class.isAssignableFrom(subclass)) {
            throw new ClassCastException("subclass is not derived from AWTKeyStroke");
        }
    }

    /** The cached one if it was already there, or this one stored in the cache. */
    private static AWTKeyStroke shared(AWTKeyStroke k) {
        synchronized (AWTKeyStroke.class) {
            AWTKeyStroke cached = cache.get(k);
            if (cached != null) {
                return cached;
            }
            cache.put(k, k);
            return k;
        }
    }

    /** The shortcut of typing that character. */
    public static AWTKeyStroke getAWTKeyStroke(char keyChar) {
        return shared(new AWTKeyStroke(keyChar, KeyEvent.VK_UNDEFINED, 0, false));
    }

    /**
     * The shortcut of that character with modifiers.
     *
     * @throws IllegalArgumentException if the character is `null`
     */
    public static AWTKeyStroke getAWTKeyStroke(Character keyChar, int modifiers) {
        if (keyChar == null) {
            throw new IllegalArgumentException("keyChar cannot be null");
        }
        return shared(new AWTKeyStroke(keyChar.charValue(), KeyEvent.VK_UNDEFINED,
                withBothMasks(modifiers), false));
    }

    /**
     * The shortcut of that key, on pressing it or on releasing it.
     *
     * <p>`onKeyRelease` is not a detail: a shortcut on release and one on press are different, and
     * there are interfaces that use both.
     */
    public static AWTKeyStroke getAWTKeyStroke(int keyCode, int modifiers,
            boolean onKeyRelease) {
        return shared(new AWTKeyStroke(KeyEvent.CHAR_UNDEFINED, keyCode,
                withBothMasks(modifiers), onKeyRelease));
    }

    /**
     * The modifiers with their two masks: the new one and the old one.
     *
     * <p>Every keyboard modifier has two constants in {@code InputEvent}: the new one ({@code
     * CTRL_DOWN_MASK}) and the earlier one ({@code CTRL_MASK}, deprecated). A shortcut carries both
     * of them on, and it is not useless redundancy: there is code that still reads the old one
     * --{@code KeyEvent.getKeyModifiersText}, which is what writes the accelerator of a menu item
     * out for a person, looks at nothing else-- and with the old one at zero it is left without a
     * modifier name and the accelerator shows as {@code "O"} instead of {@code "Ctrl-O"}.
     *
     * <p>The mouse buttons do not come in: {@code BUTTON2_MASK} and {@code BUTTON3_MASK} are worth
     * the same as {@code ALT_MASK} and {@code META_MASK}, and adding them would invent modifiers
     * nobody asked for. It is measured: {@code ctrl O} gives 130 and {@code ctrl shift S} gives
     * 195.
     */
    private static int withBothMasks(int modifiers) {
        if ((modifiers & java.awt.event.InputEvent.SHIFT_DOWN_MASK) != 0) {
            modifiers |= java.awt.event.InputEvent.SHIFT_MASK;
        }
        if ((modifiers & java.awt.event.InputEvent.CTRL_DOWN_MASK) != 0) {
            modifiers |= java.awt.event.InputEvent.CTRL_MASK;
        }
        if ((modifiers & java.awt.event.InputEvent.META_DOWN_MASK) != 0) {
            modifiers |= java.awt.event.InputEvent.META_MASK;
        }
        if ((modifiers & java.awt.event.InputEvent.ALT_DOWN_MASK) != 0) {
            modifiers |= java.awt.event.InputEvent.ALT_MASK;
        }
        if ((modifiers & java.awt.event.InputEvent.ALT_GRAPH_DOWN_MASK) != 0) {
            modifiers |= java.awt.event.InputEvent.ALT_GRAPH_MASK;
        }
        // And the other way round, for whoever still passes the old ones.
        if ((modifiers & java.awt.event.InputEvent.SHIFT_MASK) != 0) {
            modifiers |= java.awt.event.InputEvent.SHIFT_DOWN_MASK;
        }
        if ((modifiers & java.awt.event.InputEvent.CTRL_MASK) != 0) {
            modifiers |= java.awt.event.InputEvent.CTRL_DOWN_MASK;
        }
        if ((modifiers & java.awt.event.InputEvent.META_MASK) != 0) {
            modifiers |= java.awt.event.InputEvent.META_DOWN_MASK;
        }
        if ((modifiers & java.awt.event.InputEvent.ALT_MASK) != 0) {
            modifiers |= java.awt.event.InputEvent.ALT_DOWN_MASK;
        }
        return modifiers;
    }

    /** The shortcut of that key on pressing it. */
    public static AWTKeyStroke getAWTKeyStroke(int keyCode, int modifiers) {
        return getAWTKeyStroke(keyCode, modifiers, false);
    }

    /**
     * The shortcut that corresponds to that keyboard event.
     *
     * <p>A {@code KEY_TYPED} gives a shortcut by character and the other two, one by key: it is the
     * same distinction {@link KeyEvent} makes, kept.
     *
     * @throws NullPointerException if the event is `null`
     */
    public static AWTKeyStroke getAWTKeyStrokeForEvent(KeyEvent anEvent) {
        int id = anEvent.getID();
        if (id == KeyEvent.KEY_TYPED) {
            return getAWTKeyStroke(Character.valueOf(anEvent.getKeyChar()),
                    anEvent.getModifiersEx());
        }
        return getAWTKeyStroke(anEvent.getKeyCode(), anEvent.getModifiersEx(),
                id == KeyEvent.KEY_RELEASED);
    }

    /**
     * The shortcut that string describes, such as `"control S"` or `"released F1"`.
     *
     * @throws IllegalArgumentException if the string is `null` or is not understood
     */
    public static AWTKeyStroke getAWTKeyStroke(String s) {
        if (s == null) {
            throw new IllegalArgumentException("String cannot be null");
        }
        int modifiers = 0;
        boolean release = false;
        StringTokenizer st = new StringTokenizer(s, " ");
        String last = null;
        while (st.hasMoreTokens()) {
            String t = st.nextToken();
            if (t.equals("shift")) {
                modifiers = modifiers | java.awt.event.InputEvent.SHIFT_DOWN_MASK;
            } else if (t.equals("control") || t.equals("ctrl")) {
                modifiers = modifiers | java.awt.event.InputEvent.CTRL_DOWN_MASK;
            } else if (t.equals("meta")) {
                modifiers = modifiers | java.awt.event.InputEvent.META_DOWN_MASK;
            } else if (t.equals("alt")) {
                modifiers = modifiers | java.awt.event.InputEvent.ALT_DOWN_MASK;
            } else if (t.equals("altGraph")) {
                modifiers = modifiers | java.awt.event.InputEvent.ALT_GRAPH_DOWN_MASK;
            } else if (t.equals("button1")) {
                modifiers = modifiers | java.awt.event.InputEvent.BUTTON1_DOWN_MASK;
            } else if (t.equals("button2")) {
                modifiers = modifiers | java.awt.event.InputEvent.BUTTON2_DOWN_MASK;
            } else if (t.equals("button3")) {
                modifiers = modifiers | java.awt.event.InputEvent.BUTTON3_DOWN_MASK;
            } else if (t.equals("pressed")) {
                release = false;
            } else if (t.equals("released")) {
                release = true;
            } else if (t.equals("typed")) {
                release = false;
                last = "typed";
            } else {
                if ("typed".equals(last)) {
                    if (t.length() != 1) {
                        throw new IllegalArgumentException("Invalid typed key: " + t);
                    }
                    return getAWTKeyStroke(Character.valueOf(t.charAt(0)), modifiers);
                }
                int vk = keyCodeFor(t);
                if (vk == KeyEvent.VK_UNDEFINED) {
                    throw new IllegalArgumentException("Unknown keycode: " + t);
                }
                return getAWTKeyStroke(vk, modifiers, release);
            }
        }
        throw new IllegalArgumentException("String formatted incorrectly");
    }

    /**
     * The key code that goes by that name.
     *
     * <p>It understands the names of one letter or digit and a closed list of long ones: {@code
     * ENTER}, {@code ESCAPE}, {@code SPACE}, {@code TAB}, {@code DELETE}, {@code BACK_SPACE}, the
     * four arrows and {@code F1} to {@code F12}. Against what this note used to claim, it
     * understands no {@code VK_} prefix and no other constant name: the JDK resolves any of them by
     * reflection over {@code KeyEvent}, and here whatever is not on the list is rejected.
     */
    private static int keyCodeFor(String name) {
        if (name.length() == 1) {
            char c = name.charAt(0);
            if (c >= 'A' && c <= 'Z') {
                return KeyEvent.VK_A + (c - 'A');
            }
            if (c >= '0' && c <= '9') {
                return KeyEvent.VK_0 + (c - '0');
            }
        }
        if (name.equals("ENTER")) {
            return KeyEvent.VK_ENTER;
        }
        if (name.equals("ESCAPE")) {
            return KeyEvent.VK_ESCAPE;
        }
        if (name.equals("SPACE")) {
            return KeyEvent.VK_SPACE;
        }
        if (name.equals("TAB")) {
            return KeyEvent.VK_TAB;
        }
        if (name.equals("DELETE")) {
            return KeyEvent.VK_DELETE;
        }
        if (name.equals("BACK_SPACE")) {
            return KeyEvent.VK_BACK_SPACE;
        }
        if (name.equals("LEFT")) {
            return KeyEvent.VK_LEFT;
        }
        if (name.equals("RIGHT")) {
            return KeyEvent.VK_RIGHT;
        }
        if (name.equals("UP")) {
            return KeyEvent.VK_UP;
        }
        if (name.equals("DOWN")) {
            return KeyEvent.VK_DOWN;
        }
        if (name.length() >= 2 && name.charAt(0) == 'F') {
            try {
                int n = Integer.parseInt(name.substring(1));
                if (n >= 1 && n <= 12) {
                    return KeyEvent.VK_F1 + (n - 1);
                }
            } catch (NumberFormatException e) {
                return KeyEvent.VK_UNDEFINED;
            }
        }
        return KeyEvent.VK_UNDEFINED;
    }

    /** The character, or {@code CHAR_UNDEFINED} if the shortcut is by key. */
    public final char getKeyChar() {
        return this.keyChar;
    }

    /** The key, or {@code VK_UNDEFINED} if the shortcut is by character. */
    public final int getKeyCode() {
        return this.keyCode;
    }

    /** Which modifiers are needed. */
    public final int getModifiers() {
        return this.modifiers;
    }

    /** Whether it fires on release instead of on press. */
    public final boolean isOnKeyRelease() {
        return this.onKeyRelease;
    }

    /** Which {@link KeyEvent} identifier this shortcut matches. */
    public final int getKeyEventType() {
        if (this.keyCode == KeyEvent.VK_UNDEFINED) {
            return KeyEvent.KEY_TYPED;
        }
        if (this.onKeyRelease) {
            return KeyEvent.KEY_RELEASED;
        }
        return KeyEvent.KEY_PRESSED;
    }

    public int hashCode() {
        return (this.keyChar + 1) * (2 * (this.keyCode + 1)) * (this.modifiers + 1)
                + (this.onKeyRelease ? 1 : 2);
    }

    /**
     * Equality by key, character, modifiers and moment.
     *
     * <p>It is `final` because the instances are shared: two equal shortcuts are the **same**
     * object, and letting a subclass change equality would break the cache.
     */
    public final boolean equals(Object anObject) {
        if (!(anObject instanceof AWTKeyStroke)) {
            return false;
        }
        AWTKeyStroke that = (AWTKeyStroke) anObject;
        return that.keyCode == this.keyCode && that.keyChar == this.keyChar
                && that.modifiers == this.modifiers && that.onKeyRelease == this.onKeyRelease;
    }

    /**
     * The shortcut written the way {@link #getAWTKeyStroke(String)} reads it.
     *
     * <p>The two formats are meant to be one: {@code "ctrl released ENTER"} comes out of here and
     * goes back in through the parser without losing anything. That is why the modifiers are
     * written with their name --{@code shift ctrl meta alt altGraph button1 button2 button3}, in
     * that order-- and the key with the name of its {@code VK_} constant without the prefix, which
     * is not the same as {@link KeyEvent#getKeyText}: that one returns text to show a person and
     * this one the exact name of the constant.
     *
     * <p>The round trip only closes for the names the parser knows --a letter, a digit, {@code
     * ENTER}, {@code ESCAPE}, {@code SPACE}, {@code TAB}, {@code DELETE}, {@code BACK_SPACE}, the
     * arrows and {@code F1} to {@code F12}--. Any other key is written with the name of its
     * constant, which is right, and {@link #getAWTKeyStroke(String)} rejects it: {@code VK_HOME}
     * comes out as {@code "pressed HOME"} and does not go back in.
     */
    public String toString() {
        if (this.keyCode == KeyEvent.VK_UNDEFINED) {
            return modifiersText(this.modifiers) + "typed " + this.keyChar;
        }
        return modifiersText(this.modifiers)
                + (this.onKeyRelease ? "released" : "pressed") + " "
                + keyName(this.keyCode);
    }

    /** The modifiers in the order the parser expects; each one with a space after it. */
    private static String modifiersText(int modifiers) {
        StringBuilder buf = new StringBuilder();
        if ((modifiers & java.awt.event.InputEvent.SHIFT_DOWN_MASK) != 0) {
            buf.append("shift ");
        }
        if ((modifiers & java.awt.event.InputEvent.CTRL_DOWN_MASK) != 0) {
            buf.append("ctrl ");
        }
        if ((modifiers & java.awt.event.InputEvent.META_DOWN_MASK) != 0) {
            buf.append("meta ");
        }
        if ((modifiers & java.awt.event.InputEvent.ALT_DOWN_MASK) != 0) {
            buf.append("alt ");
        }
        if ((modifiers & java.awt.event.InputEvent.ALT_GRAPH_DOWN_MASK) != 0) {
            buf.append("altGraph ");
        }
        if ((modifiers & java.awt.event.InputEvent.BUTTON1_DOWN_MASK) != 0) {
            buf.append("button1 ");
        }
        if ((modifiers & java.awt.event.InputEvent.BUTTON2_DOWN_MASK) != 0) {
            buf.append("button2 ");
        }
        if ((modifiers & java.awt.event.InputEvent.BUTTON3_DOWN_MASK) != 0) {
            buf.append("button3 ");
        }
        return buf.toString();
    }

    /**
     * The names already looked up; searching the 189 {@code VK_} constants of {@code KeyEvent} for
     * every shortcut would be expensive.
     */
    private static final Map<Integer, String> NAMES = new HashMap<Integer, String>();

    /**
     * The name of the {@code VK_} constant of that key, without the prefix.
     *
     * <p>It comes out by reflection over {@link KeyEvent} and not from a table written by hand:
     * there are 189 constants, and a table that forgets one gives a wrong name instead of missing.
     * {@code "UNKNOWN"} if there is none, which is what the JDK answers.
     */
    private static String keyName(int keyCode) {
        Integer key = Integer.valueOf(keyCode);
        synchronized (NAMES) {
            String cached = NAMES.get(key);
            if (cached != null) {
                return cached;
            }
        }
        int expected = java.lang.reflect.Modifier.PUBLIC | java.lang.reflect.Modifier.STATIC
                | java.lang.reflect.Modifier.FINAL;
        java.lang.reflect.Field[] fields = KeyEvent.class.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            try {
                if (fields[i].getModifiers() == expected
                        && fields[i].getType() == Integer.TYPE
                        && fields[i].getName().startsWith("VK_")
                        && fields[i].getInt(KeyEvent.class) == keyCode) {
                    String name = fields[i].getName().substring(3);
                    synchronized (NAMES) {
                        NAMES.put(key, name);
                    }
                    return name;
                }
            } catch (IllegalAccessException e) {
                // A public field of a public class is always accessible; if some day it were not,
                // the loop goes on with the next one instead of breaking toString.
            }
        }
        return "UNKNOWN";
    }

    /**
     * The shared instance that corresponds to this shortcut.
     *
     * <p>Without this, a deserialized shortcut would be a different object from the one that was
     * already in the cache, and two equal shortcuts would stop comparing equal by identity after
     * going through disk.
     *
     * @throws ObjectStreamException if the instance cannot be resolved
     */
    protected Object readResolve() throws ObjectStreamException {
        return shared(this);
    }
}
