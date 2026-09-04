package javax.naming.ldap;

import java.io.Serializable;

/**
 * Un modificador que viaja pegado a una operacion LDAP.
 *
 * <h2>Para que sirve el mecanismo</h2>
 *
 * <p>LDAP tiene pocas operaciones —buscar, agregar, modificar, borrar— y una forma de extenderlas
 * sin cambiar el protocolo: los <em>controles</em>. Cada uno se identifica por un OID y lleva sus
 * datos codificados en BER; el servidor que lo entiende cambia su comportamiento, y el que no, lo
 * mira y sigue.
 *
 * <p>Asi es como se pide paginacion ({@link PagedResultsControl}) u ordenamiento
 * ({@link SortControl}) sin que existan operaciones "buscar paginado" y "buscar ordenado".
 *
 * <h2>{@link #isCritical}, que es lo importante</h2>
 *
 * <p>Un control critico que el servidor no entiende hace que la operacion <strong>falle</strong>;
 * uno no critico se ignora en silencio. La eleccion no es de estilo: pedir ordenamiento no critico y
 * recibir resultados sin ordenar, sin enterarse, es peor que un error.
 */
public interface Control extends Serializable {

    /** Que la operacion falle si el servidor no entiende el control. */
    boolean CRITICAL = true;

    /** Que el servidor lo ignore si no lo entiende. */
    boolean NONCRITICAL = false;

    /** El OID que identifica al control. */
    String getID();

    /** Si es critico; ver la nota de la clase. */
    boolean isCritical();

    /** Los datos del control, codificados en BER, o {@code null} si no lleva. */
    byte[] getEncodedValue();
}
