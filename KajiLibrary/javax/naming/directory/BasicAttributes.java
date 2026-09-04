package javax.naming.directory;

import java.util.ArrayList;
import java.util.List;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.directory.BasicAttributes -- un conjunto de atributos armado en memoria.
 *
 * <p>La implementacion de {@link Attributes} que se usa para construir lo que se le manda al
 * directorio.
 *
 * <p>La decision que la define es {@link #isCaseIgnored}, y se fija al construir. Con la regla que
 * ignora mayusculas --que es la que corresponde a LDAP-- guardar {@code "CN"} y despues pedir
 * {@code "cn"} funciona; con la otra, no. Poner la regla equivocada da un sintoma confuso: los
 * atributos "no estan" aunque se los vea en un volcado.
 *
 * <p>Se guarda una lista y no un mapa a proposito. Con la regla que ignora mayusculas haria falta
 * normalizar la clave, y normalizar pierde la forma original del identificador -- que es la que hay
 * que mandarle al directorio. Con pocos atributos por entrada, recorrer no cuesta nada.
 */
public class BasicAttributes implements Attributes {

    private static final long serialVersionUID = 4980164073184639448L;

    /** Si los identificadores se comparan sin distinguir mayusculas. */
    private final boolean ignoreCase;

    /** Los atributos, en orden de alta. Ver la nota de la clase. */
    private final List<Attribute> attrs = new ArrayList<Attribute>();

    /** Vacio, distinguiendo mayusculas. */
    public BasicAttributes() {
        this(false);
    }

    /** Vacio, con la regla de comparacion elegida. */
    public BasicAttributes(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    /** Con un atributo de un solo valor, distinguiendo mayusculas. */
    public BasicAttributes(String attrID, Object val) {
        this(attrID, val, false);
    }

    /** Con un atributo de un solo valor y la regla elegida. */
    public BasicAttributes(String attrID, Object val, boolean ignoreCase) {
        this(ignoreCase);
        this.attrs.add(new BasicAttribute(attrID, val));
    }

    /**
     * Una copia.
     *
     * <p>Copia la lista, no los atributos: los {@link Attribute} son los mismos objetos. Es lo que
     * hace el JDK, y hay que saberlo si alguien va a modificar uno.
     */
    public Object clone() {
        BasicAttributes copy = new BasicAttributes(this.ignoreCase);
        copy.attrs.addAll(this.attrs);
        return copy;
    }

    /** Si los identificadores se comparan sin distinguir mayusculas. */
    public boolean isCaseIgnored() {
        return this.ignoreCase;
    }

    /** Cuantos atributos hay. */
    public int size() {
        return this.attrs.size();
    }

    /**
     * El atributo con ese identificador.
     *
     * @return null si no esta
     */
    public Attribute get(String attrID) {
        int i = indexOf(attrID);
        return (i < 0) ? null : this.attrs.get(i);
    }

    /** Todos los atributos. */
    public NamingEnumeration<Attribute> getAll() {
        return new ListEnumeration<Attribute>(new ArrayList<Attribute>(this.attrs));
    }

    /** Solo los identificadores. */
    public NamingEnumeration<String> getIDs() {
        List<String> ids = new ArrayList<String>();
        int i = 0;
        while (i < this.attrs.size()) {
            ids.add(this.attrs.get(i).getID());
            i = i + 1;
        }
        return new ListEnumeration<String>(ids);
    }

    /** Agrega un atributo de un solo valor. */
    public Attribute put(String attrID, Object val) {
        return put(new BasicAttribute(attrID, val));
    }

    /**
     * Agrega un atributo ya armado.
     *
     * @return el que estaba con ese identificador, o null
     */
    public Attribute put(Attribute attr) {
        int i = indexOf(attr.getID());
        if (i < 0) {
            this.attrs.add(attr);
            return null;
        }
        Attribute old = this.attrs.get(i);
        this.attrs.set(i, attr);
        return old;
    }

    /** Lo saca y lo devuelve. */
    public Attribute remove(String attrID) {
        int i = indexOf(attrID);
        if (i < 0) {
            return null;
        }
        return this.attrs.remove(i);
    }

    /** Los atributos, para un registro. */
    public String toString() {
        if (this.attrs.isEmpty()) {
            return "No attributes";
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < this.attrs.size()) {
            sb.append(this.attrs.get(i).toString());
            if (i < this.attrs.size() - 1) {
                sb.append("; ");
            }
            i = i + 1;
        }
        return sb.toString();
    }

    /**
     * Iguales si tienen la misma regla y los mismos atributos.
     *
     * <p>El orden no cuenta: los atributos de una entrada son un conjunto.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Attributes)) {
            return false;
        }
        Attributes that = (Attributes) obj;
        if (this.ignoreCase != that.isCaseIgnored()) {
            return false;
        }
        if (this.size() != that.size()) {
            return false;
        }
        int i = 0;
        while (i < this.attrs.size()) {
            Attribute mine = this.attrs.get(i);
            Attribute theirs = that.get(mine.getID());
            if (theirs == null || !mine.equals(theirs)) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /** Coherente con {@link #equals}: suma, para no depender del orden. */
    public int hashCode() {
        int hash = this.ignoreCase ? 1 : 0;
        int i = 0;
        while (i < this.attrs.size()) {
            hash = hash + this.attrs.get(i).hashCode();
            i = i + 1;
        }
        return hash;
    }

    /** La posicion de ese identificador segun la regla de comparacion, o -1. */
    private int indexOf(String attrID) {
        int i = 0;
        while (i < this.attrs.size()) {
            String id = this.attrs.get(i).getID();
            boolean same = this.ignoreCase ? id.equalsIgnoreCase(attrID) : id.equals(attrID);
            if (same) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    /** La enumeracion sobre una copia. */
    private static final class ListEnumeration<T> implements NamingEnumeration<T> {

        private final List<T> snapshot;

        private int index = 0;

        ListEnumeration(List<T> snapshot) {
            this.snapshot = snapshot;
        }

        public boolean hasMore() throws NamingException {
            return hasMoreElements();
        }

        public T next() throws NamingException {
            return nextElement();
        }

        public void close() throws NamingException {
            this.index = this.snapshot.size();
        }

        public boolean hasMoreElements() {
            return this.index < this.snapshot.size();
        }

        public T nextElement() {
            T v = this.snapshot.get(this.index);
            this.index = this.index + 1;
            return v;
        }
    }
}
