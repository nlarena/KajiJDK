package javax.management;

/**
 * "El pedido no se pudo cumplir, y no es culpa del MBean."
 *
 * <p>Junta a las que informan un estado del **agente**: el nombre no existe, el atributo no esta, el
 * oyente no estaba registrado. Todas son condiciones normales de un sistema de gestion, no fallas.
 */
public class OperationsException extends JMException {

    private static final long serialVersionUID = -4967597595580536216L;

    public OperationsException() {
        super();
    }

    public OperationsException(String message) {
        super(message);
    }
}
