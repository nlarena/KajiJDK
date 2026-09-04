package javax.naming.spi;

import java.io.Serializable;
import javax.naming.CompositeName;
import javax.naming.InvalidNameException;
import javax.naming.Name;

/**
 * KajiLibrary's javax.naming.spi.ResolveResult -- hasta donde se llego, y que falta.
 *
 * <p>Lo que devuelve un {@link Resolver}: el objeto al que se pudo resolver y el pedazo de nombre que
 * quedo sin resolver. Con los dos, quien llama sigue la resolucion contra el objeto nuevo.
 *
 * <p>Los dos {@code append} existen porque la resolucion va <b>hacia atras</b> al desarmarse: un
 * contexto que no puede seguir le agrega al resto lo que el mismo no consumio, y asi el resultado
 * final acumula todo lo que falta desde el punto donde se corto.
 *
 * <p>Los campos son {@code protected}, como en el JDK: sus subclases --{@code CannotProceedException}
 * entre ellas-- los tocan directo.
 */
public class ResolveResult implements Serializable {

    private static final long serialVersionUID = -4552108072002407559L;

    /** A que se resolvio. */
    protected Object resolvedObj;

    /** Que quedo sin resolver. */
    protected Name remainingName;

    /** Vacio, para las subclases que se llenan despues. */
    protected ResolveResult() {
        this.resolvedObj = null;
        this.remainingName = null;
    }

    /**
     * Con el resto como texto.
     *
     * <p>El texto se parsea como {@link CompositeName}, que es el formato de los nombres que cruzan
     * espacios de nombres distintos.
     */
    public ResolveResult(Object robj, String rcomp) {
        this.resolvedObj = robj;
        try {
            this.remainingName = new CompositeName(rcomp);
        } catch (InvalidNameException e) {
            // La especificacion no deja lanzar aca. Un nombre que no parsea queda sin resto, que es
            // lo unico coherente: no hay por donde seguir.
            this.remainingName = null;
        }
    }

    /** Con el resto ya armado. */
    public ResolveResult(Object robj, Name rname) {
        this.resolvedObj = robj;
        setRemainingName(rname);
    }

    /** Que quedo sin resolver. */
    public Name getRemainingName() {
        return this.remainingName;
    }

    /** A que se resolvio. */
    public Object getResolvedObj() {
        return this.resolvedObj;
    }

    /** Reemplaza el resto. Se guarda una copia: el nombre es mutable. */
    public void setRemainingName(Name name) {
        if (name == null) {
            this.remainingName = null;
            return;
        }
        this.remainingName = (Name) name.clone();
    }

    /** Le agrega eso al final del resto. Ver la nota de la clase. */
    public void appendRemainingName(Name name) {
        if (name == null) {
            return;
        }
        if (this.remainingName == null) {
            this.remainingName = (Name) name.clone();
            return;
        }
        try {
            this.remainingName.addAll(name);
        } catch (InvalidNameException e) {
            // No puede pasar: se esta agregando al final de un nombre del mismo tipo.
        }
    }

    /** Idem, con un solo componente. */
    public void appendRemainingComponent(String name) {
        if (name == null) {
            return;
        }
        try {
            if (this.remainingName == null) {
                this.remainingName = new CompositeName();
            }
            this.remainingName.add(name);
        } catch (InvalidNameException e) {
            // Idem.
        }
    }

    /** Reemplaza el objeto resuelto. */
    public void setResolvedObj(Object obj) {
        this.resolvedObj = obj;
    }
}
