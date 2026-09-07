package javax.swing.text;

import java.awt.Graphics;
import java.awt.Point;

import javax.swing.event.ChangeListener;

/**
 * El cursor de escritura: donde se va a escribir, y cuanto hay seleccionado.
 *
 * <h2>Dos numeros, no uno</h2>
 *
 * <p>El <em>punto</em> ({@link #getDot}) es donde esta el cursor; la <em>marca</em>
 * ({@link #getMark}) es donde empezo la seleccion. Si son iguales no hay seleccion. Por eso hay
 * dos operaciones y no una: {@link #setDot} mueve los dos y deshace la seleccion,
 * {@link #moveDot} mueve solo el punto y la extiende. Es exactamente la diferencia entre hacer
 * clic y arrastrar.
 *
 * <p>La <em>posicion magica</em> es la columna que el cursor recuerda al subir y bajar por lineas
 * de distinto largo: sin ella, bajar por una linea corta y volver a subir dejaria el cursor
 * corrido.
 */
public interface Caret {

    /** Lo instalan en un componente; es donde se engancha a escuchar. */
    void install(JTextComponent c);

    void deinstall(JTextComponent c);

    void paint(Graphics g);

    void addChangeListener(ChangeListener l);

    void removeChangeListener(ChangeListener l);

    boolean isVisible();

    void setVisible(boolean v);

    boolean isSelectionVisible();

    void setSelectionVisible(boolean v);

    /** Ver la nota de la interfaz sobre la posicion magica. */
    void setMagicCaretPosition(Point p);

    Point getMagicCaretPosition();

    /** Cada cuantos milisegundos parpadea; cero para que no parpadee. */
    void setBlinkRate(int rate);

    int getBlinkRate();

    int getDot();

    int getMark();

    /** Mueve el cursor y deshace la seleccion. */
    void setDot(int dot);

    /** Mueve el cursor sin mover la marca: extiende la seleccion. */
    void moveDot(int dot);
}
