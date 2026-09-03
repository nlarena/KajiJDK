package java.awt.image;

import java.awt.Graphics;
import java.awt.Image;

/**
 * La base para una imagen de varias resoluciones.
 *
 * <p>Resuelve la parte aburrida: todo lo que {@link Image} pide —el ancho, el alto, las propiedades,
 * el productor— se contesta reenviándoselo a **una** de las versiones, la base, que la subclase
 * elige. Lo único que queda por escribir es cuál es esa base y cuáles son las variantes.
 *
 * <p>La base es la de resolución lógica: la que define cuánto mide la imagen en coordenadas de
 * usuario. Las otras versiones tienen más píxeles pero el mismo tamaño lógico, que es justamente lo
 * que hace que una pantalla densa las pueda usar sin que nada cambie de lugar.
 */
public abstract class AbstractMultiResolutionImage extends Image implements MultiResolutionImage {

    /** Para las subclases. */
    protected AbstractMultiResolutionImage() {
    }

    /** La versión que define el tamaño lógico. */
    protected abstract Image getBaseImage();

    /** El ancho de la versión base. */
    public int getWidth(ImageObserver observer) {
        return this.getBaseImage().getWidth(observer);
    }

    /** El alto de la versión base. */
    public int getHeight(ImageObserver observer) {
        return this.getBaseImage().getHeight(observer);
    }

    /** El productor de la versión base. */
    public ImageProducer getSource() {
        return this.getBaseImage().getSource();
    }

    /**
     * Un contexto para dibujar encima.
     *
     * @throws UnsupportedOperationException siempre: dibujar sobre una imagen de varias
     *     resoluciones tendría que dibujar sobre todas, y no hay forma de saber a qué escala se
     *     quiso cada trazo
     */
    public Graphics getGraphics() {
        throw new UnsupportedOperationException("getGraphics() not supported"
                + " on Multi-Resolution Images");
    }

    /** Una propiedad de la versión base. */
    public Object getProperty(String name, ImageObserver observer) {
        return this.getBaseImage().getProperty(name, observer);
    }
}
