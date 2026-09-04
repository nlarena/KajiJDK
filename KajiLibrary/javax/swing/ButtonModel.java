package javax.swing;

import java.awt.ItemSelectable;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

import javax.swing.event.ChangeListener;

/**
 * El estado de un boton, separado del boton.
 *
 * <p>Cinco bits —armado, seleccionado, habilitado, apretado, con el cursor encima— mas un
 * mnemonico, un comando y un grupo. El boton es una vista de esto: el aspecto lee el modelo para
 * decidir como pintar, y el escucha del mouse escribe en el modelo, no en el boton. La separacion
 * hace que un boton se pueda "apretar" desde un programa ({@code doClick}) exactamente como lo
 * apretaria un mouse.
 *
 * <p>La secuencia de un click es armar al entrar, apretar al bajar el boton del mouse, y disparar
 * la accion al soltarlo <em>si sigue armado</em>: mover el mouse afuera antes de soltar desarma, y
 * entonces soltar no dispara nada. Es lo que hace que un click arrepentido no cuente.
 */
public interface ButtonModel extends ItemSelectable {

    /** Si soltar el mouse ahora dispararia la accion. */
    boolean isArmed();

    boolean isSelected();

    boolean isEnabled();

    boolean isPressed();

    /** Si el cursor esta encima. */
    boolean isRollover();

    void setArmed(boolean b);

    void setSelected(boolean b);

    void setEnabled(boolean b);

    void setPressed(boolean b);

    void setRollover(boolean b);

    /** El mnemonico, como tecla virtual de {@code KeyEvent}. */
    void setMnemonic(int key);

    int getMnemonic();

    void setActionCommand(String s);

    String getActionCommand();

    /** El grupo de exclusion al que pertenece; lo llama el grupo al agregar y quitar. */
    void setGroup(ButtonGroup group);

    /** El grupo, o {@code null}; por omision ninguno. */
    default ButtonGroup getGroup() {
        return null;
    }

    void addActionListener(ActionListener l);

    void removeActionListener(ActionListener l);

    void addItemListener(ItemListener l);

    void removeItemListener(ItemListener l);

    void addChangeListener(ChangeListener l);

    void removeChangeListener(ChangeListener l);
}
