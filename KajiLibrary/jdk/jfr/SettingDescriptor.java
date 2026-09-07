package jdk.jfr;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

/**
 * La descripcion de un <strong>ajuste</strong> de un evento: {@code threshold}, {@code stackTrace}
 * o uno propio.
 *
 * <h2>En que se diferencia de un {@link ValueDescriptor}</h2>
 *
 * <p>Un {@code ValueDescriptor} describe un <strong>dato</strong> del evento: algo que se graba
 * cada vez que el evento ocurre. Un {@code SettingDescriptor} describe una
 * <strong>perilla</strong>: algo que se configura una vez y decide si el evento ocurre.
 *
 * <p>La confusion es facil porque los dos tienen nombre, etiqueta, descripcion y tipo. La
 * diferencia esta en {@link #getDefaultValue}, que solo tiene sentido para una perilla — un dato no
 * tiene valor por omision, lo trae el evento.
 *
 * <h2>Por que no tiene constructor publico</h2>
 *
 * <p>Porque los ajustes no se inventan del lado del que consulta: salen de las anotaciones del
 * evento y de los metodos marcados con {@link SettingDefinition}. Se obtienen de
 * {@link EventType#getSettingDescriptors}.
 *
 * @since 9
 */
public final class SettingDescriptor {

    private final String nombre;
    private final String tipoNombre;
    private final long tipoId;
    private final String etiqueta;
    private final String descripcion;
    private final String valorPorOmision;
    private final List<AnnotationElement> anotaciones;

    /**
     * La etiqueta y la descripcion se guardan aparte y no salen de las anotaciones, que es lo que
     * hace el JDK: los ajustes de siempre tienen etiqueta y {@code getAnnotationElements} vacio.
     * Comprobado contra el JDK 25.
     */
    SettingDescriptor(final String nombre, final String tipoNombre, final String etiqueta,
            final String descripcion, final String valorPorOmision,
            final List<AnnotationElement> anotaciones) {
        this.nombre = nombre;
        this.tipoNombre = tipoNombre;
        this.tipoId = Tipos.idDeNombre(tipoNombre);
        this.etiqueta = etiqueta;
        this.descripcion = descripcion;
        this.valorPorOmision = valorPorOmision;
        this.anotaciones = Collections.unmodifiableList(anotaciones);
    }

    /**
     * El nombre del ajuste, como se escribe en una configuracion.
     *
     * @return el nombre
     */
    public String getName() {
        return nombre;
    }

    /**
     * El nombre legible del ajuste.
     *
     * @return la etiqueta, o {@code null} si no tiene
     */
    public String getLabel() {
        return etiqueta;
    }

    /**
     * La explicacion del ajuste.
     *
     * @return la descripcion, o {@code null} si no tiene
     */
    public String getDescription() {
        return descripcion;
    }

    /**
     * El nombre de la anotacion que le da significado al valor, si tiene una.
     *
     * @return el nombre del tipo de contenido, o {@code null}
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
     * El nombre del tipo del ajuste.
     *
     * @return el nombre del tipo
     */
    public String getTypeName() {
        return tipoNombre;
    }

    /**
     * El identificador numerico del tipo del ajuste.
     *
     * @return el identificador
     */
    public long getTypeId() {
        return tipoId;
    }

    /**
     * La anotacion de ese tipo que lleva el ajuste, si la lleva.
     *
     * @param <A> el tipo de la anotacion
     * @param annotationType el tipo de la anotacion
     * @return la anotacion, o {@code null}
     */
    public <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
        for (final AnnotationElement a : anotaciones) {
            if (a.getTypeName().equals(annotationType.getName())) {
                return a.<A>getAnnotation(annotationType);
            }
        }
        return null;
    }

    /**
     * Las anotaciones del ajuste.
     *
     * @return las anotaciones
     */
    public List<AnnotationElement> getAnnotationElements() {
        return anotaciones;
    }

    /**
     * El valor con el que arranca el ajuste si nadie lo configura.
     *
     * @return el valor por omision
     */
    public String getDefaultValue() {
        return valorPorOmision;
    }
}
