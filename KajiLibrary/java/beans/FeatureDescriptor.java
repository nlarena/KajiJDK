package java.beans;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// The common base of everything Introspector describes: properties, methods, events and the bean
// itself. It keeps what the four share —a name, how to show it, and visibility flags— plus a free
// attribute table so that a tool can hang its own things there without the class having to foresee
// them.
//
// The defaults chain and it is checked against the real JDK: getDisplayName() returns the name when
// nobody set one, and getShortDescription() returns the displayName when nobody set one. That way a
// freshly built descriptor with only setName("en") already answers "en" to all three.
public class FeatureDescriptor {

    private String name;
    private String displayName;
    private String shortDescription;
    private boolean expert;
    private boolean hidden;
    private boolean preferred;

    // Free attributes. It is created lazily: most descriptors never receive one.
    private Map<String, Object> tabla;

    public FeatureDescriptor() {
    }

    // The programmatic name: the property's, the method's or the event's.
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // The name to show. If nobody set one, it falls back to the programmatic name.
    public String getDisplayName() {
        String d = this.displayName;
        if (d == null) {
            d = this.name;
        }
        return d;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    // The short description. If nobody set one, it falls back to the displayName (which may in
    // turn fall back to the name).
    public String getShortDescription() {
        String s = this.shortDescription;
        if (s == null) {
            s = this.getDisplayName();
        }
        return s;
    }

    public void setShortDescription(String text) {
        this.shortDescription = text;
    }

    // "For expert users": a tool may hide it from the basic panel.
    public boolean isExpert() {
        return this.expert;
    }

    public void setExpert(boolean expert) {
        this.expert = expert;
    }

    // "For internal use": it is not shown to the human.
    public boolean isHidden() {
        return this.hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    // "Worth highlighting": the opposite of hidden.
    public boolean isPreferred() {
        return this.preferred;
    }

    public void setPreferred(boolean preferred) {
        this.preferred = preferred;
    }

    // It hangs an arbitrary attribute. Setting null deletes the entry, as in the JDK.
    public void setValue(String attributeName, Object value) {
        if (attributeName != null) {
            if (value == null) {
                if (this.tabla != null) {
                    this.tabla.remove(attributeName);
                }
            } else {
                if (this.tabla == null) {
                    this.tabla = new HashMap<String, Object>();
                }
                this.tabla.put(attributeName, value);
            }
        }
    }

    public Object getValue(String attributeName) {
        Object v = null;
        if (this.tabla != null && attributeName != null) {
            v = this.tabla.get(attributeName);
        }
        return v;
    }

    // The names of the hung attributes. Enumeration and not Iterator: it is the JDK's signature,
    // which predates Iterator and cannot be changed without breaking the contract.
    public Enumeration<String> attributeNames() {
        List<String> nombres = new ArrayList<String>();
        if (this.tabla != null) {
            Object[] claves = this.tabla.keySet().toArray();
            for (int i = 0; i < claves.length; i++) {
                nombres.add((String) claves[i]);
            }
        }
        return new ListEnumeration(nombres);
    }

    // It copies `other`'s fields over this one's. Introspector uses it when merging the descriptor
    // worked out by reflection with the one an explicit BeanInfo contributes.
    void copyFrom(FeatureDescriptor otro) {
        if (otro.name != null) {
            this.name = otro.name;
        }
        if (otro.displayName != null) {
            this.displayName = otro.displayName;
        }
        if (otro.shortDescription != null) {
            this.shortDescription = otro.shortDescription;
        }
        this.expert = this.expert || otro.expert;
        this.hidden = this.hidden || otro.hidden;
        this.preferred = this.preferred || otro.preferred;
        if (otro.tabla != null) {
            Object[] claves = otro.tabla.keySet().toArray();
            for (int i = 0; i < claves.length; i++) {
                String c = (String) claves[i];
                this.setValue(c, otro.tabla.get(c));
            }
        }
    }

    // An Enumeration over an already materialized list. It is walked by index on purpose: in this
    // tree the for-each over a collection does not compile properly (finding #113).
    private static class ListEnumeration implements Enumeration<String> {

        private List<String> datos;
        private int pos;

        ListEnumeration(List<String> datos) {
            this.datos = datos;
            this.pos = 0;
        }

        public boolean hasMoreElements() {
            return this.pos < this.datos.size();
        }

        public String nextElement() {
            String s = this.datos.get(this.pos);
            this.pos = this.pos + 1;
            return s;
        }
    }
}
