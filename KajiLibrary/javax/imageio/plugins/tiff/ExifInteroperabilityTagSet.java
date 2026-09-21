package javax.imageio.plugins.tiff;

import java.util.ArrayList;
import java.util.List;

/**
 * KajiLibrary's javax.imageio.plugins.tiff.ExifInteroperabilityTagSet -- Exif's interoperability
 * tag.
 *
 * <p>The smallest set in the package: a single tag, which says which interoperability profile the
 * file follows --{@code "R98"} for classic Exif, {@code "THM"} for a thumbnail. (An earlier note
 * spoke of two tags; there is one, with those two named values.)
 *
 * <p>It hangs from a directory pointed to by {@code
 * ExifTIFFTagSet.TAG_INTEROPERABILITY_IFD_POINTER}.
 *
 * <p>It is a singleton: it is obtained with {@link #getInstance}. The tags and their named values
 * were transcribed from the JDK 25 and not by hand; a changed number produces a TIFF that other
 * programs read differently.
 */
public final class ExifInteroperabilityTagSet extends TIFFTagSet {

    /** The only one, built the first time it is asked for. */
    private static ExifInteroperabilityTagSet theInstance = null;

    /** The number of the interoperability index tag. */
    public static final int TAG_INTEROPERABILITY_INDEX = 1;

    /** A value of {@link #TAG_INTEROPERABILITY_INDEX}. */
    public static final String INTEROPERABILITY_INDEX_R98 = "R98";

    /** A value of {@link #TAG_INTEROPERABILITY_INDEX}. */
    public static final String INTEROPERABILITY_INDEX_THM = "THM";


    /** Reached through {@link #getInstance}. */
    private ExifInteroperabilityTagSet() {
        super(tags());
    }

    /** The set. See the class note. */
    public static synchronized ExifInteroperabilityTagSet getInstance() {
        if (theInstance == null) {
            theInstance = new ExifInteroperabilityTagSet();
        }
        return theInstance;
    }

    /** The tags of this set. */
    private static List<TIFFTag> tags() {
        List<TIFFTag> tags = new ArrayList<TIFFTag>();
        tags.add(new TIFFTag("InteroperabilityIndex", 1, 4, -1));
        return tags;
    }
}
