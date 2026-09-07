package jdk.jfr;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Una anotacion de metadatos, descrita como <strong>datos</strong> en vez de como anotacion Java.
 *
 * <h2>Por que hace falta describirla como datos</h2>
 *
 * <p>Por dos razones distintas y las dos importan.
 *
 * <p>La primera: un evento se puede fabricar en tiempo de ejecucion con {@link EventFactory}, sin
 * que exista una clase Java para el. Ahi no hay donde poner una anotacion, y sin embargo el evento
 * tiene que poder llevar su etiqueta y su descripcion. Esta clase es como se las pone.
 *
 * <p>La segunda: una grabacion se lee en <strong>otro</strong> proceso, que puede no tener en su
 * classpath las anotaciones que el que grabo uso. Guardando nombre y valores en vez de la anotacion
 * misma, el que lee puede mostrarlas igual.
 *
 * <h2>Que se puede guardar adentro</h2>
 *
 * <p>Solo tipos simples: los primitivos, {@code String}, {@code Class}, enumeraciones, y arreglos
 * de esos. No hay anotaciones anidadas como valor. Es la misma restriccion que el formato de la
 * grabacion impone, y por eso esta aca y no mas abajo.
 *
 * <h2>Las meta-anotaciones viajan tambien</h2>
 *
 * <p>{@link #getAnnotationElements} devuelve las anotaciones <strong>de la anotacion</strong>. Es
 * lo que permite que quien lee sepa que {@code @Timespan} es un {@link ContentType} y por lo tanto
 * que el numero que acompana es una duracion — sin tener la clase {@code Timespan} a mano.
 *
 * @since 9
 */
public final class AnnotationElement {

    private final Class<? extends Annotation> tipo;
    private final List<ValueDescriptor> descriptores;
    private final List<Object> valores;
    private final Map<String, Object> porNombre;

    /**
     * Una anotacion con varios valores, dados por nombre de miembro.
     *
     * @param annotationType el tipo de la anotacion
     * @param values los valores, por nombre de miembro
     * @throws NullPointerException si alguno de los dos es {@code null}
     * @throws IllegalArgumentException si un nombre no es un miembro de la anotacion, si falta un
     * miembro sin valor por omision, o si un valor no es de un tipo permitido
     */
    public AnnotationElement(final Class<? extends Annotation> annotationType,
            final Map<String, Object> values) {
        this.tipo = Objects.requireNonNull(annotationType, "annotationType");
        Objects.requireNonNull(values, "values");

        final List<ValueDescriptor> ds = new ArrayList<ValueDescriptor>();
        final List<Object> vs = new ArrayList<Object>();
        final Map<String, Object> mapa = new LinkedHashMap<String, Object>();

        // Se recorren los miembros de la anotacion, no las claves del mapa: asi el orden de los
        // valores es el de la declaracion y no el del mapa que paso el que llama, que puede ser
        // cualquiera.
        for (final Method m : annotationType.getDeclaredMethods()) {
            final String nombre = m.getName();
            Object v = values.get(nombre);
            if (v == null) {
                v = m.getDefaultValue();
                if (v == null) {
                    throw new IllegalArgumentException(
                            "falta el valor del miembro " + nombre + " de " + annotationType);
                }
            }
            revisar(v, nombre);
            ds.add(new ValueDescriptor(m.getReturnType(), nombre));
            vs.add(v);
            mapa.put(nombre, v);
        }
        for (final String clave : values.keySet()) {
            if (!mapa.containsKey(clave)) {
                throw new IllegalArgumentException(
                        clave + " no es un miembro de " + annotationType);
            }
        }

        this.descriptores = Collections.unmodifiableList(ds);
        this.valores = Collections.unmodifiableList(vs);
        this.porNombre = Collections.unmodifiableMap(mapa);
    }

    /**
     * Una anotacion de un solo miembro, el llamado {@code value}.
     *
     * @param annotationType el tipo de la anotacion
     * @param value el valor
     * @throws NullPointerException si alguno de los dos es {@code null}
     * @throws IllegalArgumentException si la anotacion no tiene un miembro {@code value} o el valor
     * no es de un tipo permitido
     */
    public AnnotationElement(final Class<? extends Annotation> annotationType, final Object value) {
        this(annotationType, Collections.singletonMap("value",
                Objects.requireNonNull(value, "value")));
    }

    /**
     * Una anotacion sin valores, o con todos sus miembros en el valor por omision.
     *
     * @param annotationType el tipo de la anotacion
     * @throws NullPointerException si es {@code null}
     * @throws IllegalArgumentException si algun miembro no tiene valor por omision
     */
    public AnnotationElement(final Class<? extends Annotation> annotationType) {
        this(annotationType, Collections.<String, Object>emptyMap());
    }

    /**
     * Los tipos que el formato de la grabacion sabe guardar.
     *
     * <p>Rechazar aca y no mas tarde importa: un valor de tipo no soportado descubierto al escribir
     * el archivo arruinaria una grabacion que ya empezo.
     */
    private static void revisar(final Object v, final String nombre) {
        Class<?> c = v.getClass();
        if (c.isArray()) {
            c = c.getComponentType();
        }
        if (c == Byte.class || c == Short.class || c == Integer.class || c == Long.class
                || c == Float.class || c == Double.class || c == Character.class
                || c == Boolean.class || c == String.class || c == Class.class
                || c.isPrimitive() || c.isEnum()) {
            return;
        }
        throw new IllegalArgumentException(
                "el valor del miembro " + nombre + " es de un tipo que no se puede guardar en una "
                + "grabacion: " + c.getName());
    }

    /**
     * Los valores, en el orden en que la anotacion declara sus miembros.
     *
     * @return los valores
     */
    public List<Object> getValues() {
        return valores;
    }

    /**
     * Los miembros de la anotacion, descritos.
     *
     * <p>Se corresponden posicion a posicion con {@link #getValues}.
     *
     * @return los descriptores
     */
    public List<ValueDescriptor> getValueDescriptors() {
        return descriptores;
    }

    /**
     * Las anotaciones que lleva puestas <strong>esta</strong> anotacion.
     *
     * <p>Se saltean las de {@code java.lang.annotation} —{@code @Retention}, {@code @Target} y
     * companeras— porque describen como funciona la anotacion en Java y no dicen nada sobre el
     * evento. Al que lee la grabacion no le sirven.
     *
     * @return las meta-anotaciones
     */
    public List<AnnotationElement> getAnnotationElements() {
        final List<AnnotationElement> out = new ArrayList<AnnotationElement>();
        for (final Annotation a : tipo.getAnnotations()) {
            final Class<? extends Annotation> t = a.annotationType();
            if (t.getName().startsWith("java.lang.annotation.")) {
                continue;
            }
            out.add(desde(a));
        }
        return Collections.unmodifiableList(out);
    }

    /** Arma un elemento a partir de una anotacion viva, leyendole los miembros por reflexion. */
    private static AnnotationElement desde(final Annotation a) {
        final Map<String, Object> vals = new LinkedHashMap<String, Object>();
        for (final Method m : a.annotationType().getDeclaredMethods()) {
            try {
                vals.put(m.getName(), m.invoke(a));
            } catch (final ReflectiveOperationException e) {
                // Un miembro que no se puede leer no deberia hacer desaparecer a los demas: se
                // omite y el elemento queda con lo que si se pudo leer.
                continue;
            }
        }
        return new AnnotationElement(a.annotationType(), vals);
    }

    /**
     * El nombre completo del tipo de la anotacion.
     *
     * @return el nombre
     */
    public String getTypeName() {
        return tipo.getName();
    }

    /**
     * El valor de ese miembro.
     *
     * @param name el nombre del miembro
     * @return el valor
     * @throws IllegalArgumentException si la anotacion no tiene ese miembro
     */
    public Object getValue(final String name) {
        Objects.requireNonNull(name, "name");
        if (!porNombre.containsKey(name)) {
            throw new IllegalArgumentException(
                    name + " no es un miembro de " + tipo.getName());
        }
        return porNombre.get(name);
    }

    /**
     * Si la anotacion tiene ese miembro.
     *
     * @param name el nombre del miembro
     * @return si lo tiene
     */
    public boolean hasValue(final String name) {
        return porNombre.containsKey(Objects.requireNonNull(name, "name"));
    }

    /**
     * La meta-anotacion de ese tipo que lleva esta anotacion, si la lleva.
     *
     * <p>La firma es la del JDK, con un parametro de tipo sin acotar: el resultado no esta ligado
     * al argumento y la conversion no se puede comprobar. Es una rareza de la API que se reproduce
     * tal cual.
     *
     * @param <A> el tipo esperado
     * @param annotationType el tipo de la meta-anotacion
     * @return la anotacion, o {@code null} si no esta
     */
    @SuppressWarnings("unchecked")
    public final <A> A getAnnotation(final Class<? extends Annotation> annotationType) {
        Objects.requireNonNull(annotationType, "annotationType");
        return (A) tipo.getAnnotation(annotationType);
    }

    /**
     * El identificador numerico del tipo de esta anotacion.
     *
     * <p>Vale dentro de esta ejecucion de la VM y no fuera; ver {@code Tipos}.
     *
     * @return el identificador
     */
    public long getTypeId() {
        return Tipos.id(tipo);
    }
}
