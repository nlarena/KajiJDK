package javax.swing.plaf.basic;

import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JList;

/**
 * Lo que {@link BasicComboBoxUI} le pide a la lista que se despliega.
 *
 * <h2>Por que es una interfaz y no una clase</h2>
 *
 * <p>La lista desplegable de un combo es la parte que mas cambia de un aspecto a otro: puede ser una
 * ventana flotante, un panel dentro de la misma ventana, o algo que ni siquiera sea una lista. Pero
 * lo que el UI del combo necesita de ella es siempre lo mismo -- mostrala, escondela, decime si esta
 * visible, y prestame tus escuchas para reenviarles los eventos --, y eso es lo que dice esta
 * interfaz.
 *
 * <h2>Los tres escuchas prestados</h2>
 *
 * <p>{@link #getMouseListener}, {@link #getMouseMotionListener} y {@link #getKeyListener} no son
 * para que el combo los agregue a la lista: son para que el combo los agregue <em>a si mismo</em>.
 * Apretar el boton del combo, arrastrar hacia abajo y soltar sobre un item es un solo gesto que
 * empieza en el combo y termina en la lista, y esa es la unica manera de que los dos vean el mismo
 * arrastre.
 *
 * <h2>{@link #uninstallingUI}</h2>
 *
 * <p>El aviso de que el combo se esta quedando sin aspecto. Es donde la ventana desplegable suelta
 * lo que engancho en el modelo del combo; sin ese aviso, cambiar de aspecto dejaria la lista vieja
 * escuchando para siempre.
 */
public interface ComboPopup {

    /** Muestra la lista. */
    void show();

    /** La esconde. */
    void hide();

    boolean isVisible();

    /** La lista que se ve; el UI la necesita para saber que item quedo debajo del mouse. */
    JList<Object> getList();

    /** Ver la nota de la interfaz: van en el combo, no en la lista. */
    MouseListener getMouseListener();

    MouseMotionListener getMouseMotionListener();

    KeyListener getKeyListener();

    /** Ver la nota de la interfaz. */
    void uninstallingUI();
}
