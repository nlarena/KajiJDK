package java.awt.im.spi;

import java.awt.AWTEvent;
import java.awt.Rectangle;
import java.util.Locale;

/**
 * Un metodo de entrada: lo que convierte teclas en texto cuando una tecla no alcanza.
 *
 * <p>Escribir japones, chino o coreano no es una tecla por caracter: el usuario teclea una lectura
 * fonetica, el metodo de entrada le ofrece candidatos, y recien cuando elige uno se produce el
 * texto. Todo lo de esta interfaz sale de ahi.
 *
 * <h2>El texto en composicion</h2>
 *
 * <p>Entre la primera tecla y la eleccion hay un estado intermedio --el **texto en composicion**--
 * que el cliente muestra pero que todavia no es parte del documento. Es lo que se ve subrayado
 * mientras se elige. El metodo de entrada lo va reportando por
 * {@link InputMethodContext#dispatchInputMethodEvent}, y el texto pasa a ser definitivo cuando se
 * lo *compromete*.
 *
 * <p>Por eso {@link #endComposition} aparece en tantos lugares: cada vez que el foco se va, o que
 * el cliente necesita el texto de verdad, hay que decidir que hacer con lo que estaba a medio
 * componer.
 *
 * <h2>Activo y desactivado</h2>
 *
 * <p>Un metodo de entrada se {@link #activate activa} cuando el cliente que lo usa toma el foco y
 * se {@link #deactivate desactiva} cuando lo pierde. Entre medio puede seguir vivo: la
 * desactivacion no libera nada, para eso esta {@link #dispose}.
 *
 * @see InputMethodDescriptor
 */
public interface InputMethod {

    /**
     * Le da al metodo de entrada el contexto por el que se comunica con el cliente.
     *
     * <p>Se llama una sola vez, apenas creado y antes que cualquier otra cosa.
     */
    void setInputMethodContext(InputMethodContext context);

    /**
     * Cambia el idioma que se esta escribiendo.
     *
     * @return `true` si lo pudo hacer; `false` si no soporta ese idioma
     */
    boolean setLocale(Locale locale);

    /** El idioma que se esta escribiendo, o `null` si no hay ninguno todavia. */
    Locale getLocale();

    /**
     * Restringe los caracteres que se pueden producir a esos subconjuntos.
     *
     * <p>Con `null` o un arreglo vacio no hay restriccion.
     */
    void setCharacterSubsets(Character.Subset[] subsets);

    /**
     * Prende o apaga la composicion.
     *
     * <p>Apagada, las teclas pasan directo. Es como se alterna entre escribir japones y escribir
     * caracteres latinos sin cambiar de metodo de entrada.
     *
     * @throws UnsupportedOperationException si este metodo de entrada no se puede apagar
     */
    void setCompositionEnabled(boolean enable);

    /**
     * Si la composicion esta prendida.
     *
     * @throws UnsupportedOperationException si este metodo de entrada no sabe responderlo
     */
    boolean isCompositionEnabled();

    /**
     * Vuelve a poner en composicion el texto ya comprometido que rodea al cursor.
     *
     * <p>Sirve para corregir: el usuario eligio mal el candidato, y en vez de borrar y volver a
     * teclear pide reconvertir lo que ya escribio.
     *
     * @throws UnsupportedOperationException si este metodo de entrada no sabe reconvertir
     */
    void reconvert();

    /**
     * Le entrega un evento al metodo de entrada.
     *
     * <p>Solo llegan los eventos que el metodo pidio; el que consuma el evento se queda con el, y
     * el cliente no lo ve.
     */
    void dispatchEvent(AWTEvent event);

    /**
     * Avisa que la ventana del cliente se movio, cambio de tamanio o de visibilidad.
     *
     * <p>Sirve para reubicar la ventana de candidatos, que tiene que seguir al texto. Solo llega si
     * el metodo lo pidio con {@link InputMethodContext#enableClientWindowNotification}.
     *
     * @param bounds la nueva posicion en pantalla, o `null` si la ventana ya no se ve
     */
    void notifyClientWindowChange(Rectangle bounds);

    /** Lo activa: su cliente acaba de tomar el foco. */
    void activate();

    /**
     * Lo desactiva: su cliente perdio el foco.
     *
     * @param isTemporary si el foco se fue por poco tiempo --un menu, un dialogo-- en cuyo caso
     *     conviene conservar el estado de composicion en vez de tirarlo
     */
    void deactivate(boolean isTemporary);

    /** Esconde las ventanas que haya abierto: la de candidatos, la de estado. */
    void hideWindows();

    /**
     * Avisa que el cliente se va: hay que soltar el texto en composicion sin comprometerlo.
     */
    void removeNotify();

    /**
     * Termina la composicion en curso comprometiendo lo que haya.
     *
     * <p>El texto sale por el evento de siempre, asi que el cliente se entera por el mismo camino
     * que si el usuario lo hubiera elegido.
     */
    void endComposition();

    /**
     * Suelta los recursos del metodo de entrada.
     *
     * <p>Se llama con el metodo ya desactivado, y despues de esto no se lo usa mas.
     */
    void dispose();

    /**
     * Un objeto de control especifico de esta implementacion, o `null` si no hay.
     *
     * <p>Es la valvula de escape: lo que un metodo de entrada quiera exponer y esta interfaz no
     * cubra sale por aca, y el cliente que lo entienda lo castea.
     */
    Object getControlObject();
}
