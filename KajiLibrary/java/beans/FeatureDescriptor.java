package java.beans;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// La base comun de todo lo que Introspector describe: propiedades, metodos, eventos y el bean
// mismo. Guarda lo que los cuatro comparten —un nombre, como mostrarlo, y banderas de visibilidad—
// mas una tabla de atributos libre para que una herramienta cuelgue lo suyo sin que la clase
// tenga que preverlo.
//
// Los defaults encadenan y esta verificado contra el JDK real: getDisplayName() devuelve el nombre
// cuando nadie fijo uno, y getShortDescription() devuelve el displayName cuando nadie fijo una.
// Asi un descriptor recien construido con solo setName("ene") ya responde "ene" a los tres.
public class FeatureDescriptor {

    private String name;
    private String displayName;
    private String shortDescription;
    private boolean expert;
    private boolean hidden;
    private boolean preferred;

    // Atributos libres. Se crea perezosamente: la mayoria de los descriptores nunca recibe uno.
    private Map<String, Object> tabla;

    public FeatureDescriptor() {
    }

    // El nombre programatico: el de la propiedad, el metodo o el evento.
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // El nombre para mostrar. Si nadie lo fijo, cae al nombre programatico.
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

    // La descripcion corta. Si nadie la fijo, cae al displayName (que a su vez puede caer al nombre).
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

    // "Para usuarios expertos": una herramienta puede esconderlo del panel basico.
    public boolean isExpert() {
        return this.expert;
    }

    public void setExpert(boolean expert) {
        this.expert = expert;
    }

    // "De uso interno": no se muestra al humano.
    public boolean isHidden() {
        return this.hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    // "Vale la pena destacarlo": lo contrario de hidden.
    public boolean isPreferred() {
        return this.preferred;
    }

    public void setPreferred(boolean preferred) {
        this.preferred = preferred;
    }

    // Cuelga un atributo arbitrario. Fijar null borra la entrada, como en el JDK.
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

    // Los nombres de los atributos colgados. Enumeration y no Iterator: es la firma del JDK, que
    // es anterior a Iterator y no se puede cambiar sin romper el contrato.
    public Enumeration<String> attributeNames() {
        List<String> nombres = new ArrayList<String>();
        if (this.tabla != null) {
            Object[] claves = this.tabla.keySet().toArray();
            for (int i = 0; i < claves.length; i++) {
                nombres.add((String) claves[i]);
            }
        }
        return new EnumeracionDeLista(nombres);
    }

    // Copia los campos de `otro` sobre este. Lo usa Introspector al fusionar el descriptor
    // deducido por reflexion con el que aporta un BeanInfo explicito.
    void copiarDe(FeatureDescriptor otro) {
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

    // Enumeration sobre una lista ya materializada. Se recorre por indice a proposito: en este
    // arbol el for-each sobre una coleccion no compila bien (hallazgo #113).
    private static class EnumeracionDeLista implements Enumeration<String> {

        private List<String> datos;
        private int pos;

        EnumeracionDeLista(List<String> datos) {
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
