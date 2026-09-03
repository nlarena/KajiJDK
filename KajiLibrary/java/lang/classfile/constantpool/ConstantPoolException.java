package java.lang.classfile.constantpool;

// Lo que se tira cuando una entrada del pool no cumple lo que se le pidió: un índice fuera de rango,
// una etiqueta que no corresponde al tipo pedido, o una referencia interna rota. Hereda de
// `IllegalArgumentException`, igual que en el JDK.
public class ConstantPoolException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    /** Sin mensaje ni causa. */
    public ConstantPoolException() {
        super();
    }

    /** Con mensaje. */
    public ConstantPoolException(String message) {
        super(message);
    }

    /** Con mensaje y causa. */
    public ConstantPoolException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Con causa. */
    public ConstantPoolException(Throwable cause) {
        super(cause);
    }
}
