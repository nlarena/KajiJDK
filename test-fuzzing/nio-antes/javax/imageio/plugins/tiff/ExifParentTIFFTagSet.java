package javax.imageio.plugins.tiff;

import java.util.ArrayList;
import java.util.List;

/**
 * KajiLibrary's javax.imageio.plugins.tiff.ExifParentTIFFTagSet -- the two tags that lead to the
 * Exif directories.
 *
 * <p>It carries no data: it carries <b>pointers</b>. Its two tags hang from a TIFF's main directory
 * and point to the Exif directory and to the position one.
 *
 * <p>It is what makes a JPEG with Exif be, inside, a TIFF with nested directories. See
 * {@link TIFFTag#isIFDPointer}.
 *
 * <p>It is a singleton: it is obtained with {@link #getInstance}. The tags and their named values
 * were transcribed from the JDK 25 and not by hand; a changed number produces a TIFF that other
 * programs read differently.
 */
public final class ExifParentTIFFTagSet extends TIFFTagSet {

    /** The only one, built the first time it is asked for. */
    private static ExifParentTIFFTagSet theInstance = null;

    /** The number of the exif ifd pointer tag. */
    public static final int TAG_EXIF_IFD_POINTER = 34665;

    /** The number of the gps info ifd pointer tag. */
    public static final int TAG_GPS_INFO_IFD_POINTER = 34853;


    /** Reached through {@link #getInstance}. */
    private ExifParentTIFFTagSet() {
        super(tags());
    }

    /** The set. See the class note. */
    public static synchronized ExifParentTIFFTagSet getInstance() {
        if (theInstance == null) {
            theInstance = new ExifParentTIFFTagSet();
        }
        return theInstance;
    }

    /** The tags of this set. */
    private static List<TIFFTag> tags() {
        List<TIFFTag> tags = new ArrayList<TIFFTag>();
        tags.add(new TIFFTag("ExifIFDPointer", 34665, ExifTIFFTagSet.getInstance()));
        tags.add(new TIFFTag("GPSInfoIFDPointer", 34853, ExifGPSTagSet.getInstance()));
        return tags;
    }
}
