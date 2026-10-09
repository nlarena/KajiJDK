package javax.imageio.plugins.tiff;

import java.util.ArrayList;
import java.util.List;

/**
 * KajiLibrary's javax.imageio.plugins.tiff.FaxTIFFTagSet -- the three TIFF tags for fax.
 *
 * <p>From the TIFF-F profile: how clean the received data is, how many bad lines there were, and
 * the longest run of consecutive bad lines. They are still there because fax over TIFF outlived
 * what anybody expected. (An earlier note said four tags and listed a transmission time; the JDK
 * set has these three.)
 *
 * <p>It is a singleton: it is obtained with {@link #getInstance}. The tags and their named values
 * were transcribed from the JDK 25 and not by hand; a changed number produces a TIFF that other
 * programs read differently.
 */
public final class FaxTIFFTagSet extends TIFFTagSet {

    /** The only one, built the first time it is asked for. */
    private static FaxTIFFTagSet theInstance = null;

    /** The number of the bad fax lines tag. */
    public static final int TAG_BAD_FAX_LINES = 326;

    /** The number of the clean fax data tag. */
    public static final int TAG_CLEAN_FAX_DATA = 327;

    /** A value of {@link #TAG_CLEAN_FAX_DATA}. */
    public static final int CLEAN_FAX_DATA_NO_ERRORS = 0;

    /** A value of {@link #TAG_CLEAN_FAX_DATA}. */
    public static final int CLEAN_FAX_DATA_ERRORS_CORRECTED = 1;

    /** A value of {@link #TAG_CLEAN_FAX_DATA}. */
    public static final int CLEAN_FAX_DATA_ERRORS_UNCORRECTED = 2;

    /** The number of the consecutive bad lines tag. */
    public static final int TAG_CONSECUTIVE_BAD_LINES = 328;


    /** Reached through {@link #getInstance}. */
    private FaxTIFFTagSet() {
        super(tags());
    }

    /** The set. See the class note. */
    public static synchronized FaxTIFFTagSet getInstance() {
        if (theInstance == null) {
            theInstance = new FaxTIFFTagSet();
        }
        return theInstance;
    }

    /** The tags of this set. */
    private static List<TIFFTag> tags() {
        List<TIFFTag> tags = new ArrayList<TIFFTag>();
        tags.add(new TIFFTag("BadFaxLines", 326, 24, 1));
        tags.add(new TagCleanFaxData());
        tags.add(new TIFFTag("ConsecutiveBadFaxLines", 328, 24, 1));
        return tags;
    }

    /** {@code CleanFaxData}, with the names of its values. */
    private static final class TagCleanFaxData extends TIFFTag {

        TagCleanFaxData() {
            super("CleanFaxData", 327, 8, 1);
            addValueName(0, "No errors");
            addValueName(1, "Errors corrected");
            addValueName(2, "Errors uncorrected");
        }
    }
}
