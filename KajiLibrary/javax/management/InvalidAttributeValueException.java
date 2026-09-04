package javax.management;

/** El valor no es del tipo que el atributo declara. */
public class InvalidAttributeValueException extends OperationsException {

    private static final long serialVersionUID = 2164571879317142449L;

    public InvalidAttributeValueException() {
        super();
    }

    public InvalidAttributeValueException(String message) {
        super(message);
    }
}
