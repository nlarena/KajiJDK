package javax.xml.stream;

/**
 * KajiLibrary's javax.xml.stream.FactoryConfigurationError -- cuando no hay implementacion de StAX
 * que darle al llamador.
 *
 * <p>Es un {@link Error} y no una excepcion, y la eleccion tiene sentido: que no exista un parser
 * instalado no es un problema del documento ni de los argumentos, es un despliegue incompleto. No
 * hay nada que el llamador pueda hacer en tiempo de ejecucion para arreglarlo, asi que obligarlo a
 * escribir un {@code catch} solo agregaria ruido. Es la misma decision que
 * {@link javax.xml.transform.TransformerFactoryConfigurationError} para XSLT.
 *
 * <p>En esta biblioteca es el camino normal y no el excepcional: no hay parser de StAX --ver el
 * encabezado de {@link XMLInputFactory}-- asi que las fabricas terminan aca.
 *
 * <h2>Los tres metodos sobrescritos</h2>
 *
 * <p>{@link #getException} y {@link #getCause} devuelven lo mismo. El primero es de 2004 y el
 * segundo apareció con las causas encadenadas de {@code Throwable}; se mantienen los dos porque hay
 * codigo que llama a cada uno.
 *
 * <p>{@link #getMessage} tiene una cascada que vale explicar: si hay mensaje propio lo devuelve; si
 * no, el de la excepcion envuelta; y si la envuelta tampoco tiene, el nombre de su clase. Sin esa
 * ultima rama, envolver una excepcion sin mensaje --que es lo normal en una
 * {@code ClassNotFoundException} de algunas VMs-- daria un error con mensaje null, que es la traza
 * que no dice nada.
 */
public class FactoryConfigurationError extends Error {

    /**
     * La excepcion que causo esto, si la hubo.
     *
     * <p>Sin modificador, como en el original: no es parte de la API publica, pero {@code getCause}
     * y {@code getMessage} la leen.
     */
    Exception nested;

    /** Sin mensaje ni causa. */
    public FactoryConfigurationError() {
        super();
    }

    /**
     * Envolviendo la excepcion que impidio construir la fabrica.
     *
     * @param e la excepcion de mas adentro
     */
    public FactoryConfigurationError(Exception e) {
        nested = e;
    }

    /**
     * Con causa y mensaje, en ese orden.
     *
     * <p>Que existan las dos variantes de orden --esta y
     * {@link #FactoryConfigurationError(String, Exception)}-- es historia, no diseño: quedaron las
     * dos por compatibilidad y hacen exactamente lo mismo.
     *
     * @param e la excepcion de mas adentro
     * @param msg el mensaje
     */
    public FactoryConfigurationError(Exception e, String msg) {
        super(msg);
        nested = e;
    }

    /**
     * Con mensaje y causa, en ese orden.
     *
     * @param msg el mensaje
     * @param e la excepcion de mas adentro
     */
    public FactoryConfigurationError(String msg, Exception e) {
        super(msg);
        nested = e;
    }

    /**
     * Con mensaje solo.
     *
     * @param msg el mensaje
     */
    public FactoryConfigurationError(String msg) {
        super(msg);
    }

    /**
     * La excepcion envuelta, o null.
     *
     * @return la de mas adentro
     */
    public Exception getException() {
        return nested;
    }

    /**
     * Lo mismo que {@link #getException}, con el nombre que usa {@code Throwable}.
     *
     * @return la de mas adentro
     */
    public Throwable getCause() {
        return nested;
    }

    /**
     * El mensaje propio; si no hay, el de la envuelta; si tampoco, el nombre de su clase.
     *
     * @return el mensaje, que puede ser null solo si no hay ni mensaje ni causa
     */
    public String getMessage() {
        String msg = super.getMessage();
        if (msg != null) {
            return msg;
        }
        if (nested != null) {
            msg = nested.getMessage();
            if (msg == null) {
                msg = nested.getClass().toString();
            }
        }
        return msg;
    }
}
