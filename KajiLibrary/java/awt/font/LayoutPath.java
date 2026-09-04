package java.awt.font;

import java.awt.geom.Point2D;

/**
 * Un camino sobre el que se apoya el texto, que no tiene por qué ser una recta.
 *
 * <p>Convierte entre dos sistemas de coordenadas: el de la **pantalla**, donde el texto ya está
 * dibujado, y el del **renglón**, donde una coordenada es "tanto avance a lo largo del renglón,
 * tanto separado de la línea de base". Sobre una recta la conversión es trivial; sobre un círculo o
 * una curva no, y ahí está el punto de la clase.
 *
 * <p>{@link #pointToPath} tiene que decir además de qué **lado** cayó el punto, porque un camino
 * curvo puede pasar cerca de sí mismo y la distancia sola no alcanza para saber a qué parte del
 * renglón corresponde un clic.
 */
public abstract class LayoutPath {

    /** Para las subclases. */
    protected LayoutPath() {
    }

    /**
     * De coordenadas de pantalla a coordenadas de renglón.
     *
     * @param point el punto de pantalla
     * @param location dónde escribir el resultado
     * @return `true` si el punto cae del lado izquierdo del camino, mirando en su dirección
     */
    public abstract boolean pointToPath(Point2D point, Point2D location);

    /**
     * De coordenadas de renglón a coordenadas de pantalla.
     *
     * @param location el punto sobre el renglón
     * @param preceding si hay que resolver una ambigüedad tomando el tramo anterior
     * @param point dónde escribir el resultado
     */
    public abstract void pathToPoint(Point2D location, boolean preceding, Point2D point);
}
