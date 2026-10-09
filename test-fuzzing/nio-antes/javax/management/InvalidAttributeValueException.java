package javax.management;

/** The value is not of the type the attribute declares. */
public class InvalidAttributeValueException extends OperationsException {

    private static final long serialVersionUID = 2164571879317142449L;

    public InvalidAttributeValueException() {
        super();
    }

    public InvalidAttributeValueException(String message) {
        super(message);
    }
}
