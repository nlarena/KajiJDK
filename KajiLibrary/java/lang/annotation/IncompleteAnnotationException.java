package java.lang.annotation;

/**
 * KajiLibrary's java.lang.annotation.IncompleteAnnotationException — a la anotacion le falta un
 * elemento que el tipo declara.
 *
 * <p>Otro desfasaje entre dos compilaciones, hermano de {@link AnnotationTypeMismatchException}:
 * alguien agrego un elemento <strong>sin valor por defecto</strong> a un tipo de anotacion y
 * recompilo solo ese tipo. Los `.class` que ya usaban la anotacion no traen ese elemento, porque
 * cuando se compilaron no existia. Al pedirlo por reflexion no hay nada que devolver ni default al
 * que caer, y salta esto.
 *
 * <p>El detalle que explica todo el diseno de las anotaciones: un elemento con {@code default}
 * nunca provoca esta excepcion. Por eso agregar elementos con valor por defecto es compatible hacia
 * atras y agregarlos sin el, no.
 *
 * <p>La clase esta completa. Los dos campos son mutables (no `final`) igual que en el JDK; se dejo
 * asi por fidelidad, no porque nadie los reasigne.
 *
 * <p>No se declara `serialVersionUID` por la misma razon que en el resto del paquete: no es API
 * publica y esta biblioteca no tiene serializacion de objetos que lo consulte.
 */
public class IncompleteAnnotationException extends RuntimeException {

    private Class<? extends Annotation> annotationType;

    private String elementName;

    /**
     * Los dos argumentos se desreferencian antes de asignarse --{@code getName()} sobre uno,
     * {@code toString()} sobre el otro-- asi que un `null` sale como
     * {@code NullPointerException} desde el `super`, que es justo lo que el JDK documenta.
     *
     * <p>El {@code elementName.toString()} parece redundante sobre un {@link String} y no lo es:
     * es lo que fuerza el fallo temprano. Sin el, la concatenacion escribiria el literal "null" y
     * la excepcion se construiria feliz con un nombre de elemento invalido adentro.
     */
    public IncompleteAnnotationException(Class<? extends Annotation> annotationType,
            String elementName) {
        super(annotationType.getName() + " missing element " + elementName.toString());

        this.annotationType = annotationType;
        this.elementName = elementName;
    }

    public Class<? extends Annotation> annotationType() {
        return annotationType;
    }

    public String elementName() {
        return elementName;
    }
}
