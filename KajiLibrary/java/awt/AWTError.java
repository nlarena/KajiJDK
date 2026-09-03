package java.awt;

/**
 * El AWT quedo en un estado del que no se vuelve --el toolkit no se pudo cargar, por ejemplo--.
 * Hereda de Error y no de Exception justamente porque no hay nada sensato que hacer al atraparlo.
 */
public class AWTError extends Error {

    private static final long serialVersionUID = -1819846354050686206L;

    public AWTError(String msg) {
        super(msg);
    }
}
