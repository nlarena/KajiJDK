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
        return unico(new AWTKeyStroke(keyChar.charValue(), KeyEvent.VK_UNDEFINED,
                conLasDosMascaras(modifiers), false));
    }

    /**
     * El atajo de esa tecla, al apretarla o al soltarla.
     *
     * <p>`onKeyRelease` no es un detalle: un atajo al soltar y uno al apretar son distintos, y hay
     * interfaces que usan los dos.
     */
    public static AWTKeyStroke getAWTKeyStroke(int keyCode, int modifiers,
            boolean onKeyRelease) {
        return unico(new AWTKeyStroke(KeyEvent.CHAR_UNDEFINED, keyCode,
                conLasDosMascaras(modifiers), onKeyRelease));
    }

    /**
     * Los modificadores con sus dos mascaras: la nueva y la vieja.
     *
     * <p>Cada modificador de teclado tiene dos constantes en {@code InputEvent}: la nueva
     * ({@code CTRL_DOWN_MASK}) y la de antes ({@code CTRL_MASK}, en desuso). Un atajo lleva las dos
     * puestas, y no es redundancia inutil: hay codigo que sigue leyendo la vieja --el texto del
     * acelerador de un item de menu, sin ir mas lejos, que sale de
     * {@code KeyEvent.getKeyModifiersText}--, y con la vieja en cero se queda sin nombre de
     * modificador y el acelerador se muestra como {@code "O"} en vez de {@code "Ctrl-O"}.
     *
     * <p>Los botones del mouse no entran: {@code BUTTON2_MASK} y {@code BUTTON3_MASK} valen lo
     * mismo que {@code ALT_MASK} y {@code META_MASK}, y agregarlas inventaria modificadores que
     * nadie pidio. Esta medido: {@code ctrl O} da 130 y {@code ctrl shift S} da 195.
     */
    private static int conLasDosMascaras(int modifiers) {
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
        // Y al reves, para quien todavia pase las viejas.
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

    /**
     * El atajo escrito como lo lee {@link #getAWTKeyStroke(String)}.
     *
     * <p>Los dos formatos son el mismo: {@code "ctrl released ENTER"} sale de aca y vuelve a
     * entrar por el analizador sin perder nada. Por eso los modificadores se escriben con su
     * nombre --{@code shift ctrl meta alt altGraph button1 button2 button3}, en ese orden-- y la
     * tecla con el nombre de su constante {@code VK_} sin el prefijo, que no es lo mismo que
     * {@link KeyEvent#getKeyText}: esa devuelve texto para mostrarle a una persona y esta el
     * nombre exacto de la constante.
     */
    public String toString() {
        if (this.keyCode == KeyEvent.VK_UNDEFINED) {
            return textoDeModificadores(this.modifiers) + "typed " + this.keyChar;
        }
        return textoDeModificadores(this.modifiers)
                + (this.onKeyRelease ? "released" : "pressed") + " "
                + nombreDeTecla(this.keyCode);
    }

    /** Los modificadores en el orden que espera el analizador; cada uno con un espacio atras. */
    private static String textoDeModificadores(int modifiers) {
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

    /** Los nombres ya buscados; buscar por reflexion 189 campos por atajo seria caro. */
    private static final Map<Integer, String> NOMBRES = new HashMap<Integer, String>();

    /**
     * El nombre de la constante {@code VK_} de esa tecla, sin el prefijo.
     *
     * <p>Sale por reflexion sobre {@link KeyEvent} y no de una tabla escrita a mano: son casi
     * doscientas constantes, y una tabla que se olvide de una da un nombre equivocado en vez de
     * faltar. {@code "UNKNOWN"} si no hay ninguna, que es lo que contesta el JDK.
     */
    private static String nombreDeTecla(int keyCode) {
        Integer clave = Integer.valueOf(keyCode);
        synchronized (NOMBRES) {
            String ya = NOMBRES.get(clave);
            if (ya != null) {
                return ya;
            }
        }
        int esperados = java.lang.reflect.Modifier.PUBLIC | java.lang.reflect.Modifier.STATIC
                | java.lang.reflect.Modifier.FINAL;
        java.lang.reflect.Field[] campos = KeyEvent.class.getDeclaredFields();
        for (int i = 0; i < campos.length; i++) {
            try {
                if (campos[i].getModifiers() == esperados
                        && campos[i].getType() == Integer.TYPE
                        && campos[i].getName().startsWith("VK_")
                        && campos[i].getInt(KeyEvent.class) == keyCode) {
                    String nombre = campos[i].getName().substring(3);
                    synchronized (NOMBRES) {
                        NOMBRES.put(clave, nombre);
                    }
                    return nombre;
                }
            } catch (IllegalAccessException e) {
                // Un campo publico de una clase publica siempre es accesible; si algun dia no lo
                // fuera, se sigue con el que viene en vez de romper el toString.
            }
        }
        return "UNKNOWN";
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
