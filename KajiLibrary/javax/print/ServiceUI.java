package javax.print;

import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;
import javax.print.attribute.PrintRequestAttributeSet;

/**
 * KajiLibrary's javax.print.ServiceUI -- el dialogo de impresion.
 *
 * <p>Una clase con un solo metodo estatico. Muestra el cuadro donde el usuario elige impresora y
 * opciones, y devuelve la que eligio o null si cancelo.
 *
 * <h2>El conjunto de atributos es de entrada y de salida</h2>
 *
 * <p>Es lo unico que hay que saber de este metodo. El {@link PrintRequestAttributeSet} que se pasa se
 * usa para <b>rellenar</b> el dialogo, y despues se <b>modifica en el lugar</b> con lo que el usuario
 * eligio. Pasar un conjunto compartido, o reusar el mismo entre dos dialogos, produce sorpresas.
 *
 * <p>Devolver null y haber modificado el conjunto no es contradictorio: si el usuario cancela, la
 * documentacion no promete que el conjunto quede intacto.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Esta biblioteca no tiene interfaz grafica: no hay Swing ni ventanas, y por lo tanto siempre esta
 * en el caso que el JDK llama sin pantalla. Ahi el propio JDK lanza {@link HeadlessException}
 * <b>antes</b> de mirar los argumentos --se comprobo contra el JDK 25 con
 * {@code -Djava.awt.headless=true}--, asi que eso es exactamente lo que hacemos: la validacion de
 * {@code services} y {@code attributes} nunca llega a correr, igual que alla.
 */
public class ServiceUI {

    /** Publico porque el JDK lo dejo publico; la clase no tiene estado. */
    public ServiceUI() {
    }

    /**
     * Muestra el dialogo y devuelve la impresora elegida, o null si se cancelo.
     *
     * <p>Ver la nota de la clase: {@code attributes} se modifica en el lugar.
     *
     * @param gc en que pantalla, o null para la principal
     * @param x posicion de la esquina
     * @param y idem
     * @param services entre cuales elegir; no puede ser null ni vacio
     * @param defaultService cual viene seleccionada, o null
     * @param flavor el formato que se va a imprimir, o null
     * @param attributes entra con lo pedido y sale con lo elegido
     * @throws IllegalArgumentException si {@code services} es null o vacio, o si
     *     {@code defaultService} no esta entre ellos -- en el JDK con pantalla; aca gana el
     *     {@code HeadlessException}
     * @throws HeadlessException siempre: no hay pantalla. Ver la nota de la clase
     */
    public static PrintService printDialog(GraphicsConfiguration gc, int x, int y,
                                           PrintService[] services, PrintService defaultService,
                                           DocFlavor flavor, PrintRequestAttributeSet attributes)
        throws HeadlessException {
        throw new HeadlessException();
    }
}
