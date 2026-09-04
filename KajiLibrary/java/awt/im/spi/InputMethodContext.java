package java.awt.im.spi;

import java.awt.Window;
import java.awt.font.TextHitInfo;
import java.awt.im.InputMethodRequests;
import java.text.AttributedCharacterIterator;
import javax.swing.JFrame;

/**
 * Por donde un {@link InputMethod} le habla al cliente y al marco de trabajo.
 *
 * <p>Hereda {@link InputMethodRequests}, que es la mitad de preguntar --donde esta el cursor, que
 * texto hay alrededor-- y agrega la mitad de contar: el evento con el texto en composicion, y las
 * ventanas auxiliares.
 *
 * <p>Lo aporta el marco de trabajo, no el metodo de entrada. Este lo recibe una sola vez, en
 * {@link InputMethod#setInputMethodContext}.
 *
 * <h2>Por que hay dos formas de crear ventana</h2>
 *
 * <p>{@link #createInputMethodWindow} da una ventana de AWT y
 * {@link #createInputMethodJFrame} una de Swing. No es duplicacion por gusto: una ventana de metodo
 * de entrada tiene que aparecer **sin robar el foco** --si lo robara, el cliente dejaria de recibir
 * teclas, que es lo unico que el metodo de entrada esta tratando de conseguir-- y eso se arregla
 * distinto en cada juego de componentes. La de Swing existe porque una ventana de AWT adentro de
 * una aplicacion Swing se mezcla mal.
 */
public interface InputMethodContext extends InputMethodRequests {

    /**
     * Manda un evento de metodo de entrada al cliente.
     *
     * <p>Es como viaja el texto: los primeros `committedCharacterCount` caracteres del iterador son
     * definitivos y el resto es lo que sigue en composicion.
     *
     * @param id el tipo de evento, de los de `InputMethodEvent`
     * @param text el texto comprometido seguido del texto en composicion, o `null` si no hay
     * @param committedCharacterCount cuantos caracteres del principio son definitivos
     * @param caret donde va el cursor dentro del texto en composicion, o `null`
     * @param visiblePosition que parte del texto en composicion conviene mantener a la vista, o
     *     `null`
     */
    void dispatchInputMethodEvent(int id, AttributedCharacterIterator text,
            int committedCharacterCount, TextHitInfo caret, TextHitInfo visiblePosition);

    /**
     * Una ventana de AWT para el metodo de entrada.
     *
     * <p>No toma el foco al mostrarse, y no lo saca del cliente. Ver la nota de la clase.
     *
     * @param title el titulo; puede no verse, segun como se decore
     * @param attachToInputContext si el marco de trabajo tiene que enrutarle los eventos de
     *     entrada al mismo contexto que el cliente
     */
    Window createInputMethodWindow(String title, boolean attachToInputContext);

    /**
     * Una ventana de Swing para el metodo de entrada.
     *
     * <p>Lo mismo que {@link #createInputMethodWindow}, con un {@link JFrame}.
     *
     * @param title el titulo; puede no verse, segun como se decore
     * @param attachToInputContext si el marco de trabajo tiene que enrutarle los eventos de
     *     entrada al mismo contexto que el cliente
     */
    JFrame createInputMethodJFrame(String title, boolean attachToInputContext);

    /**
     * Pide --o deja de pedir-- que le avisen al metodo de entrada cuando la ventana del cliente se
     * mueva o cambie de tamanio.
     *
     * <p>Esta apagado por omision, y con razon: seguir la ventana cuesta, y solo lo necesita el
     * metodo que abre ventanas propias que tienen que quedar pegadas al texto.
     *
     * @param inputMethod el metodo de entrada que recibe los avisos
     * @param enable si los quiere
     */
    void enableClientWindowNotification(InputMethod inputMethod, boolean enable);
}
