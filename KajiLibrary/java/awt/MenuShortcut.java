package java.awt;

/**
 * El atajo de teclado de un item de menu: una tecla y si ademas hace falta Shift.
 *
 * <p>La tecla se guarda como codigo de {@code KeyEvent}, no como caracter. La diferencia importa:
 * un atajo se dispara con la tecla fisica, asi que la 'A' de un teclado y la de otro son el mismo
 * atajo aunque el caracter que produzcan cambie con el layout.
 *
 * <p>El {@code hashCode()} es el codigo de tecla, o su complemento a uno si usa Shift. Es una
 * biyeccion --{@code ~k} nunca coincide con un codigo de tecla valido, que es positivo-- asi que
 * Ctrl+A y Ctrl+Shift+A no colisionan nunca, que es exactamente lo que hace falta en el mapa de
 * atajos de una barra de menu.
 *
 * <h2>Lo que falta y por que</h2>
 *
 * <p>{@code toString()} y {@code paramString()} <b>no estan</b>. El JDK los arma con
 * {@code KeyEvent.getKeyModifiersText()} y {@code KeyEvent.getKeyText()} --que traducen un codigo
 * de tecla al nombre que le pone el sistema-- y ademas le preguntan al {@code Toolkit} cual es la
 * tecla modificadora de menu de la plataforma, que en macOS no es Ctrl. Ni {@code java.awt.event}
 * ni {@code Toolkit} existen en KajiLibrary.
 *
 * <p>Devolver algo como {@code "Ctrl+65"} compilaria y seria mentira: diria que el modificador es
 * Ctrl sin haberlo averiguado y llamaria "65" a una tecla que se llama "A". Sin toString propio se
 * hereda el de {@code Object}, que no afirma nada.
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

    /** Sobrecarga tipada: la que usa la barra de menu, que ya sabe que compara con otro atajo. */
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
}
