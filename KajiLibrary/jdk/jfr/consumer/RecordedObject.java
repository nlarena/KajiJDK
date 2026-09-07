package jdk.jfr.consumer;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jdk.jfr.Timespan;
import jdk.jfr.Timestamp;
import jdk.jfr.ValueDescriptor;

/**
 * Un objeto leido de una grabacion: campos con nombre y sin clase Java.
 *
 * <h2>Por que no es un objeto normal</h2>
 *
 * <p>Porque el proceso que lee una grabacion casi nunca tiene las clases del que la escribio. Puede
 * ser otra maquina, otra version, otro programa entero. Deserializar a objetos tipados exigiria
 * tener esas clases en el classpath, que es justamente lo que no se puede pedir.
 *
 * <p>La salida es esta: los datos quedan como pares nombre-valor, con los
 * {@link ValueDescriptor descriptores} al lado explicando que es cada uno. Se accede por nombre y
 * el tipo lo pone el que lee, que es el que sabe que espera.
 *
 * <h2>Los getters tipados no son azucar</h2>
 *
 * <p>{@link #getInt} podria ser {@code (int) getValue(...)} y no lo es: hace la conversion
 * <strong>ensanchando</strong>. Un campo grabado como {@code short} se lee con {@code getInt} sin
 * problema, que es lo que hace falta cuando el que lee no sabe con que ancho se grabo — y no lo
 * sabe, porque eso depende de la version del JDK que produjo el archivo.
 *
 * <p>{@link #getDuration} y {@link #getInstant} van mas lejos: leen un {@code long} y lo
 * interpretan segun la anotacion {@link Timespan} o {@link Timestamp} del campo. Sin eso, el que
 * lee tendria que saber en que unidad se grabo cada campo de cada evento.
 *
 * <h2>Estado en esta VM</h2>
 *
 * <p>Toda esta clase es real y funciona: dados los descriptores y los valores, la lectura tipada,
 * la conversion de unidades y {@link #toString} hacen lo que dicen.
 *
 * <p>Lo que no hay es de donde sacar esos valores, porque {@link RecordingFile} no puede leer el
 * formato binario. Con la lectura del archivo escrita, esta clase anda sin tocarla.
 *
 * @since 9
 */
public class RecordedObject {

    private final List<ValueDescriptor> descriptores;
    private final Object[] valores;

    RecordedObject(final List<ValueDescriptor> descriptores, final Object[] valores) {
        this.descriptores = Collections.unmodifiableList(
                new ArrayList<ValueDescriptor>(descriptores));
        this.valores = valores.clone();
    }

    /**
     * Si el objeto tiene un campo con ese nombre.
     *
     * <p>Acepta nombres con puntos para llegar a un campo anidado, como {@code "thread.javaName"}:
     * un evento tiene objetos adentro y esta es la forma de recorrerlos sin ir sacando uno por uno.
     *
     * @param name el nombre
     * @return si existe
     */
    public boolean hasField(final String name) {
        Objects.requireNonNull(name, "name");
        final int punto = name.indexOf('.');
        if (punto < 0) {
            return indice(name) >= 0;
        }
        final int i = indice(name.substring(0, punto));
        if (i < 0) {
            return false;
        }
        final Object v = valores[i];
        return v instanceof RecordedObject
                && ((RecordedObject) v).hasField(name.substring(punto + 1));
    }

    /**
     * El valor de ese campo, sin convertir.
     *
     * <p>Acepta nombres con puntos, igual que {@link #hasField}.
     *
     * @param <T> el tipo esperado; no se comprueba
     * @param name el nombre
     * @return el valor
     * @throws IllegalArgumentException si no hay un campo con ese nombre
     */
    @SuppressWarnings("unchecked")
    public final <T> T getValue(final String name) {
        return (T) crudo(name);
    }

    private Object crudo(final String name) {
        Objects.requireNonNull(name, "name");
        final int punto = name.indexOf('.');
        if (punto < 0) {
            final int i = indice(name);
            if (i < 0) {
                throw new IllegalArgumentException("no hay un campo llamado " + name);
            }
            return valores[i];
        }
        final Object v = crudo(name.substring(0, punto));
        if (!(v instanceof RecordedObject)) {
            throw new IllegalArgumentException(
                    "el campo " + name.substring(0, punto) + " no es un objeto");
        }
        return ((RecordedObject) v).crudo(name.substring(punto + 1));
    }

    private int indice(final String name) {
        for (int i = 0; i < descriptores.size(); i++) {
            if (descriptores.get(i).getName().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    /** El descriptor de ese campo, para leerle las anotaciones. */
    private ValueDescriptor descriptor(final String name) {
        final int punto = name.indexOf('.');
        if (punto < 0) {
            final int i = indice(name);
            return i < 0 ? null : descriptores.get(i);
        }
        final Object v = crudo(name.substring(0, punto));
        return v instanceof RecordedObject
                ? ((RecordedObject) v).descriptor(name.substring(punto + 1)) : null;
    }

    /**
     * Los campos de este objeto.
     *
     * @return los descriptores
     */
    public List<ValueDescriptor> getFields() {
        return descriptores;
    }

    /**
     * El valor booleano de ese campo.
     *
     * @param name el nombre
     * @return el valor
     * @throws IllegalArgumentException si el campo no existe o no es booleano
     */
    public final boolean getBoolean(final String name) {
        final Object v = crudo(name);
        if (v instanceof Boolean) {
            return ((Boolean) v).booleanValue();
        }
        throw noEs(name, "boolean", v);
    }

    /**
     * El valor de ese campo como {@code byte}.
     *
     * @param name el nombre
     * @return el valor
     * @throws IllegalArgumentException si el campo no existe o no entra en un {@code byte}
     */
    public final byte getByte(final String name) {
        return (byte) entero(name, Byte.MIN_VALUE, Byte.MAX_VALUE, "byte");
    }

    /**
     * El valor de ese campo como {@code char}.
     *
     * @param name el nombre
     * @return el valor
     * @throws IllegalArgumentException si el campo no existe o no es un caracter
     */
    public final char getChar(final String name) {
        final Object v = crudo(name);
        if (v instanceof Character) {
            return ((Character) v).charValue();
        }
        throw noEs(name, "char", v);
    }

    /**
     * El valor de ese campo como {@code short}.
     *
     * @param name el nombre
     * @return el valor
     * @throws IllegalArgumentException si el campo no existe o no entra en un {@code short}
     */
    public final short getShort(final String name) {
        return (short) entero(name, Short.MIN_VALUE, Short.MAX_VALUE, "short");
    }

    /**
     * El valor de ese campo como {@code int}.
     *
     * @param name el nombre
     * @return el valor
     * @throws IllegalArgumentException si el campo no existe o no entra en un {@code int}
     */
    public final int getInt(final String name) {
        return (int) entero(name, Integer.MIN_VALUE, Integer.MAX_VALUE, "int");
    }

    /**
     * El valor de ese campo como {@code long}.
     *
     * @param name el nombre
     * @return el valor
     * @throws IllegalArgumentException si el campo no existe o no es entero
     */
    public final long getLong(final String name) {
        return entero(name, Long.MIN_VALUE, Long.MAX_VALUE, "long");
    }

    /**
     * Lee un entero de cualquier ancho y comprueba que entre en el pedido.
     *
     * <p>Ensanchar es el caso normal —un campo grabado como {@code short} se lee con
     * {@code getInt}— y por eso no se rechaza. Lo que si se rechaza es <strong>angostar</strong> un
     * valor que no entra: devolver el truncado seria un numero equivocado sin aviso, que es
     * exactamente lo que no se quiere del lado del que lee una grabacion.
     */
    private long entero(final String name, final long min, final long max, final String comoQue) {
        final Object v = crudo(name);
        final long l;
        if (v instanceof Byte) {
            l = ((Byte) v).byteValue();
        } else if (v instanceof Short) {
            l = ((Short) v).shortValue();
        } else if (v instanceof Integer) {
            l = ((Integer) v).intValue();
        } else if (v instanceof Long) {
            l = ((Long) v).longValue();
        } else if (v instanceof Character) {
            l = ((Character) v).charValue();
        } else {
            throw noEs(name, comoQue, v);
        }
        if (l < min || l > max) {
            throw new IllegalArgumentException(
                    "el valor del campo " + name + " no entra en un " + comoQue + ": " + l);
        }
        return l;
    }

    /**
     * El valor de ese campo como {@code float}.
     *
     * @param name el nombre
     * @return el valor
     * @throws IllegalArgumentException si el campo no existe o no es numerico
     */
    public final float getFloat(final String name) {
        final Object v = crudo(name);
        if (v instanceof Number) {
            return ((Number) v).floatValue();
        }
        throw noEs(name, "float", v);
    }

    /**
     * El valor de ese campo como {@code double}.
     *
     * @param name el nombre
     * @return el valor
     * @throws IllegalArgumentException si el campo no existe o no es numerico
     */
    public final double getDouble(final String name) {
        final Object v = crudo(name);
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        throw noEs(name, "double", v);
    }

    /**
     * El valor de ese campo como texto.
     *
     * @param name el nombre
     * @return el valor, o {@code null} si el campo esta vacio
     * @throws IllegalArgumentException si el campo no existe o no es texto
     */
    public final String getString(final String name) {
        final Object v = crudo(name);
        if (v == null || v instanceof String) {
            return (String) v;
        }
        throw noEs(name, "String", v);
    }

    /**
     * El valor de ese campo como duracion, interpretando su unidad.
     *
     * <p>La unidad sale de la anotacion {@link Timespan} del campo. Sin anotacion se asume
     * nanosegundos, que es lo que esa anotacion tiene por omision.
     *
     * @param name el nombre
     * @return la duracion
     * @throws IllegalArgumentException si el campo no existe o no es numerico
     */
    public final Duration getDuration(final String name) {
        final long v = entero(name, Long.MIN_VALUE, Long.MAX_VALUE, "Duration");
        final ValueDescriptor d = descriptor(name);
        final Timespan t = d == null ? null : d.getAnnotation(Timespan.class);
        final String unidad = t == null ? Timespan.NANOSECONDS : t.value();
        if (Timespan.SECONDS.equals(unidad)) {
            return Duration.ofSeconds(v);
        }
        if (Timespan.MILLISECONDS.equals(unidad)) {
            return Duration.ofMillis(v);
        }
        if (Timespan.MICROSECONDS.equals(unidad)) {
            return Duration.ofNanos(v * 1000L);
        }
        // TICKS incluido: sin la frecuencia del reloj de la maquina que grabo no hay como
        // convertirlos, y tratarlos como nanosegundos es lo que hace el JDK.
        return Duration.ofNanos(v);
    }

    /**
     * El valor de ese campo como momento, interpretando su unidad.
     *
     * <p>La unidad sale de la anotacion {@link Timestamp} del campo.
     *
     * @param name el nombre
     * @return el momento
     * @throws IllegalArgumentException si el campo no existe o no es numerico
     */
    public final Instant getInstant(final String name) {
        final long v = entero(name, Long.MIN_VALUE, Long.MAX_VALUE, "Instant");
        final ValueDescriptor d = descriptor(name);
        final Timestamp t = d == null ? null : d.getAnnotation(Timestamp.class);
        final String unidad = t == null ? Timestamp.MILLISECONDS_SINCE_EPOCH : t.value();
        if (Timestamp.TICKS.equals(unidad)) {
            return Instant.ofEpochSecond(0L, v);
        }
        return Instant.ofEpochMilli(v);
    }

    /**
     * El valor de ese campo como clase grabada.
     *
     * @param name el nombre
     * @return la clase, o {@code null} si el campo esta vacio
     * @throws IllegalArgumentException si el campo no existe o no es una clase
     */
    public final RecordedClass getClass(final String name) {
        final Object v = crudo(name);
        if (v == null || v instanceof RecordedClass) {
            return (RecordedClass) v;
        }
        throw noEs(name, "RecordedClass", v);
    }

    /**
     * El valor de ese campo como hilo grabado.
     *
     * @param name el nombre
     * @return el hilo, o {@code null} si el campo esta vacio
     * @throws IllegalArgumentException si el campo no existe o no es un hilo
     */
    public final RecordedThread getThread(final String name) {
        final Object v = crudo(name);
        if (v == null || v instanceof RecordedThread) {
            return (RecordedThread) v;
        }
        throw noEs(name, "RecordedThread", v);
    }

    private static IllegalArgumentException noEs(final String name, final String tipo,
            final Object v) {
        return new IllegalArgumentException("el campo " + name + " no se puede leer como " + tipo
                + "; es " + (v == null ? "null" : v.getClass().getName()));
    }

    /**
     * El objeto entero, un campo por linea.
     *
     * <p>Es {@code final} porque el formato tiene que ser el mismo para todos los objetos grabados:
     * quien lee un volcado de una grabacion no deberia tener que saber de que subclase era cada
     * cosa.
     *
     * @return el texto
     */
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append('{').append(System.lineSeparator());
        for (int i = 0; i < descriptores.size(); i++) {
            sb.append("  ").append(descriptores.get(i).getName()).append(" = ")
              .append(String.valueOf(valores[i])).append(System.lineSeparator());
        }
        sb.append('}');
        return sb.toString();
    }
}
