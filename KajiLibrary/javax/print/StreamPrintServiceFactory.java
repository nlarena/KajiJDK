package javax.print;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * KajiLibrary's javax.print.StreamPrintServiceFactory -- fabrica de conversores a flujo.
 *
 * <p>Hace falta una fabrica y no basta con un constructor porque el servicio se ata a un flujo
 * concreto: uno por archivo de salida. La fabrica es la que se encuentra una vez y se usa muchas.
 *
 * <p>La busqueda cruza <b>entrada y salida</b>: {@link #lookupStreamPrintServiceFactories} pide el
 * formato del documento y el tipo MIME que se quiere escribir, y devuelve las que saben hacer esa
 * conversion. Los dos aceptan null para no filtrar.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Esta biblioteca no trae conversores: escribir PDF o PostScript de verdad pide un motor de
 * rasterizado y tipografia que no esta. La busqueda funciona y devuelve vacio; declarando una fabrica
 * como servicio, anda sin cambios.
 */
public abstract class StreamPrintServiceFactory {

    /** Para las subclases. */
    protected StreamPrintServiceFactory() {
    }

    /**
     * Las fabricas que convierten de ese formato a ese tipo de salida.
     *
     * @param flavor el formato del documento, o null
     * @param outputMimeType el tipo MIME a escribir, o null
     * @return las que sirven; nunca null, puede estar vacio
     */
    public static StreamPrintServiceFactory[] lookupStreamPrintServiceFactories(
        DocFlavor flavor, String outputMimeType) {
        ArrayList<StreamPrintServiceFactory> found = new ArrayList<StreamPrintServiceFactory>();
        try {
            Iterator<StreamPrintServiceFactory> it =
                ServiceLoader.load(StreamPrintServiceFactory.class).iterator();
            while (it.hasNext()) {
                StreamPrintServiceFactory f = it.next();
                if (matches(f, flavor, outputMimeType)) {
                    found.add(f);
                }
            }
        } catch (Throwable e) {
            // Una fabrica rota no tumba la busqueda; ver PrintServiceLookup.
        }
        return found.toArray(new StreamPrintServiceFactory[found.size()]);
    }

    /** Que escribe, como tipo MIME. */
    public abstract String getOutputFormat();

    /** Que formatos de entrada acepta. */
    public abstract DocFlavor[] getSupportedDocFlavors();

    /** Un servicio que escribe a ese flujo. */
    public abstract StreamPrintService getPrintService(OutputStream out);

    /** El filtro de la busqueda. */
    private static boolean matches(StreamPrintServiceFactory f, DocFlavor flavor,
                                   String outputMimeType) {
        if (outputMimeType != null && !outputMimeType.equalsIgnoreCase(f.getOutputFormat())) {
            return false;
        }
        if (flavor == null) {
            return true;
        }
        DocFlavor[] supported = f.getSupportedDocFlavors();
        int i = 0;
        while (supported != null && i < supported.length) {
            if (flavor.equals(supported[i])) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }
}
