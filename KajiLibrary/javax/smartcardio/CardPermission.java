package javax.smartcardio;

import java.security.Permission;

/**
 * KajiLibrary's javax.smartcardio.CardPermission -- permiso para hablar con un lector.
 *
 * <p>El nombre es el del lector, o {@code "*"} para todos. Las acciones son seis:
 *
 * <ul>
 *   <li>{@code connect}: conectarse a la tarjeta;
 *   <li>{@code reset}: reiniciarla, que se lleva puesto el estado de cualquier otro que la este
 *       usando;
 *   <li>{@code exclusive}: quedarse con el lector para uno solo;
 *   <li>{@code transmitControl}: mandarle ordenes al lector, no a la tarjeta;
 *   <li>{@code getBasicChannel}: usar el canal basico;
 *   <li>{@code openLogicalChannel}: abrir un canal logico nuevo.
 * </ul>
 *
 * <p>{@code "*"} tambien vale como accion y son las seis. Nombrarlas todas da lo mismo: la forma
 * canonica que devuelve {@link #getActions} las colapsa en un asterisco.
 *
 * <p>Esa forma canonica ordena las acciones <b>alfabeticamente</b>, no por importancia; dos permisos
 * con las mismas acciones en distinto orden son iguales.
 */
public class CardPermission extends Permission {

    private static final long serialVersionUID = 7146787880530705613L;

    /** Conectarse a la tarjeta. */
    private static final int CONNECT = 0x1;

    /** Quedarse con el lector. */
    private static final int EXCLUSIVE = 0x2;

    /** Usar el canal basico. */
    private static final int GET_BASIC_CHANNEL = 0x4;

    /** Abrir un canal logico. */
    private static final int OPEN_LOGICAL_CHANNEL = 0x8;

    /** Reiniciar la tarjeta. */
    private static final int RESET = 0x10;

    /** Mandarle ordenes al lector. */
    private static final int TRANSMIT_CONTROL = 0x20;

    /** Las seis juntas. */
    private static final int ALL = CONNECT | EXCLUSIVE | GET_BASIC_CHANNEL | OPEN_LOGICAL_CHANNEL
        | RESET | TRANSMIT_CONTROL;

    /** Los nombres, en el orden de los bits, que es el alfabetico. */
    private static final String[] ACTION_NAMES = {
        "connect", "exclusive", "getBasicChannel", "openLogicalChannel", "reset", "transmitControl",
    };

    /** Que acciones estan permitidas. */
    private final int mask;

    /** La forma canonica, o null si se construyo sin acciones. */
    private final String actions;

    /**
     * Ese permiso sobre ese lector.
     *
     * @param name el nombre del lector, o {@code "*"}
     * @param actions las acciones separadas por comas, {@code "*"}, o null
     * @throws NullPointerException si el nombre es null
     * @throws IllegalArgumentException si las acciones son la cadena vacia o alguna no existe
     */
    public CardPermission(String name, String actions) {
        super(name);
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        this.mask = getMask(actions);
        this.actions = actions == null ? null : canonicalize(this.mask);
    }

    /** Las acciones en forma canonica, o null si se construyo sin ellas. */
    @Override
    public String getActions() {
        return this.actions;
    }

    /**
     * Si este permiso alcanza para lo que el otro pide.
     *
     * <p>Hacen falta las dos cosas: que el nombre sea el mismo o el nuestro sea {@code "*"}, y que
     * nuestras acciones incluyan a todas las suyas.
     */
    @Override
    public boolean implies(Permission permission) {
        if (!(permission instanceof CardPermission)) {
            return false;
        }
        CardPermission other = (CardPermission) permission;
        if ((this.mask & other.mask) != other.mask) {
            return false;
        }
        return "*".equals(getName()) || getName().equals(other.getName());
    }

    /** Dos permisos son iguales si nombran al mismo lector y permiten las mismas acciones. */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CardPermission)) {
            return false;
        }
        CardPermission other = (CardPermission) obj;
        return this.mask == other.mask && getName().equals(other.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode() + 31 * this.mask;
    }

    /**
     * Los bits de esa lista de acciones.
     *
     * <p>No recorta espacios a proposito: {@code " connect "} no es una accion, y aceptarlo callado
     * dejaria pasar listas mal escritas que despues dan permisos que nadie quiso dar.
     */
    private static int getMask(String actions) {
        if (actions == null) {
            return 0;
        }
        if (actions.isEmpty()) {
            throw new IllegalArgumentException("actions must not be empty");
        }
        if ("*".equals(actions)) {
            return ALL;
        }
        int mask = 0;
        String[] pieces = actions.split(",", -1);
        int i = 0;
        while (i < pieces.length) {
            int bit = bitFor(pieces[i]);
            if (bit == 0) {
                throw new IllegalArgumentException("Invalid action: '" + pieces[i] + "'");
            }
            mask = mask | bit;
            i = i + 1;
        }
        return mask;
    }

    /** El bit de esa accion, sin distinguir mayusculas, o cero si no existe. */
    private static int bitFor(String action) {
        int i = 0;
        while (i < ACTION_NAMES.length) {
            if (ACTION_NAMES[i].equalsIgnoreCase(action)) {
                return 1 << i;
            }
            i = i + 1;
        }
        return 0;
    }

    /** La forma canonica de esos bits. Ver la nota de la clase. */
    private static String canonicalize(int mask) {
        if (mask == ALL) {
            return "*";
        }
        StringBuilder text = new StringBuilder();
        int i = 0;
        while (i < ACTION_NAMES.length) {
            if ((mask & (1 << i)) != 0) {
                if (text.length() > 0) {
                    text.append(',');
                }
                text.append(ACTION_NAMES[i]);
            }
            i = i + 1;
        }
        return text.toString();
    }
}
