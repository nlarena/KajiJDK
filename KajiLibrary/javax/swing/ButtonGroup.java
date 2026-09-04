package javax.swing;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Un grupo de botones del que a lo sumo uno esta seleccionado.
 *
 * <p>Es la exclusion mutua de los botones de radio, y vive fuera de ellos: el grupo no pinta nada
 * ni tiene padre, solo recuerda quien esta seleccionado y deselecciona al anterior cuando otro se
 * selecciona. Un boton pertenece al grupo por su <em>modelo</em>, no por si mismo, que es por lo que
 * {@link #setSelected} y {@link #isSelected} reciben un {@link ButtonModel}.
 *
 * <p>La seleccion, una vez hecha, no se puede quitar clickeando: un boton de radio seleccionado
 * sigue seleccionado aunque se lo vuelva a apretar. La unica manera de que ninguno lo este es
 * {@link #clearSelection}.
 */
public class ButtonGroup implements Serializable {

    /** Los botones, en el orden en que se agregaron. */
    protected Vector<AbstractButton> buttons = new Vector<AbstractButton>();

    /** El modelo seleccionado, o {@code null}. */
    ButtonModel selection = null;

    public ButtonGroup() {
    }

    /**
     * Agrega un boton.
     *
     * <p>Si ya esta seleccionado y el grupo no tiene seleccion, pasa a ser la seleccion; si el
     * grupo ya tenia una, el que llega se deselecciona. Es la regla del grupo aplicada al momento
     * de entrar.
     */
    public void add(AbstractButton b) {
        if (b == null) {
            return;
        }
        buttons.addElement(b);
        if (b.isSelected()) {
            if (selection == null) {
                selection = b.getModel();
            } else {
                b.setSelected(false);
            }
        }
        b.getModel().setGroup(this);
    }

    /** Quita un boton; si era la seleccion, el grupo queda sin ella. */
    public void remove(AbstractButton b) {
        if (b == null) {
            return;
        }
        buttons.removeElement(b);
        if (b.getModel() == selection) {
            selection = null;
        }
        b.getModel().setGroup(null);
    }

    /** Deja al grupo sin seleccion, deseleccionando al que la tenia. */
    public void clearSelection() {
        if (selection != null) {
            ButtonModel viejo = selection;
            selection = null;
            viejo.setSelected(false);
        }
    }

    public Enumeration<AbstractButton> getElements() {
        return buttons.elements();
    }

    /** El modelo seleccionado, o {@code null}. */
    public ButtonModel getSelection() {
        return selection;
    }

    /**
     * Selecciona ese modelo, deseleccionando al que estaba.
     *
     * <p>Solo selecciona: pedir {@code false} no hace nada, que es la regla de "no se puede quitar
     * clickeando" dicha en la API. Lo llama el propio modelo cuando lo seleccionan.
     */
    public void setSelected(ButtonModel m, boolean b) {
        if (b && m != null && m != selection) {
            ButtonModel viejo = selection;
            selection = m;
            if (viejo != null) {
                viejo.setSelected(false);
            }
            m.setSelected(true);
        }
    }

    public boolean isSelected(ButtonModel m) {
        return m == selection;
    }

    public int getButtonCount() {
        if (buttons == null) {
            return 0;
        }
        return buttons.size();
    }
}
