package org.ietf.jgss;

/**
 * KajiLibrary's org.ietf.jgss.GSSException -- fallo una operacion GSS-API.
 *
 * <p>Lleva <b>dos</b> codigos y esa es su particularidad: un codigo mayor, de esta lista, que es
 * comun a toda implementacion de GSS-API, y uno menor, que lo define el mecanismo de abajo
 * --Kerberos, por ejemplo-- y que no significa nada fuera de el.
 *
 * <p>La division viene de que GSS-API es una capa sobre mecanismos distintos: el codigo mayor deja
 * que una aplicacion reaccione sin saber cual esta abajo, y el menor conserva el detalle para el
 * registro. Por eso {@link #getMessage} los junta cuando hay los dos, con el menor entre parentesis.
 *
 * <p>El codigo mayor es un {@code int} y no un enum porque el API es una traduccion literal del
 * estandar de la IETF, que lo define asi. Un valor que no este en la lista se describe como
 * "failure unspecified": es lo que hace el JDK y evita que un mecanismo nuevo rompa el
 * formateo del mensaje.
 */
public class GSSException extends Exception {

    private static final long serialVersionUID = -2706218945227726672L;

    /** Las etiquetas del canal no coinciden. */
    public static final int BAD_BINDINGS = 1;

    /** Se pidio un mecanismo que no esta. */
    public static final int BAD_MECH = 2;

    /** El nombre no sirve. */
    public static final int BAD_NAME = 3;

    /** El tipo de nombre no esta soportado. */
    public static final int BAD_NAMETYPE = 4;

    /** El selector de estado no sirve. */
    public static final int BAD_STATUS = 5;

    /** El token no paso el chequeo de integridad. */
    public static final int BAD_MIC = 6;

    /** El contexto vencio. */
    public static final int CONTEXT_EXPIRED = 7;

    /** Las credenciales vencieron. */
    public static final int CREDENTIALS_EXPIRED = 8;

    /** La credencial esta rota. */
    public static final int DEFECTIVE_CREDENTIAL = 9;

    /** El token esta roto. */
    public static final int DEFECTIVE_TOKEN = 10;

    /** Falla sin especificar. Es tambien lo que se contesta para cualquier codigo desconocido. */
    public static final int FAILURE = 11;

    /** No hay contexto, o ya se destruyo. */
    public static final int NO_CONTEXT = 12;

    /** No se dieron credenciales validas. */
    public static final int NO_CRED = 13;

    /** La calidad de proteccion pedida no esta soportada. */
    public static final int BAD_QOP = 14;

    /** La operacion no esta autorizada. */
    public static final int UNAUTHORIZED = 15;

    /** La operacion no esta disponible. */
    public static final int UNAVAILABLE = 16;

    /** Se pidio agregar un elemento de credencial que ya estaba. */
    public static final int DUPLICATE_ELEMENT = 17;

    /** El nombre tiene elementos de varios mecanismos. */
    public static final int NAME_NOT_MN = 18;

    /** El token es un duplicado de uno anterior. */
    public static final int DUPLICATE_TOKEN = 19;

    /** El token ya vencio. */
    public static final int OLD_TOKEN = 20;

    /** Ya se proceso un token posterior. */
    public static final int UNSEQ_TOKEN = 21;

    /** Falta un token que se esperaba. */
    public static final int GAP_TOKEN = 22;

    /** Los textos de los codigos mayores, indexados por el codigo. */
    private static final String[] MAJOR_TEXT = {
        "Failure unspecified at GSS-API level",
        "Channel binding mismatch",
        "Unsupported mechanism requested",
        "Invalid name provided",
        "Name of unsupported type provided",
        "Invalid input status selector",
        "Token had invalid integrity check",
        "Specified security context expired",
        "Expired credentials detected",
        "Defective credential detected",
        "Defective token detected",
        "Failure unspecified at GSS-API level",
        "Security context init/accept not yet called or context deleted",
        "No valid credentials provided",
        "Unsupported QOP value",
        "Operation unauthorized",
        "Operation unavailable",
        "Duplicate credential element requested",
        "Name contains multi-mechanism elements",
        "The token was a duplicate of an earlier token",
        "The token's validity period has expired",
        "A later token has already been processed",
        "An expected per-message token was not received",
    };

    private final int major;

    private int minor;

    private String minorMessage;

    /** Solo con el codigo mayor. */
    public GSSException(int majorCode) {
        this.major = majorCode;
        this.minor = 0;
        this.minorMessage = null;
    }

    /**
     * Con los dos codigos.
     *
     * @param minorCode el del mecanismo de abajo
     * @param minorString que dice ese mecanismo
     */
    public GSSException(int majorCode, int minorCode, String minorString) {
        this.major = majorCode;
        this.minor = minorCode;
        this.minorMessage = minorString;
    }

    /** El codigo mayor, de la lista de arriba. */
    public int getMajor() {
        return this.major;
    }

    /** El del mecanismo, o 0 si no hay. */
    public int getMinor() {
        return this.minor;
    }

    /** El texto del codigo mayor. Ver la nota de la clase sobre los codigos desconocidos. */
    public String getMajorString() {
        if (this.major > 0 && this.major < MAJOR_TEXT.length) {
            return MAJOR_TEXT[this.major];
        }
        return MAJOR_TEXT[0];
    }

    /** Lo que dijo el mecanismo, o null. */
    public String getMinorString() {
        return this.minorMessage;
    }

    /**
     * Le pone el codigo del mecanismo despues de construida.
     *
     * <p>Existe porque la capa de GSS-API arma la excepcion antes de que el mecanismo termine de
     * contar lo suyo.
     *
     * <p>Asigna las dos cosas siempre, incluso con codigo 0. Vale saberlo porque el codigo es el que
     * manda: con 0, {@link #getMessage} <b>no</b> muestra el texto que se puso aca, aunque
     * {@link #getMinorString} si lo devuelva.
     */
    public void setMinor(int minorCode, String message) {
        this.minor = minorCode;
        this.minorMessage = message;
    }

    /** El mensaje, con el prefijo del tipo. */
    public String toString() {
        return "GSSException: " + getMessage();
    }

    /**
     * El texto del mayor y, si hay codigo menor, el del menor entre parentesis.
     *
     * <p>Lo que decide es el <b>codigo</b> menor y no el texto: con un codigo distinto de 0 y texto
     * null, sale {@code "(Mechanism level: null)"}. Es lo que hace el JDK y tiene su logica -- un
     * codigo sin texto es un mecanismo que fallo y no supo explicarse, y esconderlo perderia el dato
     * de que fallo.
     */
    public String getMessage() {
        if (this.minor == 0) {
            return getMajorString();
        }
        return getMajorString() + " (Mechanism level: " + this.minorMessage + ")";
    }
}
