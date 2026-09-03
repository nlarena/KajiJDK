package java.awt.image;

/**
 * Quien quiere enterarse de que una parte de una imagen por mosaicos se puso o se dejó de escribir.
 *
 * <p>Sirve para saber cuándo un mosaico terminó de modificarse y se puede volcar a disco o a
 * pantalla sin agarrarlo a medio escribir.
 */
public interface TileObserver {

    /**
     * Avisa que un mosaico se tomó para escribir, o se soltó.
     *
     * @param willBeWritable `true` si se acaba de tomar, `false` si se soltó
     */
    void tileUpdate(WritableRenderedImage source, int tileX, int tileY, boolean willBeWritable);
}
