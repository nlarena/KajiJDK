package javax.swing;

import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * Lo que un componente puede contarle al {@link JScrollPane} que lo muestra.
 *
 * <p>Sin esto, un panel con barras trata a su contenido como un rectangulo mudo: lo desplaza de a
 * un pixel y lo deja de su tamano preferido. Implementarla es como una lista dice "una rueda mueve
 * una fila, no tres pixeles" y como un area de texto dice "hazme tan ancho como la ventana y no me
 * pongas barra horizontal".
 *
 * <p>Las dos ultimas son las que mas cambian lo que se ve: contestar {@code true} en
 * {@link #getScrollableTracksViewportWidth} obliga al contenido a medir lo que mide la ventana, y
 * entonces nunca hace falta desplazar en esa direccion.
 */
public interface Scrollable {

    /** Que tamano querria tener la ventana que lo muestra. */
    Dimension getPreferredScrollableViewportSize();

    /**
     * Cuanto avanzar en un paso chico —una flecha, una muesca de rueda—, en pixeles.
     *
     * @param visibleRect lo que se ve ahora, en coordenadas del componente
     * @param orientation {@code SwingConstants.VERTICAL} u {@code HORIZONTAL}
     * @param direction negativo hacia arriba o a la izquierda, positivo al reves
     */
    int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction);

    /** Cuanto avanzar en un paso grande —clic en la pista, av pag—, en pixeles. */
    int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction);

    /** Si su ancho debe ser siempre el de la ventana; ver la nota de la interfaz. */
    boolean getScrollableTracksViewportWidth();

    /** Si su alto debe ser siempre el de la ventana. */
    boolean getScrollableTracksViewportHeight();
}
