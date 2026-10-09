package javax.print;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * KajiLibrary's javax.print.StreamPrintServiceFactory -- factory of stream converters.
 *
 * <p>A factory is needed and a constructor is not enough because the service is bound to a concrete
 * stream: one per output file. The factory is what is found once and used many times.
 *
 * <p>The lookup crosses <b>input and output</b>: {@link #lookupStreamPrintServiceFactories} asks
 * for the document's format and the MIME type one wants to write, and returns the ones that can do
 * that conversion. Both accept null not to filter.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>This library ships no converters: really writing PDF or PostScript needs a rasterising and
 * typography engine that is not here. The lookup works and returns empty; declaring a factory as a
 * service, this works without changes.
 */
public abstract class StreamPrintServiceFactory {

    /** For subclasses. */
    protected StreamPrintServiceFactory() {
    }

    /**
     * The factories that convert from that format to that output type.
     *
     * @param flavor the document's format, or null
     * @param outputMimeType the MIME type to write, or null
     * @return the ones that serve; never null, may be empty
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
            // A broken factory does not bring the lookup down; see PrintServiceLookup.
        }
        return found.toArray(new StreamPrintServiceFactory[found.size()]);
    }

    /** What it writes, as a MIME type. */
    public abstract String getOutputFormat();

    /** Which input formats it accepts. */
    public abstract DocFlavor[] getSupportedDocFlavors();

    /** A service that writes to that stream. */
    public abstract StreamPrintService getPrintService(OutputStream out);

    /** The lookup's filter. */
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
