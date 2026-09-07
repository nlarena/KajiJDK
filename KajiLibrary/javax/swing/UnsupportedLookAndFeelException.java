package javax.swing;

/**
 * Ese aspecto grafico no sirve en esta plataforma.
 *
 * <h2>Por que es comprobada y no de ejecucion</h2>
 *
 * <p>Porque no es un error del programa: el aspecto nativo de Windows existe y esta bien pedirlo, y
 * en Linux no hay forma de darlo. Quien lo pide tiene que tener un plan para ese caso --caer en el
 * multiplataforma-- y una excepcion comprobada es lo que lo obliga a escribirlo.
 *
 * @since 1.2
 */
public class UnsupportedLookAndFeelException extends Exception {

    private static final long serialVersionUID = -6096987026804165577L;

    /**
     * Con ese motivo.
     *
     * @param s por que no sirve
     */
    public UnsupportedLookAndFeelException(String s) {
        super(s);
    }
}
