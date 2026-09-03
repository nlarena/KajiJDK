package java.awt;

/**
 * Se le pidio a un componente algo que en su estado actual no puede contestar --la posicion en
 * pantalla de algo que todavia no esta en pantalla, por ejemplo--.
 *
 * <p>Hereda de {@code IllegalStateException} y no agrega nada: existe solo para que quien atrapa
 * pueda distinguir el caso del AWT del resto.
 */
public class IllegalComponentStateException extends IllegalStateException {

    private static final long serialVersionUID = -1889339587208144238L;

    public IllegalComponentStateException() {
        super();
    }

    public IllegalComponentStateException(String s) {
        super(s);
    }
}
