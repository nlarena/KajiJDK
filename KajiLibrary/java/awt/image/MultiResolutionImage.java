package java.awt.image;

import java.awt.Image;
import java.util.List;

/**
 * Una imagen que existe en **varias resoluciones** y elige cuál usar.
 *
 * <p>Es lo que hace que un ícono se vea nítido en una pantalla de alta densidad: la misma imagen
 * lógica guarda una versión de 16 píxeles y otra de 32, y quien la dibuja pide la que le sirve para
 * el tamaño en el que la va a mostrar.
 */
public interface MultiResolutionImage {

    /**
     * La versión que mejor sirve para dibujar a ese tamaño.
     *
     * @throws IllegalArgumentException si alguna de las dos medidas no es positiva
     */
    Image getResolutionVariant(double destImageWidth, double destImageHeight);

    /** Todas las versiones, de menor a mayor. */
    List<Image> getResolutionVariants();
}
