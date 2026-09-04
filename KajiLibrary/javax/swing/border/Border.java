package javax.swing.border;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

/**
 * El marco que rodea a un componente.
 *
 * <h2>Por que un objeto y no una propiedad del componente</h2>
 *
 * <p>Porque asi los bordes se <strong>componen</strong>: {@link CompoundBorder} mete uno adentro de
 * otro y el resultado es otro {@code Border}, indistinguible de los basicos. Si el marco fuera un
 * puñado de campos en el componente —grosor, color, estilo— esa combinacion no existiria.
 *
 * <p>De ahi tambien que un borde sea normalmente <strong>inmutable y compartible</strong>: no guarda
 * nada del componente que lo usa, asi que una misma instancia sirve para cien botones.
 *
 * <h2>Los tres metodos, y por que {@link #isBorderOpaque} no sobra</h2>
 *
 * <p>Pintar y declarar cuanto espacio ocupa son los dos obvios. El tercero es una promesa que Swing
 * usa para optimizar: un borde opaco cubre <em>todos</em> los pixeles de su area, asi que lo que
 * haya debajo no hace falta dibujarlo. Mentir ahi no rompe el layout — deja basura en pantalla.
 */
public interface Border {

    /** Se dibuja alrededor de {@code c}, en el rectangulo dado. */
    void paintBorder(Component c, Graphics g, int x, int y, int width, int height);

    /** Cuanto espacio se reserva de cada lado. */
    Insets getBorderInsets(Component c);

    /** Si cubre todos los pixeles de su area; ver la nota de la interfaz. */
    boolean isBorderOpaque();
}
