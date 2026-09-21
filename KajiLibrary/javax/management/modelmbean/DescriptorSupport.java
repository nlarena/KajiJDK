package javax.management.modelmbean;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.management.Descriptor;
import javax.management.MBeanException;
import javax.management.RuntimeOperationsException;

/**
 * KajiLibrary's javax.management.modelmbean.DescriptorSupport -- a mutable descriptor.
 *
 * <p>A map from name to value, and nothing else. Its only peculiarity is that the <b>names do
 * not distinguish case</b>: {@code "name"}, {@code "Name"} and {@code "NAME"} are the same
 * field. That is what the specification says and it has to be replicated -- descriptors are
 * written by hand in configuration files, and there nobody is consistent with capitals.
 *
 * <h2>The pair constructor is the one that bites</h2>
 *
 * <p>{@link #DescriptorSupport(String...)} takes strings of the form {@code "field=value"}, and
 * the two-array one takes names and values separately. Both have existed from the start and the
 * overload is ambiguous to the eye: {@code new DescriptorSupport("a=1", "b=2")} uses the first
 * and {@code new DescriptorSupport(new String[]{"a"}, new Object[]{1})} the second.
 *
 * <p>An empty value --{@code "field="}-- is kept as the empty string and not as null. The
 * difference matters when the value is read back: {@link #getFieldValue} returns {@code ""} and
 * not {@code null}. For {@link #isValid} it makes none: it demands {@code name} and
 * {@code descriptorType} to be present <b>and</b> non-empty.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>{@link #DescriptorSupport(String)} --the one that reads XML-- throws
 * {@link XMLParseException}: reading XML asks for a parser, and this library ships none.
 * {@link #toXMLString} <b>is</b> implemented, because writing needs no parser.
 *
 * <p>It is asymmetric on purpose and worth explaining: what is written with
 * {@code toXMLString} can be read by the JDK, so the implemented half is still useful on its
 * own. The other half throws an exception the constructor already declares.
 *
 * <p>{@link #isValid} also does less than the JDK's: it checks those two fields and stops. The
 * JDK goes on and validates each field's value against what its name allows --a
 * {@code persistPolicy} that is not one of the accepted words, for instance-- and returns false
 * there too.
 */
public class DescriptorSupport implements Descriptor {

    private static final long serialVersionUID = -6292969195866300415L;

    /** The fields, with the name <b>in lower case</b> as the key and the original kept apart. */
    private final Map<String, String> names = new LinkedHashMap<String, String>();

    /** The values, under the same lower-case key. */
    private final Map<String, Object> values = new LinkedHashMap<String, Object>();

    /** Empty. */
    public DescriptorSupport() {
    }

    /**
     * Empty, with an initial size.
     *
     * <p>The size is ignored --the map grows on its own-- and the constructor exists for
     * compatibility.
     *
     * @throws RuntimeOperationsException if it is negative
     */
    public DescriptorSupport(int initNumFields) throws MBeanException, RuntimeOperationsException {
        if (initNumFields < 0) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("Descriptor field limit invalid: " + initNumFields));
        }
    }

    /** A copy. */
    public DescriptorSupport(DescriptorSupport inDescr) {
        if (inDescr == null) {
            return;
        }
        this.names.putAll(inDescr.names);
        this.values.putAll(inDescr.values);
    }

    /**
     * From XML.
     *
     * @throws XMLParseException always in KajiLibrary; see the class note
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
     * With names and values in parallel arrays.
     *
     * @throws RuntimeOperationsException if the lengths do not match, or if a name is null or empty
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
     * With {@code "field=value"} strings.
     *
     * <p>See the class note: an empty value stays as the empty string, not as null.
     *
     * @throws RuntimeOperationsException if one of them has no {@code =}, or if the name is empty
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
     * That field's value.
     *
     * @return null if it is not there
     * @throws RuntimeOperationsException if the name is null or empty
     */
    public synchronized Object getFieldValue(String fieldName) throws RuntimeOperationsException {
        checkName(fieldName);
        return this.values.get(key(fieldName));
    }

    /**
     * Puts it or replaces it.
     *
     * @throws RuntimeOperationsException if the name is null or empty
     */
    public synchronized void setField(String fieldName, Object fieldValue)
        throws RuntimeOperationsException {
        checkName(fieldName);
        String k = key(fieldName);
        // The original name is kept: it is the one that comes out in getFields and in the XML.
        // Replacing a field does not change how it was written the first time, which is what the
        // JDK does.
        if (!this.names.containsKey(k)) {
            this.names.put(k, fieldName);
        }
        this.values.put(k, fieldValue);
    }

    /** All the fields, as {@code "name=value"}. */
    public synchronized String[] getFields() {
        List<String> out = new ArrayList<String>();
        for (Map.Entry<String, String> e : this.names.entrySet()) {
            Object v = this.values.get(e.getKey());
            out.add(e.getValue() + "=" + (v == null ? "" : v.toString()));
        }
        return out.toArray(new String[out.size()]);
    }

    /** Only the names, as they were written. */
    public synchronized String[] getFieldNames() {
        List<String> out = new ArrayList<String>(this.names.values());
        return out.toArray(new String[out.size()]);
    }

    /**
     * The values of those fields, in the same order.
     *
     * <p>A name that is not there gives null in its position; it is not an error. With no arguments
     * it returns <b>all</b> the values, which is the way to take the whole descriptor out at once.
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
     * Puts several at once.
     *
     * @throws RuntimeOperationsException if the lengths do not match
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

    /** A copy. */
    public synchronized Object clone() throws RuntimeOperationsException {
        return new DescriptorSupport(this);
    }

    /** Removes it. If it was not there, it does nothing. */
    public synchronized void removeField(String fieldName) {
        if (fieldName == null || fieldName.length() == 0) {
            return;
        }
        String k = key(fieldName);
        this.names.remove(k);
        this.values.remove(k);
    }

    /** Equal if they have the same fields with the same values. */
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

    /** Consistent with {@link #equals}: a sum, so as not to depend on the order. */
    public synchronized int hashCode() {
        int hash = 0;
        for (Map.Entry<String, Object> e : this.values.entrySet()) {
            Object v = e.getValue();
            hash = hash + e.getKey().hashCode() + (v == null ? 0 : v.hashCode());
        }
        return hash;
    }

    /**
     * Whether it has the minimum to be of use.
     *
     * <p>The minimum is: a {@code name} field and a {@code descriptorType} field, both with a
     * non-empty value. Without them the descriptor cannot be attached to anything, which is the
     * only thing it exists for.
     *
     * <p>It does not go further: see the class note on what the JDK's version also checks.
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
     * The descriptor in XML.
     *
     * <p>See the class note on why writing yes and reading no.
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

    /** The fields, for a log. */
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

    /** The internal key: the name in lower case. See the class note. */
    private static String key(String fieldName) {
        return fieldName.toLowerCase();
    }

    /** That the name is usable. */
    private static void checkName(String fieldName) {
        if (fieldName == null || fieldName.length() == 0) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("Field name to be set is null or empty"));
        }
    }

    /** Escapes what cannot go raw in an XML attribute. */
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
