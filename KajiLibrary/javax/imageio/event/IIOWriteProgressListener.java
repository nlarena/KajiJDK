package javax.imageio.event;

import java.util.EventListener;
import javax.imageio.ImageWriter;

/**
 * KajiLibrary's javax.imageio.event.IIOWriteProgressListener -- sigue el avance de una escritura.
 *
 * <p>El espejo de {@link IIOReadProgressListener}, con una diferencia: <b>no hay par de secuencia</b>.
 * Escribir varias imagenes se hace de a una con {@code writeToSequence}, asi que cada llamada abre y
 * cierra su propio par de imagen.
 *
 * <p>{@link #writeAborted} reemplaza al {@code imageComplete} que hubiera correspondido, igual que en
 * la lectura -- y aca importa mas: un archivo cuya escritura se cancelo queda a medio escribir, y hay
 * que borrarlo. Un programa que solo escuche {@code imageComplete} deja archivos truncados.
 */
public interface IIOWriteProgressListener extends EventListener {

    /** Empieza a escribir una imagen. */
    void imageStarted(ImageWriter source, int imageIndex);

    /** Va por ese porcentaje, de 0 a 100. */
    void imageProgress(ImageWriter source, float percentageDone);

    /** Termino. */
    void imageComplete(ImageWriter source);

    /** Empieza una miniatura. */
    void thumbnailStarted(ImageWriter source, int imageIndex, int thumbnailIndex);

    /** Va por ese porcentaje. */
    void thumbnailProgress(ImageWriter source, float percentageDone);

    /** Termino la miniatura. */
    void thumbnailComplete(ImageWriter source);

    /** Se cancelo. Ver la nota de la clase: el archivo queda a medio escribir. */
    void writeAborted(ImageWriter source);
}
