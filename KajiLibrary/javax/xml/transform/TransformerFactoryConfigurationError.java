package javax.xml.transform;

/**
 * KajiLibrary's javax.xml.transform.TransformerFactoryConfigurationError -- no hay fabrica.
 *
 * <p>Es un {@link Error} y no una excepcion, y la eleccion tiene fundamento aunque parezca dura:
 * lo lanza {@link TransformerFactory#newInstance()}, que **no declara nada chequeado**, porque en el
 * caso normal --hay una implementacion de XSLT en el classpath-- el fallo es imposible. Cuando
 * ocurre, ocurre por un despliegue mal armado: falta el jar, o la propiedad de sistema nombra una
 * clase que no existe. No es una condicion que el codigo de la aplicacion pueda manejar, es una
 * instalacion rota, y obligarlo a un `try`/`catch` en cada llamada seria puro ruido.
 *
 * <p>**Aca se lanza siempre**, y no por un fallo: esta biblioteca no trae ningun procesador de
 * XSLT. Ver el encabezado de {@link TransformerFactory} para el criterio -- el resumen es que un
 * `Transformer` que no transforma seria mucho peor que este error.
 *
 * <p>Como {@link TransformerException}, arrastra un campo de causa propio anterior a las causas
 * encadenadas de la plataforma, y con dos diferencias respecto de aquella que conviene tener
 * presentes porque no son las que uno supondria:
 *
 * <ul>
 *   <li>el campo es {@code Exception} y no {@code Throwable}, asi que un {@code Error} de fondo no
 *       se puede guardar aca;
 *   <li>{@link #getMessage} **cae en el mensaje de la causa** cuando el propio es nulo. Es la unica
 *       clase del paquete que lo hace, y es el motivo por el que el constructor
 *       {@code (Exception, String)} tiene los argumentos en ese orden raro: el que se agrego
 *       despues quedo al final.
 * </ul>
 */
public class TransformerFactoryConfigurationError extends Error {

    private static final long serialVersionUID = -6323715983680123667L;

    /** La causa, por el nombre viejo. Ver la nota del encabezado. */
    private Exception exception;

    /** Sin mensaje ni causa. */
    public TransformerFactoryConfigurationError() {
        super();
        this.exception = null;
    }

    /**
     * Con un mensaje.
     *
     * @param msg la descripcion del error
     */
    public TransformerFactoryConfigurationError(String msg) {
        super(msg);
        this.exception = null;
    }

    /**
     * Envolviendo una excepcion; el mensaje sale de ella.
     *
     * @param e la causa
     */
    public TransformerFactoryConfigurationError(Exception e) {
        super(e.toString());
        this.exception = e;
    }

    /**
     * Con causa y mensaje, en ese orden.
     *
     * @param e la causa
     * @param msg la descripcion del error
     */
    public TransformerFactoryConfigurationError(Exception e, String msg) {
        super(msg);
        this.exception = e;
    }

    /**
     * El mensaje propio; si no hay, el de la causa.
     *
     * <p>La cascada existe para que {@code new TransformerFactoryConfigurationError(e, null)} no
     * pierda lo unico que se sabia del problema.
     */
    public String getMessage() {
        String message = super.getMessage();
        if (message == null && exception != null) {
            return exception.getMessage();
        }
        return message;
    }

    /** La causa, por el nombre viejo. */
    public Exception getException() {
        return exception;
    }

    /** La causa, por el nombre de la plataforma. */
    public Throwable getCause() {
        return exception;
    }
}
