package javax.management.modelmbean;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.management.Descriptor;
import javax.management.MBeanException;
import javax.management.RuntimeOperationsException;

/**
 * KajiLibrary's javax.management.modelmbean.DescriptorSupport -- un descriptor mutable.
 *
 * <p>Un mapa de nombre a valor, y nada mas. Su unica particularidad es que los <b>nombres no
 * distinguen mayusculas</b>: {@code "name"}, {@code "Name"} y {@code "NAME"} son el mismo campo. Es
 * lo que dice la especificacion y hay que replicarlo -- los descriptores se escriben a mano en
 * archivos de configuracion, y ahi nadie es consistente con las mayusculas.
 *
 * <h2>El constructor de pares es el que muerde</h2>
 *
 * <p>{@link #DescriptorSupport(String...)} recibe cadenas con la forma {@code "campo=valor"}, y el
 * de dos arreglos recibe nombres y valores por separado. Los dos existen desde el principio y la
 * sobrecarga es ambigua a la vista: {@code new DescriptorSupport("a=1", "b=2")} usa el primero y
 * {@code new DescriptorSupport(new String[]{"a"}, new Object[]{1})} el segundo.
 *
 * <p>Un valor vacio --{@code "campo="}-- se guarda como cadena vacia y no como null. La diferencia
 * importa: {@link #isValid} rechaza un descriptor cuyos campos obligatorios esten en null, y la
 * cadena vacia pasa.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>{@link #DescriptorSupport(String)} --el que lee XML-- lanza {@link XMLParseException}: leer XML
 * pide un analizador, y esta biblioteca no trae ninguno. {@link #toXMLString} <b>si</b> esta
 * implementado, porque escribir no necesita analizador.
 *
 * <p>Es asimetrico a proposito y vale explicarlo: lo que se escribe con {@code toXMLString} lo puede
 * leer el JDK, asi que la mitad implementada sigue siendo util por su cuenta. La otra mitad lanza una
 * excepcion que el constructor ya declara.
 */
public class DescriptorSupport implements Descriptor {

    private static final long serialVersionUID = -6292969195866300415L;

    /** Los campos, con el nombre <b>en minusculas</b> como clave y el original guardado aparte. */
    private final Map<String, String> names = new LinkedHashMap<String, String>();

    /** Los valores, por la misma clave en minusculas. */
    private final Map<String, Object> values = new LinkedHashMap<String, Object>();

    /** Vacio. */
    public DescriptorSupport() {
    }

    /**
     * Vacio, con un tamano inicial.
     *
     * <p>El tamano se ignora --el mapa crece solo-- y el constructor existe por compatibilidad.
     *
     * @throws RuntimeOperationsException si es negativo
     */
    public DescriptorSupport(int initNumFields) throws MBeanException, RuntimeOperationsException {
        if (initNumFields < 0) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("Descriptor field limit invalid: " + initNumFields));
        }
    }

    /** Una copia. */
    public DescriptorSupport(DescriptorSupport inDescr) {
        if (inDescr == null) {
            return;
        }
        this.names.putAll(inDescr.names);
        this.values.putAll(inDescr.values);
    }

    /**
     * Desde XML.
     *
     * @throws XMLParseException siempre en KajiLibrary; ver la nota de la clase
     */
    public DescriptorSupport(String inStr)
        throws MBeanException, RuntimeOperationsException, XMLParseException {
        if (inStr == null) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("String in parameter is null"));
        }
        throw new XMLParseException(
            "KajiLibrary includes no XML parser; a descriptor cannot be read from XML");
    }

    /**
     * Con nombres y valores en arreglos paralelos.
     *
     * @throws RuntimeOperationsException si los largos no coinciden, o si un nombre es null o vacio
     */
    public DescriptorSupport(String[] fieldNames, Object[] fieldValues)
        throws RuntimeOperationsException {
        if (fieldNames == null || fieldValues == null) {
            return;
        }
        if (fieldNames.length != fieldValues.length) {
            throw new RuntimeOperationsException(new IllegalArgumentException(
                "Descriptor field names and values are not the same length"));
        }
        int i = 0;
        while (i < fieldNames.length) {
            setField(fieldNames[i], fieldValues[i]);
            i = i + 1;
        }
    }

    /**
     * Con cadenas {@code "campo=valor"}.
     *
     * <p>Ver la nota de la clase: un valor vacio queda como cadena vacia, no como null.
     *
     * @throws RuntimeOperationsException si alguna no tiene {@code =}, o si el nombre esta vacio
     */
    public DescriptorSupport(String... fields) {
        if (fields == null) {
            return;
        }
        int i = 0;
        while (i < fields.length) {
            String pair = fields[i];
            if (pair == null || pair.length() == 0) {
                i = i + 1;
                continue;
            }
            int eq = pair.indexOf('=');
            if (eq < 0) {
                throw new RuntimeOperationsException(new IllegalArgumentException(
                    "Field in invalid format: no equals sign"));
            }
            String name = pair.substring(0, eq);
            String value = pair.substring(eq + 1);
            if (name.length() == 0) {
                throw new RuntimeOperationsException(new IllegalArgumentException(
                    "Field in invalid format: no name"));
            }
            setField(name, value);
            i = i + 1;
        }
    }

    /**
     * El valor de ese campo.
     *
     * @return null si no esta
     * @throws RuntimeOperationsException si el nombre es null o vacio
     */
    public synchronized Object getFieldValue(String fieldName) throws RuntimeOperationsException {
        checkName(fieldName);
        return this.values.get(key(fieldName));
    }

    /**
     * Lo pone o lo reemplaza.
     *
     * @throws RuntimeOperationsException si el nombre es null o vacio
     */
    public synchronized void setField(String fieldName, Object fieldValue)
        throws RuntimeOperationsException {
        checkName(fieldName);
        String k = key(fieldName);
        // El nombre original se conserva: es el que sale en getFields y en el XML. Reemplazar un
        // campo no cambia como estaba escrito la primera vez, que es lo que hace el JDK.
        if (!this.names.containsKey(k)) {
            this.names.put(k, fieldName);
        }
        this.values.put(k, fieldValue);
    }

    /** Todos los campos, como {@code "nombre=valor"}. */
    public synchronized String[] getFields() {
        List<String> out = new ArrayList<String>();
        for (Map.Entry<String, String> e : this.names.entrySet()) {
            Object v = this.values.get(e.getKey());
            out.add(e.getValue() + "=" + (v == null ? "" : v.toString()));
        }
        return out.toArray(new String[out.size()]);
    }

    /** Solo los nombres, como se escribieron. */
    public synchronized String[] getFieldNames() {
        List<String> out = new ArrayList<String>(this.names.values());
        return out.toArray(new String[out.size()]);
    }

    /**
     * Los valores de esos campos, en el mismo orden.
     *
     * <p>Un nombre que no esta da null en su posicion; no es un error. Sin argumentos devuelve
     * <b>todos</b> los valores, que es la forma de sacar el descriptor entero de una.
     */
    public synchronized Object[] getFieldValues(String... fieldNames) {
        if (fieldNames == null || fieldNames.length == 0) {
            List<Object> all = new ArrayList<Object>();
            for (String k : this.names.keySet()) {
                all.add(this.values.get(k));
            }
            return all.toArray(new Object[all.size()]);
        }
        Object[] out = new Object[fieldNames.length];
        int i = 0;
        while (i < fieldNames.length) {
            String n = fieldNames[i];
            out[i] = (n == null) ? null : this.values.get(key(n));
            i = i + 1;
        }
        return out;
    }

    /**
     * Pone varios de una.
     *
     * @throws RuntimeOperationsException si los largos no coinciden
     */
    public synchronized void setFields(String[] fieldNames, Object[] fieldValues)
        throws RuntimeOperationsException {
        if (fieldNames == null || fieldValues == null) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("Descriptor field names or values are null"));
        }
        if (fieldNames.length != fieldValues.length) {
            throw new RuntimeOperationsException(new IllegalArgumentException(
                "Descriptor field names and values are not the same length"));
        }
        int i = 0;
        while (i < fieldNames.length) {
            setField(fieldNames[i], fieldValues[i]);
            i = i + 1;
        }
    }

    /** Una copia. */
    public synchronized Object clone() throws RuntimeOperationsException {
        return new DescriptorSupport(this);
    }

    /** Lo saca. Si no estaba, no hace nada. */
    public synchronized void removeField(String fieldName) {
        if (fieldName == null || fieldName.length() == 0) {
            return;
        }
        String k = key(fieldName);
        this.names.remove(k);
        this.values.remove(k);
    }

    /** Iguales si tienen los mismos campos con los mismos valores. */
    public synchronized boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Descriptor)) {
            return false;
        }
        Descriptor that = (Descriptor) obj;
        String[] theirNames = that.getFieldNames();
        if (theirNames.length != this.names.size()) {
            return false;
        }
        int i = 0;
        while (i < theirNames.length) {
            String k = key(theirNames[i]);
            if (!this.values.containsKey(k)) {
                return false;
            }
            Object mine = this.values.get(k);
            Object theirs = that.getFieldValue(theirNames[i]);
            if (mine == null ? theirs != null : !mine.equals(theirs)) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /** Coherente con {@link #equals}: suma, para no depender del orden. */
    public synchronized int hashCode() {
        int hash = 0;
        for (Map.Entry<String, Object> e : this.values.entrySet()) {
            Object v = e.getValue();
            hash = hash + e.getKey().hashCode() + (v == null ? 0 : v.hashCode());
        }
        return hash;
    }

    /**
     * Si tiene lo minimo para servir.
     *
     * <p>Lo minimo es: un campo {@code name} y un campo {@code descriptorType}, los dos con valor.
     * Sin ellos el descriptor no se puede asociar a nada, que es lo unico para lo que existe.
     */
    public synchronized boolean isValid() throws RuntimeOperationsException {
        Object name = this.values.get("name");
        Object type = this.values.get("descriptortype");
        if (name == null || type == null) {
            return false;
        }
        return name.toString().length() > 0 && type.toString().length() > 0;
    }

    /**
     * El descriptor en XML.
     *
     * <p>Ver la nota de la clase sobre por que escribir si y leer no.
     */
    public synchronized String toXMLString() {
        StringBuilder sb = new StringBuilder("<Descriptor>");
        for (Map.Entry<String, String> e : this.names.entrySet()) {
            Object v = this.values.get(e.getKey());
            sb.append("<field name=\"").append(escape(e.getValue())).append("\" value=\"")
                .append(escape(v == null ? "" : v.toString())).append("\"></field>");
        }
        sb.append("</Descriptor>");
        return sb.toString();
    }

    /** Los campos, para un registro. */
    public synchronized String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : this.names.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            Object v = this.values.get(e.getKey());
            sb.append(e.getValue()).append("=").append(v == null ? "" : v.toString());
        }
        return sb.toString();
    }

    /** La clave interna: el nombre en minusculas. Ver la nota de la clase. */
    private static String key(String fieldName) {
        return fieldName.toLowerCase();
    }

    /** Que el nombre sirva. */
    private static void checkName(String fieldName) {
        if (fieldName == null || fieldName.length() == 0) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("Field name to be set is null or empty"));
        }
    }

    /** Escapa lo que no puede ir crudo en un atributo XML. */
    private static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '&') {
                sb.append("&amp;");
            } else if (c == '<') {
                sb.append("&lt;");
            } else if (c == '>') {
                sb.append("&gt;");
            } else if (c == '"') {
                sb.append("&quot;");
            } else {
                sb.append(c);
            }
            i = i + 1;
        }
        return sb.toString();
    }
}
