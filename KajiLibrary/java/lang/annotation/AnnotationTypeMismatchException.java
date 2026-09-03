package java.lang.annotation;

import java.lang.reflect.Method;

/**
 * KajiLibrary's java.lang.annotation.AnnotationTypeMismatchException — el elemento de una anotacion
 * guarda un valor de un tipo distinto al que declara.
 *
 * <p>Es el sintoma de una recompilacion a medias. Alguien escribio {@code int value();}, un tercero
 * compilo {@code @Config(3)}, despues el autor cambio el elemento a {@code String value();} y
 * recompilo <strong>solo su</strong> anotacion. El `.class` del usuario sigue con un entero
 * adentro; el de la anotacion ya promete un texto. Nadie miente en el momento en que se compilo:
 * quedaron desfasados. Recien cuando la reflexion los junta se descubre el choque, y por eso vive
 * en tiempo de ejecucion y no la puede detectar el compilador.
 *
 * <p>Se distingue de {@link AnnotationFormatError} en que aca los bytes estan bien formados; lo que
 * falla es la concordancia entre dos archivos. Y de {@link IncompleteAnnotationException} en que
 * alli el elemento directamente <strong>no esta</strong>, mientras que aca esta con el tipo
 * equivocado.
 *
 * <p>La clase esta completa. Quien la construye normalmente es el proxy de anotaciones al leer un
 * elemento, cosa que esta VM todavia no hace; el tipo igual hace falta porque cualquier codigo que
 * llame a {@code getAnnotation(...).valor()} tiene que poder atraparla.
 *
 * <p>No se declara `serialVersionUID` por la misma razon que en el resto del paquete: no es API
 * publica y esta biblioteca no tiene serializacion de objetos que lo consulte.
 */
public class AnnotationTypeMismatchException extends RuntimeException {

    /**
     * `transient` porque un {@link Method} no viaja: al deserializar la excepcion el campo vuelve
     * en `null`, y por eso {@link #element()} documenta que puede no estar disponible. El
     * {@link #foundType} si viaja, y es lo unico que sobrevive de un lado al otro.
     */
    private final transient Method element;

    private final String foundType;

    /**
     * El mensaje se arma aca, en el `super`, y no en un `getMessage()` sobrescrito.
     *
     * <p>Tiene que ser antes de asignar los campos --es la regla del lenguaje-- y por eso el texto
     * se construye a partir de los parametros. Los dos aceptan `null` a proposito: quien detecta el
     * choque puede no tener a mano el {@link Method}, y la concatenacion los vuelve el literal
     * "null" sin explotar. Cambiar eso por un chequeo que tire {@code NullPointerException}
     * convertiria un diagnostico pobre en un fallo, que es peor.
     */
    public AnnotationTypeMismatchException(Method element, String foundType) {
        super("Incorrectly typed data found for annotation element " + element
                + " (Found data of type " + foundType + ")");
        this.element = element;
        this.foundType = foundType;
    }

    public Method element() {
        return this.element;
    }

    public String foundType() {
        return this.foundType;
    }
}
