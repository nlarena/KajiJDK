package java.awt;

import java.awt.event.KeyEvent;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Una pulsación de teclado descrita **sin haber pasado**.
 *
 * <p>Un {@link KeyEvent} dice que algo pasó; esto describe algo que podría pasar. Sirve para
 * declarar atajos: "Ctrl+S guarda" es una descripción, no un evento.
 *
 * <p>Las instancias se **comparten**: pedir dos veces el mismo atajo devuelve el mismo objeto. Por
 * eso no hay constructor público y por eso {@link #equals} es `final`. Un programa que arme miles de
 * atajos iguales gasta un objeto, y compararlos es comparar referencias.
 *
 * <p>De ahí también {@link #readResolve}: un atajo deserializado tiene que volver a ser **el mismo
 * objeto** que el que ya estaba en la caché, o dos atajos iguales dejarían de serlo después de
 * pasar por disco.
 *
 * <p>Un atajo puede describirse por **tecla** —{@code VK_S}— o por **carácter** —la letra `s`—, y no
 * es lo mismo: lo primero es una tecla física y lo segundo lo que se escribió. Los dos casos se
 * distinguen por si el código de tecla es {@code VK_UNDEFINED}.
 */
public class AWTKeyStroke implements Serializable {

    private static final long serialVersionUID = -6430539691155757144L;

    private static final Map<AWTKeyStroke, AWTKeyStroke> cache =
            new HashMap<AWTKeyStroke, AWTKeyStroke>();

    private char keyChar = KeyEvent.CHAR_UNDEFINED;
    private int keyCode = KeyEvent.VK_UNDEFINED;
    private int modifiers;
    private boolean onKeyRelease;

    /** Uno vacío, para deserializar. */
    protected AWTKeyStroke() {
    }

    /** Con todo dado; se llega por las fábricas. */
    protected AWTKeyStroke(char keyChar, int keyCode, int modifiers, boolean onKeyRelease) {
        this.keyChar = keyChar;
        this.keyCode = keyCode;
        this.modifiers = modifiers;
        this.onKeyRelease = onKeyRelease;
    }

    /**
     * Declara que las fábricas devuelvan instancias de esa subclase.
     *
     * @throws IllegalArgumentException si la clase no hereda de ésta o no tiene constructor sin
     *     argumentos
     */
    protected static void registerSubclass(Class<?> subclass) {
        if (subclass == null) {
            throw new IllegalArgumentException("subclass cannot be null");
        }
        if (!AWTKeyStroke.class.isAssignableFrom(subclass)) {
            throw new ClassCastException("subclass is not derived from AWTKeyStroke");
        }
    }

    /** El de la caché si ya estaba, o éste guardado en ella. */
    private static AWTKeyStroke unico(AWTKeyStroke k) {
        synchronized (AWTKeyStroke.class) {
            AWTKeyStroke ya = cache.get(k);
            if (ya != null) {
                return ya;
            }
            cache.put(k, k);
            return k;
        }
    }

    /** El atajo de escribir ese carácter. */
    public static AWTKeyStroke getAWTKeyStroke(char keyChar) {
        return unico(new AWTKeyStroke(keyChar, KeyEvent.VK_UNDEFINED, 0, false));
    }

    /**
     * El atajo de ese carácter con modificadores.
     *
     * @throws IllegalArgumentException si el carácter es `null`
     */
    public static AWTKeyStroke getAWTKeyStroke(Character keyChar, int modifiers) {
        if (keyChar == null) {
            throw new IllegalArgumentException("keyChar cannot be null");
        }
        return unico(new AWTKeyStroke(keyChar.charValue(), KeyEvent.VK_UNDEFINED, modifiers,
                false));
    }

    /**
     * El atajo de esa tecla, al apretarla o al soltarla.
     *
     * <p>`onKeyRelease` no es un detalle: un atajo al soltar y uno al apretar son distintos, y hay
     * interfaces que usan los dos.
     */
    public static AWTKeyStroke getAWTKeyStroke(int keyCode, int modifiers,
            boolean onKeyRelease) {
        return unico(new AWTKeyStroke(KeyEvent.CHAR_UNDEFINED, keyCode, modifiers, onKeyRelease));
    }

    /** El atajo de esa tecla al apretarla. */
    public static AWTKeyStroke getAWTKeyStroke(int keyCode, int modifiers) {
        return getAWTKeyStroke(keyCode, modifiers, false);
    }

    /**
     * El atajo que corresponde a ese evento de teclado.
     *
     * <p>Un {@code KEY_TYPED} da un atajo por carácter y los otros dos, uno por tecla: es la misma
     * distinción que hace {@link KeyEvent}, conservada.
     *
     * @throws NullPointerException si el evento es `null`
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
     * El atajo que describe esa cadena, como `"control S"` o `"released F1"`.
     *
     * @throws IllegalArgumentException si la cadena es `null` o no se entiende
     */
    public static AWTKeyStroke getAWTKeyStroke(String s) {
        if (s == null) {
            throw new IllegalArgumentException("String cannot be null");
        }
        int modifiers = 0;
        boolean release = false;
        StringTokenizer st = new StringTokenizer(s, " ");
        String ultimo = null;
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
                ultimo = "typed";
            } else {
                if ("typed".equals(ultimo)) {
                    if (t.length() != 1) {
                        throw new IllegalArgumentException("Invalid typed key: " + t);
                    }
                    return getAWTKeyStroke(Character.valueOf(t.charAt(0)), modifiers);
                }
                int vk = codigoDe(t);
                if (vk == KeyEvent.VK_UNDEFINED) {
                    throw new IllegalArgumentException("Unknown keycode: " + t);
                }
                return getAWTKeyStroke(vk, modifiers, release);
            }
        }
        throw new IllegalArgumentException("String formatted incorrectly");
    }

    /**
     * El código de tecla que se llama así.
     *
     * <p>Sólo entiende los nombres de una letra o dígito y los de `VK_`. Los nombres largos del JDK
     * —`ENTER`, `F1`— salen de una tabla que se arma por reflexión sobre {@code KeyEvent}, y acá se
     * resuelven comparando contra las constantes que hacen falta.
     */
    private static int codigoDe(String nombre) {
        if (nombre.length() == 1) {
            char c = nombre.charAt(0);
            if (c >= 'A' && c <= 'Z') {
                return KeyEvent.VK_A + (c - 'A');
            }
            if (c >= '0' && c <= '9') {
                return KeyEvent.VK_0 + (c - '0');
            }
        }
        if (nombre.equals("ENTER")) {
            return KeyEvent.VK_ENTER;
        }
        if (nombre.equals("ESCAPE")) {
            return KeyEvent.VK_ESCAPE;
        }
        if (nombre.equals("SPACE")) {
            return KeyEvent.VK_SPACE;
        }
        if (nombre.equals("TAB")) {
            return KeyEvent.VK_TAB;
        }
        if (nombre.equals("DELETE")) {
            return KeyEvent.VK_DELETE;
        }
        if (nombre.equals("BACK_SPACE")) {
            return KeyEvent.VK_BACK_SPACE;
        }
        if (nombre.equals("LEFT")) {
            return KeyEvent.VK_LEFT;
        }
        if (nombre.equals("RIGHT")) {
            return KeyEvent.VK_RIGHT;
        }
        if (nombre.equals("UP")) {
            return KeyEvent.VK_UP;
        }
        if (nombre.equals("DOWN")) {
            return KeyEvent.VK_DOWN;
        }
        if (nombre.length() >= 2 && nombre.charAt(0) == 'F') {
            try {
                int n = Integer.parseInt(nombre.substring(1));
                if (n >= 1 && n <= 12) {
                    return KeyEvent.VK_F1 + (n - 1);
                }
            } catch (NumberFormatException e) {
                return KeyEvent.VK_UNDEFINED;
            }
        }
        return KeyEvent.VK_UNDEFINED;
    }

    /** El carácter, o {@code CHAR_UNDEFINED} si el atajo es por tecla. */
    public final char getKeyChar() {
        return this.keyChar;
    }

    /** La tecla, o {@code VK_UNDEFINED} si el atajo es por carácter. */
    public final int getKeyCode() {
        return this.keyCode;
    }

    /** Qué modificadores hacen falta. */
    public final int getModifiers() {
        return this.modifiers;
    }

    /** Si dispara al soltar en vez de al apretar. */
    public final boolean isOnKeyRelease() {
        return this.onKeyRelease;
    }

    /** Con qué identificador de {@link KeyEvent} coincide este atajo. */
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
     * Igualdad por tecla, carácter, modificadores y momento.
     *
     * <p>Es `final` porque las instancias se comparten: dos atajos iguales son el **mismo** objeto,
     * y dejar que una subclase cambiara la igualdad rompería la caché.
     */
    public final boolean equals(Object anObject) {
        if (!(anObject instanceof AWTKeyStroke)) {
            return false;
        }
        AWTKeyStroke that = (AWTKeyStroke) anObject;
        return that.keyCode == this.keyCode && that.keyChar == this.keyChar
                && that.modifiers == this.modifiers && that.onKeyRelease == this.onKeyRelease;
    }

    public String toString() {
        if (this.keyCode == KeyEvent.VK_UNDEFINED) {
            return this.modifiers + " typed " + this.keyChar;
        }
        return this.modifiers + " " + (this.onKeyRelease ? "released" : "pressed") + " "
                + KeyEvent.getKeyText(this.keyCode);
    }

    /**
     * La instancia compartida que corresponde a este atajo.
     *
     * <p>Sin esto, un atajo deserializado sería un objeto distinto del que ya estaba en la caché, y
     * dos atajos iguales dejarían de compararse iguales por identidad después de pasar por disco.
     *
     * @throws ObjectStreamException si la instancia no se puede resolver
     */
    protected Object readResolve() throws ObjectStreamException {
        return unico(this);
    }
}
