package javax.swing.text;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * El conjunto de atributos mutable de siempre: una tabla de nombre a valor.
 *
 * <p>Es el que se usa para <em>armar</em> atributos —{@code StyleConstants.setBold(attr, true)}— y
 * pasarselos a un documento. Un documento no lo guarda tal cual: lo pasa por su
 * {@code StyleContext}, que devuelve un conjunto inmutable y compartido. Por eso esta clase puede
 * ser una tabla simple sin preocuparse por la memoria.
 *
 * <p>El padre de resolucion se guarda como un atributo mas, bajo la clave
 * {@link AttributeSet#ResolveAttribute}. Es un detalle que se nota: {@link #getAttributeCount} lo
 * cuenta, y {@link #getAttributeNames} lo nombra.
 */
public class SimpleAttributeSet implements MutableAttributeSet, Serializable, Cloneable {

    /** Un conjunto vacio e inmutable, para no crear uno cada vez que hace falta "ninguno". */
    public static final AttributeSet EMPTY = new EmptyAttributeSet();

    private transient Hashtable<Object, Object> table = new Hashtable<Object, Object>(3);

    /** Un conjunto vacio. */
    public SimpleAttributeSet() {
    }

    /** Una copia de ese conjunto. */
    public SimpleAttributeSet(AttributeSet source) {
        addAttributes(source);
    }

    public boolean isEmpty() {
        return table.isEmpty();
    }

    public int getAttributeCount() {
        return table.size();
    }

    public boolean isDefined(Object attrName) {
        return table.get(attrName) != null;
    }

    /** Si los dos tienen la misma cantidad de atributos y este contiene a todos los del otro. */
    public boolean isEqual(AttributeSet attr) {
        return ((getAttributeCount() == attr.getAttributeCount())
                && containsAttributes(attr));
    }

    /** Una copia propia; el que la recibe puede cambiarla sin tocar a esta. */
    public AttributeSet copyAttributes() {
        return (AttributeSet) clone();
    }

    public Enumeration<?> getAttributeNames() {
        return table.keys();
    }

    /** El valor, o el que diga el padre de resolucion; ver {@link MutableAttributeSet}. */
    public Object getAttribute(Object name) {
        Object value = table.get(name);
        if (value == null) {
            AttributeSet parent = getResolveParent();
            if (parent != null) {
                value = parent.getAttribute(name);
            }
        }
        return value;
    }

    public boolean containsAttribute(Object name, Object value) {
        return value.equals(getAttribute(name));
    }

    public boolean containsAttributes(AttributeSet attributes) {
        boolean result = true;
        Enumeration<?> names = attributes.getAttributeNames();
        while (result && names.hasMoreElements()) {
            Object name = names.nextElement();
            result = attributes.getAttribute(name).equals(getAttribute(name));
        }
        return result;
    }

    public void addAttribute(Object name, Object value) {
        table.put(name, value);
    }

    public void addAttributes(AttributeSet attributes) {
        Enumeration<?> names = attributes.getAttributeNames();
        while (names.hasMoreElements()) {
            Object name = names.nextElement();
            addAttribute(name, attributes.getAttribute(name));
        }
    }

    public void removeAttribute(Object name) {
        table.remove(name);
    }

    public void removeAttributes(Enumeration<?> names) {
        while (names.hasMoreElements()) {
            removeAttribute(names.nextElement());
        }
    }

    /** Ver la nota de {@link MutableAttributeSet#removeAttributes(AttributeSet)}. */
    public void removeAttributes(AttributeSet attributes) {
        if (attributes == this) {
            table.clear();
        } else {
            Enumeration<?> names = attributes.getAttributeNames();
            while (names.hasMoreElements()) {
                Object name = names.nextElement();
                Object value = attributes.getAttribute(name);
                if (value.equals(getAttribute(name))) {
                    removeAttribute(name);
                }
            }
        }
    }

    public AttributeSet getResolveParent() {
        return (AttributeSet) table.get(AttributeSet.ResolveAttribute);
    }

    public void setResolveParent(AttributeSet parent) {
        addAttribute(AttributeSet.ResolveAttribute, parent);
    }

    /** Una copia con su propia tabla. */
    public Object clone() {
        SimpleAttributeSet attr;
        try {
            attr = (SimpleAttributeSet) super.clone();
            attr.table = new Hashtable<Object, Object>(table);
        } catch (CloneNotSupportedException cnse) {
            attr = null;
        }
        return attr;
    }

    public int hashCode() {
        return table.hashCode();
    }

    /** Igual a otro conjunto que tenga exactamente los mismos atributos. */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AttributeSet) {
            AttributeSet attrs = (AttributeSet) obj;
            return isEqual(attrs);
        }
        return false;
    }

    public String toString() {
        String s = "";
        Enumeration<?> names = getAttributeNames();
        while (names.hasMoreElements()) {
            Object key = names.nextElement();
            Object value = getAttribute(key);
            if (value instanceof AttributeSet) {
                // Un padre de resolucion se resume: imprimirlo entero podria no terminar nunca.
                s = s + key + "=**AttributeSet** ";
            } else {
                s = s + key + "=" + value + " ";
            }
        }
        return s;
    }

    /**
     * El conjunto vacio de {@link SimpleAttributeSet#EMPTY}.
     *
     * <p>Es una clase aparte, y no un {@code SimpleAttributeSet} sin nada, para que sea de verdad
     * inmutable: uno vacio pero mutable podria llenarse por accidente y el error aparecerria muy
     * lejos de donde se cometio.
     */
    static class EmptyAttributeSet implements AttributeSet, Serializable {

        public int getAttributeCount() {
            return 0;
        }

        public boolean isDefined(Object attrName) {
            return false;
        }

        public boolean isEqual(AttributeSet attr) {
            return (attr.getAttributeCount() == 0);
        }

        public AttributeSet copyAttributes() {
            return this;
        }

        public Object getAttribute(Object key) {
            return null;
        }

        public Enumeration<Object> getAttributeNames() {
            return new java.util.Vector<Object>().elements();
        }

        public boolean containsAttribute(Object name, Object value) {
            return false;
        }

        public boolean containsAttributes(AttributeSet attributes) {
            return (attributes.getAttributeCount() == 0);
        }

        public AttributeSet getResolveParent() {
            return null;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            return ((obj instanceof AttributeSet) && (((AttributeSet) obj).getAttributeCount() == 0));
        }

        public int hashCode() {
            return 0;
        }
    }
}
