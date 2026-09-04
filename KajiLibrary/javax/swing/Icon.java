package javax.swing;

import java.awt.Component;
import java.awt.Graphics;

/**
 * Algo que sabe dibujarse en un tamano fijo.
 *
 * <h2>Por que no es simplemente una imagen</h2>
 *
 * <p>Una {@link java.awt.Image} son pixeles ya decididos. Un icono es <strong>una instruccion de
 * dibujo</strong>: se le pide que se pinte en un lugar, y puede resolverlo como quiera —con una
 * imagen, con trazos, o mirando al componente que se lo pide para elegir un color acorde al tema—.
 * De ahi que {@link #paintIcon} reciba el {@link Component}: no es decoracion, es lo que permite que
 * un mismo icono se vea distinto en un boton habilitado y en uno deshabilitado.
 *
 * <p>El tamano se declara aparte y por adelantado, porque quien hace el layout necesita saber cuanto
 * ocupa <em>antes</em> de que se dibuje nada.
 */
public interface Icon {

    /**
     * Se dibuja con su esquina superior izquierda en {@code (x, y)}.
     *
     * @param c el componente que lo pide, que el icono puede consultar; puede ser {@code null}
     */
    void paintIcon(Component c, Graphics g, int x, int y);

    /** Cuanto mide de ancho. */
    int getIconWidth();

    /** Cuanto mide de alto. */
    int getIconHeight();
}
