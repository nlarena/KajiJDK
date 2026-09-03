package java.lang.annotation;

/**
 * KajiLibrary's java.lang.annotation.AnnotationFormatError — el `.class` traia anotaciones
 * sintacticamente rotas.
 *
 * <p>Es un {@link Error} y no una excepcion a proposito: no describe un programa que pidio algo
 * imposible sino un archivo de clase corrupto o generado por una herramienta con un bug. Nadie
 * puede recuperarse razonablemente de eso, asi que no vale la pena obligar a atraparlo.
 *
 * <p>Conviene no confundirlo con {@link AnnotationTypeMismatchException}: aquel aparece cuando los
 * bytes se parsearon bien pero el valor no es del tipo que el elemento declara --el `.class` de la
 * anotacion y el del tipo anotado se compilaron por separado y quedaron desfasados--. Este aparece
 * antes, cuando los bytes ni siquiera forman una anotacion.
 *
 * <p>La clase esta completa: son tres constructores que solo delegan. Quien parsea anotaciones en
 * esta VM esta en Rust y hoy no la construye; eso no le quita valor al tipo, porque el codigo que
 * escribe o lee anotaciones a mano necesita poder nombrarlo y atraparlo.
 *
 * <p>No se declara `serialVersionUID`: no es API publica y esta biblioteca no tiene serializacion
 * de objetos (hay {@code java.io.Serializable} y {@code ObjectOutput}, pero ningun
 * {@code ObjectOutputStream} que los use), asi que el campo no protegeria nada.
 */
public class AnnotationFormatError extends Error {

    public AnnotationFormatError(String message) {
        super(message);
    }

    public AnnotationFormatError(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * El detalle queda en manos de {@link Throwable#toString()} de la causa, no de un texto propio.
     *
     * <p>Es la forma canonica de "envolver": el error de formato casi siempre nace de una
     * {@code IOException} o de un parser que se quedo sin bytes, y esa excepcion ya sabe explicarse
     * mejor que cualquier frase fija que pudieramos poner aca.
     */
    public AnnotationFormatError(Throwable cause) {
        super(cause);
    }
}
