package javax.imageio.event;

import java.util.EventListener;
import javax.imageio.ImageReader;

/**
 * KajiLibrary's javax.imageio.event.IIOReadProgressListener -- sigue el avance de una lectura.
 *
 * <p>Existe porque decodificar una imagen grande tarda, y {@code ImageReader.read} no vuelve hasta que
 * termina. Sin esto no hay forma de dibujar una barra de progreso ni de saber que sigue viva.
 *
 * <h2>Los tres pares de eventos</h2>
 *
 * <p>Cada par abre y cierra, y anidan:
 *
 * <ul>
 *   <li><b>secuencia</b>: solo en una lectura de varias imagenes de una vez. Envuelve a los demas;
 *   <li><b>imagen</b>: una imagen. {@link #imageProgress} llega varias veces en el medio, con un
 *       porcentaje de 0 a 100;
 *   <li><b>miniatura</b>: igual, para las vistas previas incrustadas.
 * </ul>
 *
 * <p>{@link #readAborted} <b>reemplaza</b> al {@code complete} que hubiera correspondido: si alguien
 * llamo {@code ImageReader.abort()}, llega este y no aquel. Un programa que solo escuche
 * {@code imageComplete} para cerrar su barra de progreso la deja abierta para siempre al cancelar.
 *
 * <p>Los avisos llegan en el hilo que esta leyendo, no en el de la interfaz. Bloquearlos frena la
 * decodificacion.
 */
public interface IIOReadProgressListener extends EventListener {

    /**
     * Empieza una lectura de varias imagenes.
     *
     * @param minIndex el indice de la primera
     */
    void sequenceStarted(ImageReader source, int minIndex);

    /** Termino la secuencia. */
    void sequenceComplete(ImageReader source);

    /** Empieza una imagen. */
    void imageStarted(ImageReader source, int imageIndex);

    /** Va por ese porcentaje, de 0 a 100. */
    void imageProgress(ImageReader source, float percentageDone);

    /** Termino la imagen. */
    void imageComplete(ImageReader source);

    /** Empieza una miniatura. */
    void thumbnailStarted(ImageReader source, int imageIndex, int thumbnailIndex);

    /** Va por ese porcentaje. */
    void thumbnailProgress(ImageReader source, float percentageDone);

    /** Termino la miniatura. */
    void thumbnailComplete(ImageReader source);

    /** Se cancelo. Ver la nota de la clase: viene en lugar del {@code complete}. */
    void readAborted(ImageReader source);
}
