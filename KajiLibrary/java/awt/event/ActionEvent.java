package java.awt.event;

import java.awt.AWTEvent;

/**
 * Se ejecutó una acción: se apretó un botón, se eligió una opción, se dio Enter en un campo.
 *
 * <p>Es el evento de más alto nivel de AWT y por eso el más usado. No dice qué tecla ni qué botón:
 * dice que **pasó lo que el componente quería que pasara**, sin importar cómo se llegó. El mismo
 * evento sale de un clic, de la barra espaciadora o de un atajo de teclado.
 *
 * <p>El "comando" es una cadena que identifica **qué** acción fue, y sirve para que un solo oyente
 * atienda varios componentes sin comparar referencias.
 */
public class ActionEvent extends AWTEvent {

    private static final long serialVersionUID = -7671078796273832149L;

    /** El primer identificador de la familia. */
    public static final int ACTION_FIRST = 1001;

    /** El último identificador de la familia. */
    public static final int ACTION_LAST = 1001;

    /** Se ejecutó la acción. */
    public static final int ACTION_PERFORMED = 1001;

    /** Alt estaba apretada. */
    public static final int ALT_MASK = 8;

    /** Control estaba apretada. */
    public static final int CTRL_MASK = 2;

    /** Meta estaba apretada. */
    public static final int META_MASK = 4;

    /** Mayúsculas estaba apretada. */
    public static final int SHIFT_MASK = 1;

    private final String actionCommand;
    private final long when;
    private final int modifiers;

    /**
     * Con la fuente, el identificador y el comando.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    public ActionEvent(Object source, int id, String command) {
        this(source, id, command, 0, 0);
    }

    /**
     * Como el anterior, con los modificadores.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    public ActionEvent(Object source, int id, String command, int modifiers) {
        this(source, id, command, 0, modifiers);
    }

    /**
     * Con todo dado.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    public ActionEvent(Object source, int id, String command, long when, int modifiers) {
        super(source, id);
        this.actionCommand = command;
        this.when = when;
        this.modifiers = modifiers;
    }

    /** Qué acción fue. */
    public String getActionCommand() {
        return this.actionCommand;
    }

    /** Cuándo pasó. */
    public long getWhen() {
        return this.when;
    }

    /** Qué modificadores estaban apretados. */
    public int getModifiers() {
        return this.modifiers;
    }

    public String paramString() {
        String tipo = this.id == ACTION_PERFORMED ? "ACTION_PERFORMED" : "unknown type";
        return tipo + ",cmd=" + this.actionCommand + ",when=" + this.when + ",modifiers="
                + java.awt.event.InputEvent.getModifiersExText(this.modifiers);
    }
}
