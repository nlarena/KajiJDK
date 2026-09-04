package javax.management;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Un {@link Descriptor} que no cambia nunca.
 *
 * <p>Los cuatro mutadores de la interfaz --{@code setField}, {@code setFields},
 * {@code removeField}-- estan y tiran {@link RuntimeOperationsException}. Podria parecer que
 * mentir, pero es lo contrario: el contrato de `Descriptor` dice que esos metodos tiran
 * `RuntimeOperationsException` cuando el descriptor es inmutable, asi que cumplirlo <b>es</b>
 * tirar. Y por eso {@link #clone()} se devuelve a si mismo: copiar lo que no cambia no sirve para
 * nada.
 *
 * <p>Los nombres se guardan <b>ordenados</b> y se comparan <b>sin distinguir mayusculas</b>. Lo
 * primero permite buscar por biseccion; lo segundo es la regla de JMX, y hace que un descriptor no
 * pueda llevar {@code Units} y {@code units} a la vez.
 */
public class ImmutableDescriptor implements Descriptor {

    private static final long serialVersionUID = 8853308591080540165L;

    /** El descriptor sin campos. Se comparte porque no hay nada que se le pueda hacer. */
    public static final ImmutableDescriptor EMPTY_DESCRIPTOR = new ImmutableDescriptor();

    /**
     * @serial los nombres, ordenados sin distinguir mayusculas
     */
    private final String[] names;

    /**
     * @serial los valores, en el orden de los nombres
     */
    private final Object[] values;

    private transient int hashCode = -1;

    /**
     * @throws IllegalArgumentException si los arreglos no miden lo mismo, si un nombre es nulo o
     *     vacio, o si un nombre se repite con otro valor
     */
    public ImmutableDescriptor(String[] fieldNames, Object[] fieldValues) {
        if (fieldNames == null || fieldValues == null) {
            throw new IllegalArgumentException("Null array parameter");
        }
        if (fieldNames.length != fieldValues.length) {
            throw new IllegalArgumentException("Different size arrays");
        }
        Map<String, Object> mapa = ordenar(fieldNames, fieldValues);
        int n = mapa.size();
        names = new String[n];
        values = new Object[n];
        int i = 0;
        Iterator<Map.Entry<String, Object>> it = mapa.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> e = it.next();
            names[i] = e.getKey();
            values[i] = e.getValue();
            i++;
        }
    }

    /**
     * Cada cadena es {@code "nombre=valor"}, cortada en el <b>primer</b> {@code =}.
     *
     * <p>Que el corte sea en el primero y no en el ultimo importa: un valor puede llevar `=` y un
     * nombre no.
     */
    public ImmutableDescriptor(String... fields) {
        if (fields == null) {
            throw new IllegalArgumentException("Null fields parameter");
        }
        String[] ns = new String[fields.length];
        Object[] vs = new Object[fields.length];
        for (int i = 0; i < fields.length; i++) {
            String campo = fields[i];
            if (campo == null || campo.length() == 0) {
                throw new IllegalArgumentException("Empty field name");
            }
            int eq = campo.indexOf('=');
            if (eq < 0) {
                throw new IllegalArgumentException("Missing = character: " + campo);
            }
            ns[i] = campo.substring(0, eq);
            vs[i] = campo.substring(eq + 1);
        }
        Map<String, Object> mapa = ordenar(ns, vs);
        int n = mapa.size();
        names = new String[n];
        values = new Object[n];
        int i = 0;
        Iterator<Map.Entry<String, Object>> it = mapa.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> e = it.next();
            names[i] = e.getKey();
            values[i] = e.getValue();
            i++;
        }
    }

    /** Desde un mapa; el orden del mapa no importa, se reordena igual. */
    public ImmutableDescriptor(Map<String, ?> fields) {
        if (fields == null) {
            throw new IllegalArgumentException("Null Map");
        }
        TreeMap<String, Object> mapa = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        Iterator<? extends Map.Entry<String, ?>> it = fields.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ?> e = it.next();
            String k = e.getKey();
            if (k == null || k.length() == 0) {
                throw new IllegalArgumentException("Empty or null key");
            }
            mapa.put(k, e.getValue());
        }
        int n = mapa.size();
        names = new String[n];
        values = new Object[n];
        int i = 0;
        Iterator<Map.Entry<String, Object>> it2 = mapa.entrySet().iterator();
        while (it2.hasNext()) {
            Map.Entry<String, Object> e = it2.next();
            names[i] = e.getKey();
            values[i] = e.getValue();
            i++;
        }
    }

    private static Map<String, Object> ordenar(String[] ns, Object[] vs) {
        TreeMap<String, Object> mapa = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        for (int i = 0; i < ns.length; i++) {
            if (ns[i] == null || ns[i].length() == 0) {
                throw new IllegalArgumentException("Empty or null field name");
            }
            Object previo = mapa.put(ns[i], vs[i]);
            if (previo != null && !previo.equals(vs[i])) {
                throw new IllegalArgumentException("Duplicate field name: " + ns[i]);
            }
        }
        return mapa;
    }

    /**
     * Junta varios descriptores en uno.
     *
     * <p>Gana el <b>primero</b> que define cada campo; si dos definen el mismo con valores
     * distintos, es un error y no una eleccion silenciosa.
     *
     * @throws IllegalArgumentException ante un campo repetido con valores distintos
     */
    public static ImmutableDescriptor union(Descriptor... descriptors) {
        TreeMap<String, Object> mapa = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        for (int i = 0; i < descriptors.length; i++) {
            Descriptor d = descriptors[i];
            if (d == null) {
                continue;
            }
            String[] ns = d.getFieldNames();
            Object[] vs = d.getFieldValues(ns);
            for (int j = 0; j < ns.length; j++) {
                if (mapa.containsKey(ns[j])) {
                    Object viejo = mapa.get(ns[j]);
                    if (viejo == null ? vs[j] != null : !viejo.equals(vs[j])) {
                        throw new IllegalArgumentException("Inconsistent values for descriptor "
                                + "field " + ns[j]);
                    }
                } else {
                    mapa.put(ns[j], vs[j]);
                }
            }
        }
        if (mapa.isEmpty()) {
            return EMPTY_DESCRIPTOR;
        }
        return new ImmutableDescriptor(mapa);
    }

    /** Biseccion sobre los nombres ordenados, sin distinguir mayusculas. */
    private int indice(String name) {
        return Arrays.binarySearch(names, name, String.CASE_INSENSITIVE_ORDER);
    }

    public final Object getFieldValue(String fieldName) {
        revisarNombre(fieldName);
        int i = indice(fieldName);
        return i < 0 ? null : values[i];
    }

    /** Cada campo como {@code "nombre=valor"}. */
    public final String[] getFields() {
        String[] r = new String[names.length];
        for (int i = 0; i < names.length; i++) {
            Object v = values[i];
            r[i] = names[i] + "=" + (v == null ? "" : String.valueOf(v));
        }
        return r;
    }

    /**
     * Los valores pedidos. Sin argumentos --o con `null`-- devuelve <b>todos</b>.
     *
     * <p>Un nombre que no esta da `null` en su posicion, no un hueco: la respuesta siempre mide lo
     * mismo que el pedido.
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
            int j = (n == null || n.length() == 0) ? -1 : indice(n);
            r[i] = j < 0 ? null : values[j];
        }
        return r;
    }

    /** Los nombres, ya ordenados. */
    public final String[] getFieldNames() {
        String[] r = new String[names.length];
        System.arraycopy(names, 0, r, 0, names.length);
        return r;
    }

    /** Contra cualquier {@link Descriptor}, no solo contra otro inmutable. */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Descriptor)) {
            return false;
        }
        String[] otros;
        Object[] otrosVal;
        if (o instanceof ImmutableDescriptor) {
            otros = ((ImmutableDescriptor) o).names;
            otrosVal = ((ImmutableDescriptor) o).values;
        } else {
            otros = ((Descriptor) o).getFieldNames();
            Arrays.sort(otros, String.CASE_INSENSITIVE_ORDER);
            otrosVal = ((Descriptor) o).getFieldValues(otros);
        }
        if (names.length != otros.length) {
            return false;
        }
        for (int i = 0; i < names.length; i++) {
            if (!names[i].equalsIgnoreCase(otros[i])) {
                return false;
            }
        }
        return Arrays.deepEquals(values, otrosVal);
    }

    /**
     * Se calcula una vez y se guarda: el objeto es inmutable, asi que el valor tampoco cambia.
     *
     * <p>Los nombres entran en minusculas para que dos descriptores que solo difieren en la caja de
     * los nombres --y que por lo tanto son iguales-- den el mismo numero.
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
     * Siempre `true`.
     *
     * <p>El JDK solo valida aca los campos que <b>el</b> conoce, y un descriptor de este paquete no
     * lleva ninguno de esos: los que se validan viven en `modelmbean`.
     */
    public boolean isValid() {
        return true;
    }

    /** Se devuelve a si mismo: no cambia, asi que no hay nada que copiar. */
    public Descriptor clone() {
        return this;
    }

    /** @throws RuntimeOperationsException siempre: el descriptor es inmutable */
    public final void setFields(String[] fieldNames, Object[] fieldValues)
            throws RuntimeOperationsException {
        noSePuede();
    }

    /** @throws RuntimeOperationsException siempre: el descriptor es inmutable */
    public final void setField(String fieldName, Object fieldValue)
            throws RuntimeOperationsException {
        noSePuede();
    }

    /** No hace nada si el campo no esta; si esta, tira, porque el descriptor es inmutable. */
    public final void removeField(String fieldName) {
        if (fieldName != null && indice(fieldName) >= 0) {
            noSePuede();
        }
    }

    private static void noSePuede() {
        throw new RuntimeOperationsException(
                new UnsupportedOperationException("Descriptor is read-only"));
    }

    private static void revisarNombre(String fieldName) {
        if (fieldName == null || fieldName.length() == 0) {
            throw new RuntimeOperationsException(
                    new IllegalArgumentException("Null or empty field name"));
        }
    }
}
