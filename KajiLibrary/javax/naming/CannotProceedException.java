package javax.naming;

import java.util.Hashtable;

/**
 * "Hasta aca llego yo": la excepcion con la que un proveedor entrega la resolucion a otro.
 *
 * <p>Un nombre compuesto puede atravesar espacios de nombres de proveedores distintos --`ldap`
 * hasta cierto punto y despues un sistema de archivos, por ejemplo--. Cuando el primero llega al
 * borde de lo suyo no puede fallar ni inventar: lanza esta, y lo que lleva adentro es
 * **exactamente lo necesario para que otro siga**, que es mas que un mensaje de error.
 *
 * <p>Los cuatro campos propios son las cuatro piezas de esa continuacion:
 *
 * <ul>
 *   <li>`remainingNewName`, para las operaciones que tienen **dos** nombres (`rename`): el
 *       heredado `remainingName` cuenta el origen, este cuenta el destino.
 *   <li>`environment`, porque el que siga necesita las propiedades del que venia resolviendo.
 *   <li>`altName` y `altNameCtx`, que son el mismo punto de corte visto desde el otro lado: el
 *       nombre del objeto resuelto **relativo a** `altNameCtx`. Sin ese par, el que continua
 *       tendria un objeto pero no sabria como se llama en el espacio de nombres de donde sale.
 * </ul>
 *
 * <p>La usa `javax.naming.spi.NamingManager.getContinuationContext`, que es la funcion que toma
 * esta excepcion y devuelve el contexto por el que se sigue. En este JDK ese metodo no esta
 * declarado --ver la cabecera de `InitialContext`--, asi que la clase existe como el tipo que es
 * y como el contenedor de estado que es, que es todo lo que se puede cumplir sin proveedores.
 *
 * <p>El resto de la jerarquia esta explicado en `NamingException`.
 */
public class CannotProceedException extends NamingException {

    private static final long serialVersionUID = 1219724816191576813L;

    /** El "nombre restante" del **segundo** nombre, para operaciones de dos nombres como `rename`. */
    protected Name remainingNewName;

    /** El entorno con el que se venia resolviendo; el que continua lo necesita igual. */
    protected Hashtable<?, ?> environment;

    /** El nombre del objeto resuelto, pero relativo a `altNameCtx` y no al contexto original. */
    protected Name altName;

    /** El contexto contra el que `altName` se lee. */
    protected Context altNameCtx;

    public CannotProceedException(String explanation) {
        super(explanation);
    }

    public CannotProceedException() {
        super();
    }

    public Hashtable<?, ?> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Hashtable<?, ?> environment) {
        this.environment = environment;
    }

    public Name getRemainingNewName() {
        return remainingNewName;
    }

    /** Clona, igual que los setters de nombre de `NamingException` y por la misma razon. */
    public void setRemainingNewName(Name newName) {
        remainingNewName = (newName != null) ? (Name) newName.clone() : null;
    }

    public Name getAltName() {
        return altName;
    }

    public void setAltName(Name altName) {
        this.altName = altName;
    }

    public Context getAltNameCtx() {
        return altNameCtx;
    }

    public void setAltNameCtx(Context altNameCtx) {
        this.altNameCtx = altNameCtx;
    }
}
