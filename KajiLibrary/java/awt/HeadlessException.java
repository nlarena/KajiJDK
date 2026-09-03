package java.awt;

/**
 * Se pidio algo que necesita teclado, mouse o pantalla en un entorno que no los tiene.
 *
 * <p>Es la excepcion que le da sentido a todo lo demas de este paquete en KajiLibrary: aca no hay
 * sistema de ventanas, asi que cualquier clase de {@code java.awt} que dependa de uno tendria que
 * tirar esto siempre. Por eso lo que se escribe de {@code java.awt} son las clases de datos --
 * geometria, colores, constantes de disposicion-- que no la necesitan nunca.
 *
 * <p>El {@code getMessage()} del JDK le pega al mensaje propio un sufijo que describe por que el
 * entorno es headless, y ese sufijo lo arma {@code GraphicsEnvironment}, que no existe aca. Sin
 * el, {@code getMessage()} devuelve el mensaje tal cual --que es tambien lo que devuelve el JDK
 * real cuando corre con pantalla--. Inventar un sufijo seria peor: el texto no esta especificado y
 * describiria una razon que nadie averiguo.
 */
public class HeadlessException extends UnsupportedOperationException {

    private static final long serialVersionUID = 167183644944358563L;

    public HeadlessException() {
    }

    public HeadlessException(String msg) {
        super(msg);
    }

    public String getMessage() {
        return super.getMessage();
    }
}
