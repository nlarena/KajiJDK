package java.awt;

/**
 * El archivo que se paso a {@code Font.createFont} no es una fuente de un formato que se entienda.
 *
 * <p>Se escribe aunque {@code Font} todavia no exista: es una excepcion verificada y su firma no
 * menciona ningun tipo del sistema de ventanas, asi que no depende de nada.
 */
public class FontFormatException extends Exception {

    private static final long serialVersionUID = -4481290147811361272L;

    public FontFormatException(String reason) {
        super(reason);
    }
}
