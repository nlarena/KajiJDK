package javax.print.attribute.standard;

import java.util.Collection;
import java.util.HashSet;
import javax.print.attribute.Attribute;
import javax.print.attribute.PrintJobAttribute;

/**
 * El conjunto de razones que explican el {@link JobState} de un trabajo.
 *
 * <p>Es un atributo que <b>es</b> una coleccion, no uno que la contiene: extiende
 * {@link HashSet} de {@link JobStateReason}. Eso es deliberado --se recorre y se consulta como
 * cualquier conjunto-- y trae la consecuencia de que es <b>mutable</b>, a diferencia del resto del
 * paquete. Guardarlo en un conjunto de atributos y despues modificarlo cambia lo que ese conjunto
 * reporta.
 *
 * <p>Un trabajo puede tener cero razones: "esta imprimiendo y todo va bien" no necesita explicacion.
 *
 * <p>Lo unico que se redefine es {@code add}, para rechazar el null: una razon que no es ninguna
 * razon no dice nada, y dejarla entrar haria explotar mas tarde a cualquiera que recorra el
 * conjunto.
 */
public final class JobStateReasons extends HashSet<JobStateReason> implements PrintJobAttribute {

    private static final long serialVersionUID = 8849088261264331812L;

    public JobStateReasons() {
        super();
    }

    public JobStateReasons(int initialCapacity) {
        super(initialCapacity);
    }

    public JobStateReasons(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    /** Copia el contenido de otra coleccion; los null de adentro los rechaza {@link #add}. */
    public JobStateReasons(Collection<JobStateReason> collection) {
        super(collection);
    }

    public boolean add(JobStateReason o) {
        if (o == null) {
            throw new NullPointerException();
        }
        return super.add(o);
    }

    public final Class<? extends Attribute> getCategory() {
        return JobStateReasons.class;
    }

    public final String getName() {
        return "job-state-reasons";
    }
}
