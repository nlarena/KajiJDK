package javax.accessibility;

import java.util.Vector;

/**
 * Las relaciones de un objeto con otros.
 *
 * <p>A diferencia de {@link AccessibleStateSet}, es un conjunto **por clave**: agregar una relación
 * cuya clave ya está no la duplica sino que le **suma destinos** a la que había. Es lo que se quiere:
 * un objeto puede ser la etiqueta de tres campos, y eso es una relación con tres destinos, no tres
 * relaciones.
 */
public class AccessibleRelationSet {

    /** Las relaciones. Es un {@code Vector} por herencia de la API, no por elección. */
    protected Vector<AccessibleRelation> relations = null;

    /** Un conjunto vacío. */
    public AccessibleRelationSet() {
        this.relations = null;
    }

    /**
     * Con esas relaciones.
     *
     * @throws NullPointerException si el arreglo es `null`
     */
    public AccessibleRelationSet(AccessibleRelation[] relations) {
        if (relations.length != 0) {
            this.relations = new Vector<AccessibleRelation>(relations.length);
            for (int i = 0; i < relations.length; i++) {
                this.add(relations[i]);
            }
        }
    }

    /**
     * Agrega una relación, o le suma destinos a la que ya había con esa clave.
     *
     * @return `true` siempre
     */
    public boolean add(AccessibleRelation relation) {
        if (this.relations == null) {
            this.relations = new Vector<AccessibleRelation>();
        }
        AccessibleRelation existente = this.get(relation.getKey());
        if (existente == null) {
            this.relations.addElement(relation);
            return true;
        }
        // Misma clave: se juntan los destinos en vez de tener dos relaciones iguales.
        Object[] viejos = existente.getTarget();
        Object[] nuevos = relation.getTarget();
        Object[] juntos = new Object[viejos.length + nuevos.length];
        System.arraycopy(viejos, 0, juntos, 0, viejos.length);
        System.arraycopy(nuevos, 0, juntos, viejos.length, nuevos.length);
        existente.setTarget(juntos);
        return true;
    }

    /** Agrega varias relaciones. */
    public void addAll(AccessibleRelation[] relations) {
        if (relations.length != 0) {
            if (this.relations == null) {
                this.relations = new Vector<AccessibleRelation>(relations.length);
            }
            for (int i = 0; i < relations.length; i++) {
                this.add(relations[i]);
            }
        }
    }

    /**
     * Saca una relación.
     *
     * @return `true` si estaba
     */
    public boolean remove(AccessibleRelation relation) {
        if (this.relations == null) {
            return false;
        }
        return this.relations.removeElement(relation);
    }

    /** Deja el conjunto vacío. */
    public void clear() {
        if (this.relations != null) {
            this.relations.removeAllElements();
        }
    }

    /** Cuántas relaciones hay. */
    public int size() {
        if (this.relations == null) {
            return 0;
        }
        return this.relations.size();
    }

    /** Si hay una relación con esa clave. */
    public boolean contains(String key) {
        return this.get(key) != null;
    }

    /**
     * La relación con esa clave.
     *
     * @return la relación, o `null` si no hay
     */
    public AccessibleRelation get(String key) {
        if (this.relations == null) {
            return null;
        }
        for (int i = 0; i < this.relations.size(); i++) {
            AccessibleRelation r = this.relations.elementAt(i);
            if (r.getKey().equals(key)) {
                return r;
            }
        }
        return null;
    }

    /** Las relaciones, como arreglo. */
    public AccessibleRelation[] toArray() {
        if (this.relations == null) {
            return new AccessibleRelation[0];
        }
        AccessibleRelation[] out = new AccessibleRelation[this.relations.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = this.relations.elementAt(i);
        }
        return out;
    }

    public String toString() {
        if (this.relations == null || this.relations.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(this.relations.elementAt(0).toString());
        for (int i = 1; i < this.relations.size(); i++) {
            sb.append(",").append(this.relations.elementAt(i).toString());
        }
        return sb.toString();
    }
}
