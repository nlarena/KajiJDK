package javax.swing.colorchooser;

import java.awt.Color;
import javax.swing.event.ChangeListener;

/**
 * El color elegido, y quien quiere enterarse cuando cambia.
 *
 * <p>Es lo que hace que un {@link javax.swing.JColorChooser} con cuatro solapas se comporte como
 * una sola cosa: todas las solapas leen y escriben este modelo, y ninguna sabe de las otras. Mover
 * el deslizador rojo en RGB actualiza los numeros de HSV porque HSV escucha al modelo, no al
 * deslizador.
 *
 * <p>Es tambien el punto por el que un programa se engancha: escuchar el modelo es la forma de
 * enterarse de lo que el usuario elige sin depender de la interfaz.
 */
public interface ColorSelectionModel {

    /** El color elegido. */
    Color getSelectedColor();

    /**
     * Elige ese color.
     *
     * <p>Si es distinto del que habia, se avisa a los escuchas.
     */
    void setSelectedColor(Color color);

    /** Agrega un escucha del cambio de color. */
    void addChangeListener(ChangeListener listener);

    /** Saca un escucha del cambio de color. */
    void removeChangeListener(ChangeListener listener);
}
