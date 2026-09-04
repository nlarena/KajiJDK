package javax.imageio.plugins.tiff;

import java.util.ArrayList;
import java.util.List;

/**
 * KajiLibrary's javax.imageio.plugins.tiff.ExifInteroperabilityTagSet -- las dos etiquetas de interoperabilidad de Exif.
 *
 * <p>El conjunto mas chico del paquete: dice a que perfil de interoperabilidad se ajusta el archivo
 * --{@code "R98"} para el Exif clasico, {@code "THM"} para una miniatura--.
 *
 * <p>Cuelga de un directorio al que apunta {@code ExifTIFFTagSet.TAG_INTEROPERABILITY_IFD_POINTER}.
 *
 * <p>Es un singleton: se pide con {@link #getInstance}. Las etiquetas y sus valores nombrados se
 * transcribieron del JDK 25 y no a mano; un numero cambiado produce un TIFF que otros programas leen
 * distinto.
 */
public final class ExifInteroperabilityTagSet extends TIFFTagSet {

    /** El unico, armado la primera vez que se pide. */
    private static ExifInteroperabilityTagSet theInstance = null;

    /** El numero de la etiqueta interoperability index. */
    public static final int TAG_INTEROPERABILITY_INDEX = 1;

    /** Un valor de interoperability. */
    public static final String INTEROPERABILITY_INDEX_R98 = "R98";

    /** Un valor de interoperability. */
    public static final String INTEROPERABILITY_INDEX_THM = "THM";


    /** Se llega por {@link #getInstance}. */
    private ExifInteroperabilityTagSet() {
        super(tags());
    }

    /** El conjunto. Ver la nota de la clase. */
    public static synchronized ExifInteroperabilityTagSet getInstance() {
        if (theInstance == null) {
            theInstance = new ExifInteroperabilityTagSet();
        }
        return theInstance;
    }

    /** Las etiquetas de este conjunto. */
    private static List<TIFFTag> tags() {
        List<TIFFTag> tags = new ArrayList<TIFFTag>();
        tags.add(new TIFFTag("InteroperabilityIndex", 1, 4, -1));
        return tags;
    }
}
