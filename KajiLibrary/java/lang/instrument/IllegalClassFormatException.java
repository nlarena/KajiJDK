package java.lang.instrument;

/**
 * KajiLibrary's java.lang.instrument.IllegalClassFormatException -- el transformador devolvio bytes
 * que no son una clase.
 *
 * <p>La lanza un {@link ClassFileTransformer} para decir que los bytes que <b>recibio</b> no le
 * sirven. Es la unica forma que tiene de negarse sin romper la carga: la maquina virtual la ataja,
 * ignora ese transformador y sigue con los demas.
 *
 * <p>La otra forma de negarse --devolver null-- significa "no me interesa, no lo toco". La diferencia
 * importa: null es silencioso y esta es el canal para avisar que algo estaba mal.
 */
public class IllegalClassFormatException extends Exception {

    private static final long serialVersionUID = -3841736710924794009L;

    /** Sin detalle. */
    public IllegalClassFormatException() {
        super();
    }

    /** Con un mensaje que diga que estaba mal. */
    public IllegalClassFormatException(String s) {
        super(s);
    }
}
