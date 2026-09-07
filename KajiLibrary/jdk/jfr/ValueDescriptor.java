package jdk.jfr;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * La descripcion de un campo de un evento: su nombre, su tipo y sus anotaciones.
 *
 * <h2>Que agrega sobre un {@code Field} de reflexion</h2>
 *
 * <p>Que no necesita que el campo exista. Un evento fabricado con {@link EventFactory} no tiene
 * clase Java, y sus campos son exactamente estos descriptores y nada mas.
 *
 * <p>Y que del otro lado, leyendo una grabacion, es la unica descripcion que hay: el proceso que
 * lee no tiene las clases del que grabo.
 *
 * <h2>Los accesores que parecen redundantes</h2>
 *
 * <p>{@link #getLabel}, {@link #getDescription} y {@link #getContentType} salen de las anotaciones
 * y podrian pedirse con {@link #getAnnotation}. Estan aparte porque son las tres que una
 * herramienta consulta para <strong>cada</strong> campo al mostrar una tabla, y hacerlo por el
 * camino generico seria una busqueda por tipo en cada celda.
 *
 * <p>{@link #getContentType} tiene ademas una vuelta de tuerca: no devuelve una anotacion sino el
 * nombre de la anotacion que a su vez esta marcada con {@link ContentType}. Es decir, mira las
 * meta-anotaciones. Por eso no se puede resolver con un {@code getAnnotation} directo.
 *
 * <h2>Los arreglos</h2>
 *
 * <p>{@link #getTypeName} de un {@code String[]} devuelve {@code "java.lang.String"} y
 * {@link #isArray} devuelve {@code true}. La condicion de arreglo va aparte del nombre y no pegada
 * a el, que es lo que permite que el que lee use el mismo tipo para el campo suelto y para el
 * arreglo.
 *
 * @since 9
 */
public final class ValueDescriptor {

    private final String tipoNombre;
    private final long tipoId;
    private final boolean arreglo;
    private final String nombre;
    private final List<AnnotationElement> anotaciones;

    /**
     * Un campo cuyo tipo <strong>no tiene clase Java</strong>.
     *
     * <p>No es API. Hace falta para el campo {@code stackTrace} que todo evento lleva, que es de
     * tipo {@code jdk.types.StackTrace} — un tipo del formato de la grabacion y no una clase.
     */
    ValueDescriptor(final String typeName, final String name,
            final List<AnnotationElement> annotations) {
        this.tipoNombre = typeName;
        this.tipoId = Tipos.idDeNombre(typeName);
        this.arreglo = false;
        this.nombre = name;
        this.anotaciones = Collections.unmodifiableList(
                new ArrayList<AnnotationElement>(annotations));
    }

    /**
     * Un campo con ese tipo y ese nombre, sin anotaciones.
     *
     * @param type el tipo
     * @param name el nombre
     * @throws NullPointerException si alguno es {@code null}
     * @throws IllegalArgumentException si el nombre no es un identificador Java valido
     */
    public ValueDescriptor(final Class<?> type, final String name) {
        this(type, name, Collections.<AnnotationElement>emptyList());
    }

    /**
     * Un campo con ese tipo, ese nombre y esas anotaciones.
     *
     * @param type el tipo
     * @param name el nombre
     * @param annotations las anotaciones
     * @throws NullPointerException si alguno es {@code null}
     * @throws IllegalArgumentException si el nombre no es un identificador Java valido
     */
    public ValueDescriptor(final Class<?> type, final String name,
            final List<AnnotationElement> annotations) {
        Objects.requireNonNull(type, "type");
        this.nombre = Objects.requireNonNull(name, "name");
        Objects.requireNonNull(annotations, "annotations");
        if (!esIdentificador(name)) {
            throw new IllegalArgumentException(
                    "el nombre de un campo tiene que ser un identificador Java valido: " + name);
        }
        this.tipoNombre = Tipos.nombre(type);
        this.tipoId = Tipos.id(type);
        this.arreglo = type.isArray();
        this.anotaciones = Collections.unmodifiableList(
                new ArrayList<AnnotationElement>(annotations));
    }

    private static boolean esIdentificador(final String s) {
        if (s.length() == 0 || !Character.isJavaIdentifierStart(s.charAt(0))) {
            return false;
        }
        for (int i = 1; i < s.length(); i++) {
            if (!Character.isJavaIdentifierPart(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * El nombre legible del campo, de su {@link Label}.
     *
     * @return la etiqueta, o {@code null} si no tiene
     */
    public String getLabel() {
        return texto(Label.class);
    }

    /**
     * El nombre del campo.
     *
     * @return el nombre
     */
    public String getName() {
        return nombre;
    }

    /**
     * La explicacion del campo, de su {@link Description}.
     *
     * @return la descripcion, o {@code null} si no tiene
     */
    public String getDescription() {
        return texto(Description.class);
    }

    /**
     * El nombre de la anotacion que le da significado al valor, si tiene una.
     *
     * <p>Devuelve, por ejemplo, {@code "jdk.jfr.Timespan"}: no la anotacion sino su nombre, y no
     * cualquiera sino la que a su vez esta marcada con {@link ContentType}.
     *
     * @return el nombre del tipo de contenido, o {@code null} si el campo no tiene ninguno
     */
    public String getContentType() {
        for (final AnnotationElement a : anotaciones) {
            for (final AnnotationElement meta : a.getAnnotationElements()) {
                if (ContentType.class.getName().equals(meta.getTypeName())) {
                    return a.getTypeName();
                }
            }
        }
        return null;
    }

    /**
     * El nombre del tipo del campo; para un arreglo, el de su componente.
     *
     * @return el nombre del tipo
     */
    public String getTypeName() {
        return tipoNombre;
    }

    /**
     * El identificador numerico del tipo del campo.
     *
     * @return el identificador
     */
    public long getTypeId() {
        return tipoId;
    }

    /**
     * Si el campo es un arreglo.
     *
     * @return si lo es
     */
    public boolean isArray() {
        return arreglo;
    }

    /**
     * La anotacion de ese tipo que lleva el campo, si la lleva.
     *
     * @param <A> el tipo de la anotacion
     * @param annotationType el tipo de la anotacion
     * @return la anotacion, o {@code null}
     */
    public <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
        Objects.requireNonNull(annotationType, "annotationType");
        for (final AnnotationElement a : anotaciones) {
            if (a.getTypeName().equals(annotationType.getName())) {
                return a.<A>getAnnotation(annotationType);
            }
        }
        return null;
    }

    /**
     * Las anotaciones del campo.
     *
     * @return las anotaciones
     */
    public List<AnnotationElement> getAnnotationElements() {
        return anotaciones;
    }

    /**
     * Los campos que tiene el tipo de este campo, si es compuesto.
     *
     * <p>Vacio para los tipos simples, que son la enorme mayoria de los campos de un evento. El
     * formato admite tipos compuestos —una direccion de red con su host y su puerto— y esto es como
     * se recorren.
     *
     * @return los campos, o una lista vacia
     */
    public List<ValueDescriptor> getFields() {
        return Collections.emptyList();
    }

    /** El valor de una anotacion de un solo miembro {@code value} de tipo texto. */
    private String texto(final Class<? extends Annotation> t) {
        for (final AnnotationElement a : anotaciones) {
            if (a.getTypeName().equals(t.getName()) && a.hasValue("value")) {
                final Object v = a.getValue("value");
                return v == null ? null : v.toString();
            }
        }
        return null;
    }
}
