package javax.management;

/** El servicio pedido no existe o no se pudo aplicar. */
public class ServiceNotFoundException extends OperationsException {

    private static final long serialVersionUID = -3990675661956646827L;

    public ServiceNotFoundException() {
        super();
    }

    public ServiceNotFoundException(String message) {
        super(message);
    }
}
