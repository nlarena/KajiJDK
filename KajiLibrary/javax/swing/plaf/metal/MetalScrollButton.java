package javax.swing.plaf.metal;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicArrowButton;

/**
 * Una de las dos flechas de una barra de desplazamiento de Metal.
 *
 * <h2>El pixel que se cede al vecino</h2>
 *
 * <p>Los tamanos de esta clase son asimetricos y a proposito. Una barra <strong>pegada</strong> a
 * un panel no dibuja su borde de afuera -- lo dibuja el panel -- asi que cada boton cede dos
 * pixeles del lado que da al borde. Una barra <strong>suelta</strong> si lo dibuja, y entonces el
 * boton del final cede uno solo.
 *
 * <p>La asimetria es exacta y esta medida: con ancho 16, el de arriba mide {@code 16x14} siempre;
 * el de abajo, {@code 16x14} pegado y {@code 16x15} suelto. Acostados, el de la derecha va de
 * {@code 14x16} a {@code 15x16} y el de la izquierda no cambia. Es siempre el boton del extremo
 * lejano el que recupera el pixel, porque el borde que se ahorra es el otro.
 *
 * <p>Una direccion que no sea uno de los cuatro puntos cardinales da tamano cero. No es un error:
 * es lo que hace el JDK, y un boton de tamano cero simplemente no se ve.
 *
 * <p>El maximo es {@code Integer.MAX_VALUE} en las dos dimensiones, asi que el boton se deja
 * estirar por su contenedor todo lo que haga falta.
 */
public class MetalScrollButton extends BasicArrowButton {

    private static final Dimension MAXIMO =
            new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);

    private int buttonWidth;
    private boolean freeStanding;

    /**
     * @param direction uno de los cuatro puntos cardinales
     * @param width el ancho de la barra a la que pertenece
     * @param freeStanding si la barra dibuja su propio borde de afuera
     */
    public MetalScrollButton(int direction, int width, boolean freeStanding) {
        super(direction);
        this.buttonWidth = width;
        this.freeStanding = freeStanding;
    }

    public void setFreeStanding(boolean freeStanding) {
        this.freeStanding = freeStanding;
    }

    public int getButtonWidth() {
        return buttonWidth;
    }

    /** Ver la nota de la clase: la asimetria esta medida. */
    public Dimension getPreferredSize() {
        int d = getDirection();
        if (d == SwingConstants.NORTH) {
            return new Dimension(buttonWidth, buttonWidth - 2);
        }
        if (d == SwingConstants.SOUTH) {
            return new Dimension(buttonWidth, buttonWidth - (freeStanding ? 1 : 2));
        }
        if (d == SwingConstants.EAST) {
            return new Dimension(buttonWidth - (freeStanding ? 1 : 2), buttonWidth);
        }
        if (d == SwingConstants.WEST) {
            return new Dimension(buttonWidth - 2, buttonWidth);
        }
        return new Dimension(0, 0);
    }

    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    /** Sin tope: el contenedor lo estira lo que quiera. */
    public Dimension getMaximumSize() {
        return new Dimension(MAXIMO);
    }

    public void paint(Graphics g) {
        super.paint(g);
    }
}
