package javax.management;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * A {@link Descriptor} that never changes.
 *
 * <p>The three mutators of the interface --{@code setField}, {@code setFields},
 * {@code removeField}-- are there and throw {@link RuntimeOperationsException}. It might look like
 * lying, but it is the opposite: the {@code Descriptor} contract says those methods throw
 * {@code RuntimeOperationsException} when the descriptor is immutable, so fulfilling it <b>is</b>
 * throwing. And that is why {@link #clone()} returns itself: copying what does not change serves
 * no purpose. (An earlier note said four mutators.)
 *
 * <p>Names are kept <b>sorted</b> and compared <b>case-insensitively</b>. The first allows binary
 * search; the second is the JMX rule, and it means a descriptor cannot carry {@code Units} and
 * {@code units} at once.
 */
public class ImmutableDescriptor implements Descriptor {

    private static final long serialVersionUID = 8853308591080540165L;

    /**
     * The descriptor without fields. It is shared because there is nothing that can be done to it.
     */
    public static final ImmutableDescriptor EMPTY_DESCRIPTOR = new ImmutableDescriptor();

    /**
     * @serial the names, sorted case-insensitively
     */
    private final String[] names;

    /**
     * @serial the values, in the order of the names
     */
    private final Object[] values;

    private transient int hashCode = -1;

    /**
     * @throws IllegalArgumentException if the arrays do not have the same length, if a name is null
     *     or empty, or if a name is repeated with another value
     */
    public ImmutableDescriptor(String[] fieldNames, Object[] fieldValues) {
        if (fieldNames == null || fieldValues == null) {
            throw new IllegalArgumentException("Null array parameter");
        }
        if (fieldNames.length != fieldValues.length) {
            throw new IllegalArgumentException("Different size arrays");
        }
        Map<String, Object> map = sort(fieldNames, fieldValues);
        int n = map.size();
        names = new String[n];
        values = new Object[n];
        int i = 0;
        Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> e = it.next();
            names[i] = e.getKey();
            values[i] = e.getValue();
            i++;
        }
    }

    /**
     * Each string is {@code "name=value"}, cut at the <b>first</b> {@code =}.
     *
     * <p>That the cut is at the first and not the last matters: a value may contain {@code =} and a
     * name may not.
     */
    public ImmutableDescriptor(String... fields) {
        if (fields == null) {
            throw new IllegalArgumentException("Null fields parameter");
        }
        String[] ns = new String[fields.length];
        Object[] vs = new Object[fields.length];
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            if (field == null || field.length() == 0) {
                throw new IllegalArgumentException("Empty field name");
            }
            int eq = field.indexOf('=');
            if (eq < 0) {
                throw new IllegalArgumentException("Missing = character: " + field);
            }
            ns[i] = field.substring(0, eq);
            vs[i] = field.substring(eq + 1);
        }
        Map<String, Object> map = sort(ns, vs);
        int n = map.size();
        names = new String[n];
        values = new Object[n];
        int i = 0;
        Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> e = it.next();
            names[i] = e.getKey();
            values[i] = e.getValue();
            i++;
        }
    }

    /** From a map; the map's order does not matter, it is sorted anyway. */
    public ImmutableDescriptor(Map<String, ?> fields) {
        if (fields == null) {
            throw new IllegalArgumentException("Null Map");
        }
        TreeMap<String, Object> map = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        Iterator<? extends Map.Entry<String, ?>> it = fields.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ?> e = it.next();
            String k = e.getKey();
            if (k == null || k.length() == 0) {
                throw new IllegalArgumentException("Empty or null key");
            }
            map.put(k, e.getValue());
        }
        int n = map.size();
        names = new String[n];
        values = new Object[n];
        int i = 0;
        Iterator<Map.Entry<String, Object>> it2 = map.entrySet().iterator();
        while (it2.hasNext()) {
            Map.Entry<String, Object> e = it2.next();
            names[i] = e.getKey();
            values[i] = e.getValue();
            i++;
        }
    }

    private static Map<String, Object> sort(String[] ns, Object[] vs) {
        TreeMap<String, Object> map = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        for (int i = 0; i < ns.length; i++) {
            if (ns[i] == null || ns[i].length() == 0) {
                throw new IllegalArgumentException("Empty or null field name");
            }
            Object previous = map.put(ns[i], vs[i]);
            if (previous != null && !previous.equals(vs[i])) {
                throw new IllegalArgumentException("Duplicate field name: " + ns[i]);
            }
        }
        return map;
    }

    /**
     * Merges several descriptors into one.
     *
     * <p>The <b>first</b> that defines each field wins; if two define the same one with different
     * values, it is an error and not a silent choice.
     *
     * @throws IllegalArgumentException on a field repeated with different values
     */
    public static ImmutableDescriptor union(Descriptor... descriptors) {
        TreeMap<String, Object> map = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        for (int i = 0; i < descriptors.length; i++) {
            Descriptor d = descriptors[i];
            if (d == null) {
                continue;
            }
            String[] ns = d.getFieldNames();
            Object[] vs = d.getFieldValues(ns);
            for (int j = 0; j < ns.length; j++) {
                if (map.containsKey(ns[j])) {
                    Object old = map.get(ns[j]);
                    if (old == null ? vs[j] != null : !old.equals(vs[j])) {
                        throw new IllegalArgumentException("Inconsistent values for descriptor "
                                + "field " + ns[j]);
                    }
                } else {
                    map.put(ns[j], vs[j]);
                }
            }
        }
        if (map.isEmpty()) {
            return EMPTY_DESCRIPTOR;
        }
        return new ImmutableDescriptor(map);
    }

    /** Binary search over the sorted names, case-insensitively. */
    private int index(String name) {
        return Arrays.binarySearch(names, name, String.CASE_INSENSITIVE_ORDER);
    }

    public final Object getFieldValue(String fieldName) {
        checkName(fieldName);
        int i = index(fieldName);
        return i < 0 ? null : values[i];
    }

    /** Each field as {@code "name=value"}. */
    public final String[] getFields() {
        String[] r = new String[names.length];
        for (int i = 0; i < names.length; i++) {
            Object v = values[i];
            r[i] = names[i] + "=" + (v == null ? "" : String.valueOf(v));
        }
        return r;
    }

    /**
     * The requested values. With a {@code null} argument it returns <b>all</b> of them; with no
     * arguments the varargs array is empty and so is the result. (An earlier note said the
     * no-argument call also returns all.)
     *
     * <p>A name that is not there gives {@code null} in its position, not a gap: the answer always
     * has the same length as the request.
     */
    public final Object[] getFieldValues(String... fieldNames) {
        if (fieldNames == null) {
            Object[] r = new Object[values.length];
            System.arraycopy(values, 0, r, 0, values.length);
            return r;
        }
        Object[] r = new Object[fieldNames.length];
        for (int i = 0; i < fieldNames.length; i++) {
            String n = fieldNames[i];
            int j = (n == null || n.length() == 0) ? -1 : index(n);
            r[i] = j < 0 ? null : values[j];
        }
        return r;
    }

    /** The names, already sorted. */
    public final String[] getFieldNames() {
        String[] r = new String[names.length];
        System.arraycopy(names, 0, r, 0, names.length);
        return r;
    }

    /** Against any {@link Descriptor}, not only against another immutable one. */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Descriptor)) {
            return false;
        }
        String[] others;
        Object[] otherVals;
        if (o instanceof ImmutableDescriptor) {
            others = ((ImmutableDescriptor) o).names;
            otherVals = ((ImmutableDescriptor) o).values;
        } else {
            others = ((Descriptor) o).getFieldNames();
            Arrays.sort(others, String.CASE_INSENSITIVE_ORDER);
            otherVals = ((Descriptor) o).getFieldValues(others);
        }
        if (names.length != others.length) {
            return false;
        }
        for (int i = 0; i < names.length; i++) {
            if (!names[i].equalsIgnoreCase(others[i])) {
                return false;
            }
        }
        return Arrays.deepEquals(values, otherVals);
    }

    /**
     * Computed once and kept: the object is immutable, so the value does not change either.
     *
     * <p>Names go in lower case so that two descriptors that differ only in the case of the names
     * --and are therefore equal-- give the same number.
     */
    public int hashCode() {
        if (hashCode == -1) {
            int h = 0;
            for (int i = 0; i < names.length; i++) {
                Object v = values[i];
                int hv;
                if (v == null) {
                    hv = 0;
                } else if (v instanceof Object[]) {
                    hv = Arrays.deepHashCode((Object[]) v);
                } else {
                    hv = v.hashCode();
                }
                h += names[i].toLowerCase().hashCode() ^ hv;
            }
            hashCode = h;
        }
        return hashCode;
    }

    /** {@code {a=1, b=2}}. */
    public String toString() {
        StringBuilder b = new StringBuilder("{");
        for (int i = 0; i < names.length; i++) {
            if (i > 0) {
                b.append(", ");
            }
            b.append(names[i]).append("=").append(String.valueOf(values[i]));
        }
        return b.append("}").toString();
    }

    /**
     * Always {@code true}, as in the JDK, whose {@code ImmutableDescriptor.isValid} also just
     * returns {@code true}; the fields that get validated are checked in {@code modelmbean}. (An
     * earlier note gave another reason: that the JDK validates only the fields it knows.)
     */
    public boolean isValid() {
        return true;
    }

    /** Returns itself: it does not change, so there is nothing to copy. */
    public Descriptor clone() {
        return this;
    }

    /** @throws RuntimeOperationsException always: the descriptor is immutable */
    public final void setFields(String[] fieldNames, Object[] fieldValues)
            throws RuntimeOperationsException {
        unsupported();
    }

    /** @throws RuntimeOperationsException always: the descriptor is immutable */
    public final void setField(String fieldName, Object fieldValue)
            throws RuntimeOperationsException {
        unsupported();
    }

    /**
     * Does nothing if the field is not there; if it is, throws, because the descriptor is
     * immutable.
     */
    public final void removeField(String fieldName) {
        if (fieldName != null && index(fieldName) >= 0) {
            unsupported();
        }
    }

    private static void unsupported() {
        throw new RuntimeOperationsException(
                new UnsupportedOperationException("Descriptor is read-only"));
    }

    private static void checkName(String fieldName) {
        if (fieldName == null || fieldName.length() == 0) {
            throw new RuntimeOperationsException(
                    new IllegalArgumentException("Null or empty field name"));
        }
    }
}
