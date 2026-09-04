package java.awt.dnd;

/**
 * Se hizo algo de arrastrar y soltar en un momento en que no correspondía.
 *
 * <p>Hereda de {@code IllegalStateException} y eso dice todo: no es un problema de los argumentos
 * sino del **estado**. Pedir los datos antes de aceptar el soltado, o aceptar dos veces, o soltar
 * cuando no hay arrastre en curso.
 *
 * <p>Que no sea comprobada es deliberado: son errores de secuencia del programa, no situaciones que
 * haya que atajar.
 */
public class InvalidDnDOperationException extends IllegalStateException {

    private static final long serialVersionUID = -6062568741193956678L;

    /** Con el mensaje por omisión. */
    public InvalidDnDOperationException() {
        super("The operation requested cannot be performed by the DnD system since it is not in "
                + "the appropriate state");
    }

    /** Con la explicación dada. */
    public InvalidDnDOperationException(String msg) {
        super(msg);
    }
}
