package javax.naming.directory;

import java.util.Vector;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;

/**
 * KajiLibrary's javax.naming.directory.BasicAttribute -- un atributo armado en memoria.
 *
 * <p>La implementacion de {@link Attribute} que se usa para <b>construir</b> lo que se le va a mandar
 * al directorio. Lo que vuelve de una consulta suele ser otra implementacion, la del proveedor.
 *
 * <h2>La comparacion de valores</h2>
 *
 * <p>{@link #contains}, {@link #remove} y {@link #equals} comparan con {@code equals}, con una
 * excepcion importante: si el valor es un <b>arreglo</b>, se comparan sus elementos. Sin eso, dos
 * atributos con los mismos bytes no serian iguales --{@code byte[].equals} es identidad-- y los
 * valores binarios de un directorio son justamente arreglos de bytes.
 *
 * <h2>Los dos metodos de esquema no hacen nada</h2>
 *
 * <p>{@link #getAttributeDefinition} y {@link #getAttributeSyntaxDefinition} lanzan
 * {@link OperationNotSupportedException}. No es una limitacion de esta biblioteca: un atributo armado
 * en memoria no viene de ningun directorio, asi que no hay esquema del que hablar. El JDK hace lo
 * mismo.
 */
public class BasicAttribute implements Attribute {

    private static final long serialVersionUID = 6743528196119291326L;

    /** El identificador. */
    protected String attrID;

    /** Los valores. Transitorio porque se serializa a mano; protegido como en el JDK. */
    protected transient Vector<Object> values;

    /** Si los valores son una lista y no un conjunto. */
    protected boolean ordered;

    /** Sin valores, sin orden. */
    public BasicAttribute(String id) {
        this(id, false);
    }

    /** Con un valor, sin orden. */
    public BasicAttribute(String id, Object value) {
        this(id, value, false);
    }

    /** Sin valores, diciendo si lleva orden. */
    public BasicAttribute(String id, boolean ordered) {
        this.attrID = id;
        this.values = new Vector<Object>();
        this.ordered = ordered;
    }

    /** Con un valor, diciendo si lleva orden. */
    public BasicAttribute(String id, Object value, boolean ordered) {
        this(id, ordered);
        this.values.addElement(value);
    }

    /** Una copia con los mismos valores. */
    public Object clone() {
        BasicAttribute copy = new BasicAttribute(this.attrID, this.ordered);
        copy.values = new Vector<Object>(this.values);
        return copy;
    }

    /**
     * Iguales si coinciden el identificador, el orden y los valores.
     *
     * <p>Sin orden, los valores se comparan como conjuntos: mismo contenido en cualquier orden. Con
     * orden, posicion por posicion.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Attribute)) {
            return false;
        }
        Attribute that = (Attribute) obj;
        if (!this.attrID.equals(that.getID())) {
            return false;
        }
        if (this.ordered != that.isOrdered()) {
            return false;
        }
        if (this.size() != that.size()) {
            return false;
        }
        try {
            int i = 0;
            while (i < this.values.size()) {
                if (this.ordered) {
                    if (!sameValue(this.values.elementAt(i), that.get(i))) {
                        return false;
                    }
                } else {
                    if (!that.contains(this.values.elementAt(i))) {
                        return false;
                    }
                }
                i = i + 1;
            }
        } catch (NamingException e) {
            return false;
        }
        return true;
    }

    /** Coherente con {@link #equals}: no depende del orden cuando el atributo no lo tiene. */
    public int hashCode() {
        int hash = this.attrID.hashCode();
        int i = 0;
        while (i < this.values.size()) {
            Object v = this.values.elementAt(i);
            if (v != null) {
                hash = hash + valueHash(v);
            }
            i = i + 1;
        }
        return hash;
    }

    /** El identificador y los valores, para un registro. */
    public String toString() {
        StringBuilder sb = new StringBuilder(this.attrID).append(": ");
        if (this.values.size() == 0) {
            sb.append("No values");
            return sb.toString();
        }
        int i = 0;
        while (i < this.values.size()) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(this.values.elementAt(i));
            i = i + 1;
        }
        return sb.toString();
    }

    /** Todos los valores. */
    public NamingEnumeration<?> getAll() throws NamingException {
        return new ValueEnumeration(new Vector<Object>(this.values));
    }

    /**
     * El primero de los valores.
     *
     * @throws javax.naming.NoSuchElementException si no tiene ninguno
     */
    public Object get() throws NamingException {
        if (this.values.size() == 0) {
            throw new java.util.NoSuchElementException("Attribute " + this.attrID + " has no value");
        }
        return this.values.elementAt(0);
    }

    /** Cuantos valores tiene. */
    public int size() {
        return this.values.size();
    }

    /** El identificador. */
    public String getID() {
        return this.attrID;
    }

    /** Si tiene ese valor. Ver la nota de la clase sobre los arreglos. */
    public boolean contains(Object attrVal) {
        return indexOf(attrVal) >= 0;
    }

    /**
     * Agrega un valor.
     *
     * @return false si el atributo no lleva orden y ya lo tenia
     */
    public boolean add(Object attrVal) {
        if (!this.ordered && contains(attrVal)) {
            return false;
        }
        this.values.addElement(attrVal);
        return true;
    }

    /** Saca la primera aparicion de ese valor. */
    public boolean remove(Object attrval) {
        int i = indexOf(attrval);
        if (i < 0) {
            return false;
        }
        this.values.removeElementAt(i);
        return true;
    }

    /** Saca todos. */
    public void clear() {
        this.values.setSize(0);
    }

    /** Si los valores son una lista y no un conjunto. */
    public boolean isOrdered() {
        return this.ordered;
    }

    /**
     * El valor de esa posicion.
     *
     * @throws IndexOutOfBoundsException si no existe
     */
    public Object get(int ix) throws NamingException {
        return this.values.elementAt(ix);
    }

    /** Saca el de esa posicion. */
    public Object remove(int ix) {
        Object old = this.values.elementAt(ix);
        this.values.removeElementAt(ix);
        return old;
    }

    /** Inserta en esa posicion. */
    public void add(int ix, Object attrVal) {
        if (!this.ordered && contains(attrVal)) {
            throw new IllegalStateException(
                "Cannot add duplicate to unordered attribute " + this.attrID);
        }
        this.values.insertElementAt(attrVal, ix);
    }

    /** Reemplaza el de esa posicion. */
    public Object set(int ix, Object attrVal) {
        if (!this.ordered && contains(attrVal)) {
            throw new IllegalStateException(
                "Cannot add duplicate to unordered attribute " + this.attrID);
        }
        Object old = this.values.elementAt(ix);
        this.values.setElementAt(attrVal, ix);
        return old;
    }

    /**
     * No hay esquema.
     *
     * @throws OperationNotSupportedException siempre; ver la nota de la clase
     */
    public DirContext getAttributeSyntaxDefinition() throws NamingException {
        throw new OperationNotSupportedException("attribute syntax");
    }

    /**
     * No hay esquema.
     *
     * @throws OperationNotSupportedException siempre
     */
    public DirContext getAttributeDefinition() throws NamingException {
        throw new OperationNotSupportedException("attribute definition");
    }

    /** La posicion de ese valor, o -1. Ver la nota de la clase sobre los arreglos. */
    private int indexOf(Object candidate) {
        int i = 0;
        while (i < this.values.size()) {
            if (sameValue(this.values.elementAt(i), candidate)) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    /** Igualdad de valores, con los arreglos comparados por contenido. */
    private static boolean sameValue(Object a, Object b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.getClass().isArray() && b.getClass().isArray()) {
            int lenA = java.lang.reflect.Array.getLength(a);
            if (lenA != java.lang.reflect.Array.getLength(b)) {
                return false;
            }
            int i = 0;
            while (i < lenA) {
                if (!sameValue(java.lang.reflect.Array.get(a, i),
                               java.lang.reflect.Array.get(b, i))) {
                    return false;
                }
                i = i + 1;
            }
            return true;
        }
        return a.equals(b);
    }

    /** Hash de un valor, coherente con {@link #sameValue}. */
    private static int valueHash(Object v) {
        if (!v.getClass().isArray()) {
            return v.hashCode();
        }
        int hash = 0;
        int len = java.lang.reflect.Array.getLength(v);
        int i = 0;
        while (i < len) {
            Object e = java.lang.reflect.Array.get(v, i);
            hash = hash + (e == null ? 0 : valueHash(e));
            i = i + 1;
        }
        return hash;
    }

    /** La enumeracion sobre una copia de los valores. */
    private static final class ValueEnumeration implements NamingEnumeration<Object> {

        private final Vector<Object> snapshot;

        private int index = 0;

        ValueEnumeration(Vector<Object> snapshot) {
            this.snapshot = snapshot;
        }

        public boolean hasMore() throws NamingException {
            return hasMoreElements();
        }

        public Object next() throws NamingException {
            return nextElement();
        }

        public void close() throws NamingException {
            this.index = this.snapshot.size();
        }

        public boolean hasMoreElements() {
            return this.index < this.snapshot.size();
        }

        public Object nextElement() {
            Object v = this.snapshot.elementAt(this.index);
            this.index = this.index + 1;
            return v;
        }
    }
}
