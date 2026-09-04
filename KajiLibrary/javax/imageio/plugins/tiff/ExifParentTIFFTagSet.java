package javax.imageio.plugins.tiff;

import java.util.ArrayList;
import java.util.List;

/**
 * KajiLibrary's javax.imageio.plugins.tiff.ExifParentTIFFTagSet -- las dos etiquetas que llevan a los directorios Exif.
 *
 * <p>No trae datos: trae <b>punteros</b>. Sus dos etiquetas cuelgan del directorio principal de un
 * TIFF y apuntan al directorio Exif y al de posicion.
 *
 * <p>Es lo que hace que un JPEG con Exif sea, por dentro, un TIFF con directorios anidados. Ver
 * {@link TIFFTag#isIFDPointer}.
 *
 * <p>Es un singleton: se pide con {@link #getInstance}. Las etiquetas y sus valores nombrados se
 * transcribieron del JDK 25 y no a mano; un numero cambiado produce un TIFF que otros programas leen
 * distinto.
 */
public final class ExifParentTIFFTagSet extends TIFFTagSet {

    /** El unico, armado la primera vez que se pide. */
    private static ExifParentTIFFTagSet theInstance = null;

    /** El numero de la etiqueta exif ifd pointer. */
    public static final int TAG_EXIF_IFD_POINTER = 34665;

    /** El numero de la etiqueta gps info ifd pointer. */
    public static final int TAG_GPS_INFO_IFD_POINTER = 34853;


    /** Se llega por {@link #getInstance}. */
    private ExifParentTIFFTagSet() {
        super(tags());
    }

    /** El conjunto. Ver la nota de la clase. */
    public static synchronized ExifParentTIFFTagSet getInstance() {
        if (theInstance == null) {
            theInstance = new ExifParentTIFFTagSet();
        }
        return theInstance;
    }

    /** Las etiquetas de este conjunto. */
    private static List<TIFFTag> tags() {
        List<TIFFTag> tags = new ArrayList<TIFFTag>();
        tags.add(new TIFFTag("ExifIFDPointer", 34665, ExifTIFFTagSet.getInstance()));
        tags.add(new TIFFTag("GPSInfoIFDPointer", 34853, ExifGPSTagSet.getInstance()));
        return tags;
    }
}
