package javax.transaction.xa;

/**
 * KajiLibrary's javax.transaction.xa.XAException -- algo salio mal en una transaccion distribuida.
 *
 * <p>El codigo va en un **campo publico** y no en un getter, cosa que hoy no se haria; queda asi
 * porque la firma es el contrato y el codigo que existe lo lee como campo.
 *
 * <p>Los codigos se leen por familias, y vale la pena verlas: los `XA_RB*` dicen "se deshizo, y por
 * esto"; los `XA_HEUR*` dicen algo peor --que el recurso **decidio por su cuenta** sin esperar al
 * coordinador, que es la falla que rompe la atomicidad y siempre termina en intervencion humana--; y
 * los `XAER_*`, negativos, son errores del protocolo o del gestor, no de la transaccion.
 */
public class XAException extends Exception {

    /** El codigo del error. */
    public int errorCode;

    // ---- se deshizo la transaccion ------------------------------------------------------------------

    /** El primero de los codigos de vuelta atras. */
    public static final int XA_RBBASE = 100;

    /** Se deshizo sin una razon mas especifica. */
    public static final int XA_RBROLLBACK = XA_RBBASE;

    /** Se deshizo por un fallo de comunicacion. */
    public static final int XA_RBCOMMFAIL = XA_RBBASE + 1;

    /** Se deshizo porque se detecto un abrazo mortal. */
    public static final int XA_RBDEADLOCK = XA_RBBASE + 2;

    /** Se deshizo porque violaba una restriccion de integridad. */
    public static final int XA_RBINTEGRITY = XA_RBBASE + 3;

    /** Se deshizo por una razon que el gestor no clasifica. */
    public static final int XA_RBOTHER = XA_RBBASE + 4;

    /** Se deshizo por un error de protocolo dentro del recurso. */
    public static final int XA_RBPROTO = XA_RBBASE + 5;

    /** Se deshizo porque tardo demasiado. */
    public static final int XA_RBTIMEOUT = XA_RBBASE + 6;

    /** Se deshizo por algo transitorio: reintentar puede funcionar. */
    public static final int XA_RBTRANSIENT = XA_RBBASE + 7;

    /** El ultimo de los codigos de vuelta atras. */
    public static final int XA_RBEND = XA_RBTRANSIENT;

    // ---- el resto -----------------------------------------------------------------------------------

    /** La transaccion se reanudo en un lugar donde no se puede migrar. */
    public static final int XA_NOMIGRATE = 9;

    /** Puede haberse decidido por cuenta propia; no se sabe. */
    public static final int XA_HEURHAZ = 8;

    /** Se confirmo por cuenta propia. */
    public static final int XA_HEURCOM = 7;

    /** Se deshizo por cuenta propia. */
    public static final int XA_HEURRB = 6;

    /** Parte se confirmo y parte se deshizo por cuenta propia: la peor. */
    public static final int XA_HEURMIX = 5;

    /** Todavia no hay trabajo; volver a preguntar. */
    public static final int XA_RETRY = 4;

    /** La rama era de solo lectura y ya se confirmo. */
    public static final int XA_RDONLY = 3;

    /** Hay una operacion asincronica pendiente. */
    public static final int XAER_ASYNC = -2;

    /** Error del gestor de recursos al ejecutar la operacion. */
    public static final int XAER_RMERR = -3;

    /** El identificador no corresponde a ninguna transaccion. */
    public static final int XAER_NOTA = -4;

    /** Un argumento invalido. */
    public static final int XAER_INVAL = -5;

    /** La operacion se llamo fuera de secuencia. */
    public static final int XAER_PROTO = -6;

    /** El gestor de recursos no esta disponible. */
    public static final int XAER_RMFAIL = -7;

    /** Ya existe una transaccion con ese identificador. */
    public static final int XAER_DUPID = -8;

    /** El recurso esta trabajando fuera de la transaccion global. */
    public static final int XAER_OUTSIDE = -9;

    public XAException() {
        super();
    }

    public XAException(String s) {
        super(s);
    }

    public XAException(int errcode) {
        super();
        this.errorCode = errcode;
    }
}
