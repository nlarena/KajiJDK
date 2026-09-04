package java.lang.instrument;

/**
 * KajiLibrary's java.lang.instrument.UnmodifiableModuleException -- ese modulo no se puede
 * redefinir.
 *
 * <p>El equivalente de {@link UnmodifiableClassException} para modulos, con una diferencia que vale
 * mirar: esta <b>no es comprobada</b>.
 *
 * <p>La razon es que se sabe de antemano. Un modulo es modificable o no lo es, y no cambia mientras
 * el programa corre; {@code isModifiableModule} lo contesta sin ambiguedad. Redefinir uno que no lo
 * es es un error de programa, no una condicion que haya que manejar.
 */
public class UnmodifiableModuleException extends RuntimeException {

    private static final long serialVersionUID = 6912511912351080644L;

    /** Sin detalle. */
    public UnmodifiableModuleException() {
        super();
    }

    /** Con un mensaje que diga cual. */
    public UnmodifiableModuleException(String msg) {
        super(msg);
    }
}
