package javax.print;

import java.io.OutputStream;

/**
 * KajiLibrary's javax.print.StreamPrintService -- una "impresora" que escribe a un flujo.
 *
 * <p>Convierte en lugar de imprimir: se le da un documento en un formato y escribe otro en el flujo.
 * Asi se genera un PDF o un PostScript desde el mismo codigo que imprimiria en papel.
 *
 * <p>Es un {@link PrintService} completo --acepta trabajos, tiene atributos, se le pregunta que
 * soporta-- con dos diferencias:
 *
 * <ul>
 *   <li>tiene {@link #getOutputFormat}, que dice que escribe;
 *   <li>sirve <b>una sola vez</b>. {@link #dispose} cierra el trabajo; despues de eso no se le puede
 *       mandar nada mas, aunque el flujo siga abierto.
 * </ul>
 *
 * <p>{@link #dispose} <b>no cierra el flujo</b>: lo abrio quien lo paso, y cerrarlo es de el. Eso es
 * lo que permite escribir varios documentos seguidos en el mismo archivo, con un servicio nuevo por
 * cada uno.
 *
 * <p>Por lo mismo, {@code PrintServiceLookup.registerService} rechaza estos: no son impresoras que
 * alguien deba encontrar por casualidad.
 */
public abstract class StreamPrintService implements PrintService {

    /** Donde escribe. */
    private final OutputStream outStream;

    /** Si ya se cerro el trabajo. */
    private boolean disposed = false;

    /** No se construye sin flujo; el constructor sin argumentos no existe a proposito. */
    protected StreamPrintService(OutputStream out) {
        this.outStream = out;
    }

    /** Donde escribe. */
    public OutputStream getOutputStream() {
        return this.outStream;
    }

    /** Que formato escribe, como tipo MIME. */
    public abstract String getOutputFormat();

    /** Cierra el trabajo. No cierra el flujo; ver la nota de la clase. */
    public void dispose() {
        this.disposed = true;
    }

    /** Si ya se cerro. */
    public boolean isDisposed() {
        return this.disposed;
    }
}
