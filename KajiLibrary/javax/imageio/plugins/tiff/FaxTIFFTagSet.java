package javax.imageio.plugins.tiff;

import java.util.ArrayList;
import java.util.List;

/**
 * KajiLibrary's javax.imageio.plugins.tiff.FaxTIFFTagSet -- las cuatro etiquetas de TIFF para fax.
 *
 * <p>Del perfil TIFF-F: modos de limpieza de la senal, tiempo de transmision, cantidad de lineas
 * malas. Son de 1994 y siguen ahi porque el fax sobre TIFF sobrevivio mas de lo que nadie esperaba.
 *
 * <p>Es un singleton: se pide con {@link #getInstance}. Las etiquetas y sus valores nombrados se
 * transcribieron del JDK 25 y no a mano; un numero cambiado produce un TIFF que otros programas leen
 * distinto.
 */
public final class FaxTIFFTagSet extends TIFFTagSet {

    /** El unico, armado la primera vez que se pide. */
    private static FaxTIFFTagSet theInstance = null;

    /** El numero de la etiqueta bad fax lines. */
    public static final int TAG_BAD_FAX_LINES = 326;

    /** El numero de la etiqueta clean fax data. */
    public static final int TAG_CLEAN_FAX_DATA = 327;

    /** Un valor de clean. */
    public static final int CLEAN_FAX_DATA_NO_ERRORS = 0;

    /** Un valor de clean. */
    public static final int CLEAN_FAX_DATA_ERRORS_CORRECTED = 1;

    /** Un valor de clean. */
    public static final int CLEAN_FAX_DATA_ERRORS_UNCORRECTED = 2;

    /** El numero de la etiqueta consecutive bad lines. */
    public static final int TAG_CONSECUTIVE_BAD_LINES = 328;


    /** Se llega por {@link #getInstance}. */
    private FaxTIFFTagSet() {
        super(tags());
    }

    /** El conjunto. Ver la nota de la clase. */
    public static synchronized FaxTIFFTagSet getInstance() {
        if (theInstance == null) {
            theInstance = new FaxTIFFTagSet();
        }
        return theInstance;
    }

    /** Las etiquetas de este conjunto. */
    private static List<TIFFTag> tags() {
        List<TIFFTag> tags = new ArrayList<TIFFTag>();
        tags.add(new TIFFTag("BadFaxLines", 326, 24, 1));
        tags.add(new TagCleanFaxData());
        tags.add(new TIFFTag("ConsecutiveBadFaxLines", 328, 24, 1));
        return tags;
    }

    /** {@code CleanFaxData}, con los nombres de sus valores. */
    private static final class TagCleanFaxData extends TIFFTag {

        TagCleanFaxData() {
            super("CleanFaxData", 327, 8, 1);
            addValueName(0, "No errors");
            addValueName(1, "Errors corrected");
            addValueName(2, "Errors uncorrected");
        }
    }
}
