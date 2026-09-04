package java.lang.instrument;

/**
 * KajiLibrary's java.lang.instrument.UnmodifiableClassException -- esa clase no se puede redefinir.
 *
 * <p>Sale de {@code redefineClasses} y {@code retransformClasses}. Hay clases que la maquina virtual
 * no deja tocar --las primitivas, los arreglos, y en la practica buena parte de lo que ya se estaba
 * ejecutando cuando el agente arranco--.
 *
 * <p>Es comprobada porque cuales son depende de la implementacion y del momento: un agente serio
 * consulta {@code isModifiableClass} antes, y ataja esto igual por si algo cambio entre la consulta y
 * la redefinicion.
 */
public class UnmodifiableClassException extends Exception {

    private static final long serialVersionUID = 1716652643585309178L;

    /** Sin detalle. */
    public UnmodifiableClassException() {
        super();
    }

    /** Con un mensaje que diga cual. */
    public UnmodifiableClassException(String s) {
        super(s);
    }
}
