package javax.accessibility;

import java.util.Vector;

/**
 * An object's relations with others.
 *
 * <p>Unlike {@link AccessibleStateSet}, it is a set **by key**: adding a relation whose key is
 * already there does not duplicate it but **adds targets** to the one there was. It is what is
 * wanted: an object may be the label of three fields, and that is one relation with three targets,
 * not three relations.
 */
public class AccessibleRelationSet {

    /** The relations. It is a {@code Vector} because of the API's inheritance, not by choice. */
    protected Vector<AccessibleRelation> relations = null;

    /** An empty set. */
    public AccessibleRelationSet() {
        this.relations = null;
    }

    /**
     * With those relations.
     *
     * @throws NullPointerException if the array is `null`
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
     * Adds a relation, or adds targets to the one there already was with that key.
     *
     * @return `true` always
     */
    public boolean add(AccessibleRelation relation) {
        if (this.relations == null) {
            this.relations = new Vector<AccessibleRelation>();
        }
        AccessibleRelation existing = this.get(relation.getKey());
        if (existing == null) {
            this.relations.addElement(relation);
            return true;
        }
        // Same key: the targets are merged instead of having two equal relations.
        Object[] oldTargets = existing.getTarget();
        Object[] newTargets = relation.getTarget();
        Object[] merged = new Object[oldTargets.length + newTargets.length];
        System.arraycopy(oldTargets, 0, merged, 0, oldTargets.length);
        System.arraycopy(newTargets, 0, merged, oldTargets.length, newTargets.length);
        existing.setTarget(merged);
        return true;
    }

    /** Adds several relations. */
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
     * Removes a relation.
     *
     * @return `true` if it was there
     */
    public boolean remove(AccessibleRelation relation) {
        if (this.relations == null) {
            return false;
        }
        return this.relations.removeElement(relation);
    }

    /** Leaves the set empty. */
    public void clear() {
        if (this.relations != null) {
            this.relations.removeAllElements();
        }
    }

    /** How many relations there are. */
    public int size() {
        if (this.relations == null) {
            return 0;
        }
        return this.relations.size();
    }

    /** Whether there is a relation with that key. */
    public boolean contains(String key) {
        return this.get(key) != null;
    }

    /**
     * The relation with that key.
     *
     * @return the relation, or `null` if there is none
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

    /** The relations, as an array. */
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
