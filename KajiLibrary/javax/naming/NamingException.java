package javax.naming;

/**
 * La raiz de las veinticinco excepciones de JNDI.
 *
 * <h2>Por que hay veinticinco y no una</h2>
 *
 * <p>Una operacion de nombres --resolver `ldap://host/cn=juan`, atar un objeto, listar un
 * contexto-- puede fallar por razones que el que llama trata **distinto**: si el nombre no existe
 * se crea, si el servidor no contesta se reintenta, si las credenciales estan mal se le pregunta
 * al usuario, y si el nombre esta mal escrito no se reintenta nunca. Un solo tipo con un codigo
 * adentro obligaria a cada `catch` a mirar el codigo; una jerarquia deja que el `catch` elija el
 * escalon que le sirve. Por eso el paquete tiene una excepcion por causa y **cinco** ramas:
 *
 * <ul>
 *   <li>Las hojas directas de esta clase: `NameNotFoundException`, `NameAlreadyBoundException`,
 *       `CommunicationException`, `ConfigurationException`, ... Cada una es una causa distinta y
 *       no agrega ni un miembro; lo unico que aporta es **su tipo**.
 *   <li>`NamingSecurityException`, abstracta, agrupa las tres de seguridad, para que un `catch`
 *       pueda decir "cualquier problema de autenticacion o permisos" de una.
 *   <li>`LimitExceededException` agrupa las dos de limite (tamano y tiempo).
 *   <li>`LinkException` agrupa las de enlaces y **si** agrega estado: el enlace tiene su propio
 *       nombre resuelto y su propio nombre restante, aparte de los del contexto.
 *   <li>`CannotProceedException` y `ReferralException` son las dos que el proveedor usa para
 *       decir "segui vos en otro lado", y las dos llevan el estado necesario para seguir.
 * </ul>
 *
 * <h2>Lo que esta clase agrega a `Exception`</h2>
 *
 * <p>Una excepcion de nombres no dice solamente "fallo": dice **hasta donde** llego. Resolver
 * `a/b/c/d` puede resolver `a/b` y morir en `c`; eso son dos nombres --el resuelto y el que
 * queda-- mas el objeto al que se llego. Esos tres campos son `protected` a proposito: los
 * proveedores los completan de a pedazos mientras la excepcion sube por las capas, y por eso
 * existen `appendRemainingComponent` y `appendRemainingName`, que van **acumulando** el nombre
 * restante en el camino de vuelta.
 *
 * <p>La otra particularidad es `rootException`: JNDI trajo el encadenamiento de causas en 1.3,
 * antes de que `Throwable` lo tuviera. Cuando el JDK sumo `getCause`/`initCause`, esta clase
 * quedo con **los dos** nombres para lo mismo, cableados uno al otro: `getCause` devuelve
 * `getRootCause`, e `initCause` tambien setea la causa raiz. Se conserva igual porque la
 * asimetria es observable: `setRootCause` no toca la causa de `Throwable`.
 *
 * <p>Sobre la serializacion: la clase es `Serializable` por herencia, y su forma serial es la
 * default (los cuatro campos). Este arbol no tiene `ObjectOutputStream`, asi que la declaracion
 * es un contrato sin quien lo ejercite; se deja el `serialVersionUID` del JDK real para que el
 * dia que exista un flujo la forma coincida y no haya que cambiar nada.
 */
public class NamingException extends Exception {

    private static final long serialVersionUID = -1299181962103167177L;

    /** Hasta donde se pudo resolver. `null` si no se sabe o no se llego a ningun lado. */
    protected Name resolvedName;

    /** El objeto al que se llego resolviendo `resolvedName`. */
    protected Object resolvedObj;

    /** Lo que faltaba resolver cuando se fallo. */
    protected Name remainingName;

    /** La causa de fondo, de cuando JNDI tenia que encadenar causas a mano. */
    protected Throwable rootException;

    public NamingException(String explanation) {
        super(explanation);
        // Dos sentencias y no `resolvedName = remainingName = null`: la asignacion
        // encadenada sobre **campos** dispara el #460 de COMPILER_FINDINGS (falta el
        // `dup_x1`, y el segundo `putfield` vacia la pila). Separadas es lo mismo.
        resolvedName = null;
        remainingName = null;
        resolvedObj = null;
        rootException = null;
    }

    public NamingException() {
        super();
        // Dos sentencias y no `resolvedName = remainingName = null`: la asignacion
        // encadenada sobre **campos** dispara el #460 de COMPILER_FINDINGS (falta el
        // `dup_x1`, y el segundo `putfield` vacia la pila). Separadas es lo mismo.
        resolvedName = null;
        remainingName = null;
        resolvedObj = null;
        rootException = null;
    }

    public Name getResolvedName() {
        return resolvedName;
    }

    public Name getRemainingName() {
        return remainingName;
    }

    public Object getResolvedObj() {
        return resolvedObj;
    }

    /** Es `getMessage()` con otro nombre; JNDI lo llama "explicacion" desde antes. */
    public String getExplanation() {
        return getMessage();
    }

    // Los dos setters de nombre **clonan**. Un `Name` es mutable, y la excepcion viaja hacia
    // arriba mientras el proveedor sigue usando su copia: sin clonar, el nombre que el que
    // atrapa lee podria haber cambiado despues de lanzada.

    public void setResolvedName(Name name) {
        resolvedName = (name != null) ? (Name) name.clone() : null;
    }

    public void setRemainingName(Name name) {
        remainingName = (name != null) ? (Name) name.clone() : null;
    }

    public void setResolvedObj(Object obj) {
        resolvedObj = obj;
    }

    /**
     * Suma un componente al **frente** conceptual del nombre restante.
     *
     * <p>Esto es lo que hace la capa de arriba mientras la excepcion sube: cada contexto que la
     * ve le agrega lo que **el** no llego a resolver, y al final el nombre restante es completo
     * visto desde el contexto inicial. Si todavia no hay nombre restante arranca uno compuesto,
     * que es el tipo neutro para nombres que atraviesan espacios de nombres distintos.
     */
    public void appendRemainingComponent(String name) {
        if (name != null) {
            try {
                if (remainingName == null) {
                    remainingName = new CompositeName();
                }
                remainingName.add(name);
            } catch (NamingException e) {
                // `CompositeName.add` solo falla con nombres invalidos, y aca el componente ya
                // viene partido: si igual pasa, es un error de programacion del proveedor.
                throw new IllegalArgumentException(e.toString());
            }
        }
    }

    public void appendRemainingName(Name name) {
        if (name == null) {
            return;
        }
        if (remainingName != null) {
            try {
                remainingName.addAll(name);
            } catch (NamingException e) {
                throw new IllegalArgumentException(e.toString());
            }
        } else {
            remainingName = (Name) name.clone();
        }
    }

    public Throwable getRootCause() {
        return rootException;
    }

    /** El `if` evita el ciclo trivial: una excepcion causada por si misma cuelga cualquier impresor. */
    public void setRootCause(Throwable e) {
        if (e != this) {
            rootException = e;
        }
    }

    @Override
    public Throwable getCause() {
        return getRootCause();
    }

    /** Setea las dos: la de `Throwable` --que solo admite una vez-- y la de JNDI. */
    @Override
    public Throwable initCause(Throwable cause) {
        super.initCause(cause);
        setRootCause(cause);
        return this;
    }

    @Override
    public String toString() {
        String answer = super.toString();
        if (rootException != null) {
            answer += " [Root exception is " + rootException + "]";
        }
        if (remainingName != null) {
            answer += "; remaining name '" + remainingName + "'";
        }
        return answer;
    }

    /**
     * Igual que `toString()`, mas el objeto resuelto si se pide detalle y hay uno.
     *
     * <p>Va aparte porque el objeto resuelto puede ser cualquier cosa --una conexion, un pool--
     * y su `toString` puede ser enorme o filtrar datos; el default no lo imprime.
     */
    public String toString(boolean detail) {
        if (!detail || resolvedObj == null) {
            return toString();
        }
        return toString() + "; resolved object " + resolvedObj;
    }
}
