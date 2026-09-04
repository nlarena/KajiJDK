package javax.management;

/** El atributo pedido no existe en el MBean. */
public class AttributeNotFoundException extends OperationsException {

    private static final long serialVersionUID = 6511584241791106926L;

    public AttributeNotFoundException() {
        super();
    }

    public AttributeNotFoundException(String message) {
        super(message);
    }
}
