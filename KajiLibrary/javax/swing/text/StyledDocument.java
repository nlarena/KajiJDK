package javax.swing.text;

import java.awt.Color;
import java.awt.Font;

/**
 * Un documento con estilos: texto que ademas tiene forma.
 *
 * <p>Agrega tres cosas sobre {@link Document}: un juego de {@link Style} con nombre, la capacidad
 * de aplicar atributos a un tramo —de caracter o de parrafo—, y la de preguntar como se ve un
 * conjunto de atributos ({@link #getFont}, {@link #getForeground}).
 *
 * <p>La distincion entre atributos de caracter y de parrafo no es un detalle: los de caracter
 * valen para un tramo cualquiera, y los de parrafo valen para el parrafo entero aunque el tramo
 * marcado sea una palabra. Pedir "centrado" sobre tres letras centra el parrafo.
 *
 * <p>Un <em>estilo logico</em> es el estilo que un parrafo tiene asignado como padre de todos sus
 * atributos: cambiarlo cambia el parrafo entero de golpe.
 */
public interface StyledDocument extends Document {

    Style addStyle(String nm, Style parent);

    void removeStyle(String nm);

    Style getStyle(String nm);

    /** Aplica atributos de caracter a ese tramo; {@code replace} borra los que habia. */
    void setCharacterAttributes(int offset, int length, AttributeSet s, boolean replace);

    /** Aplica atributos de parrafo a los parrafos que toca ese tramo. */
    void setParagraphAttributes(int offset, int length, AttributeSet s, boolean replace);

    /** El estilo del que cuelga ese parrafo; ver la nota de la interfaz. */
    void setLogicalStyle(int pos, Style s);

    Style getLogicalStyle(int p);

    Element getParagraphElement(int pos);

    /** El elemento hoja que contiene esa posicion: el tramo con los mismos atributos. */
    Element getCharacterElement(int pos);

    Color getForeground(AttributeSet attr);

    Color getBackground(AttributeSet attr);

    Font getFont(AttributeSet attr);
}
