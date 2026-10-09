package javax.imageio.plugins.tiff;

import java.util.ArrayList;
import java.util.List;

/**
 * KajiLibrary's javax.imageio.plugins.tiff.GeoTIFFTagSet -- the six GeoTIFF tags.
 *
 * <p>Georeferencing: how pixels translate to ground coordinates, and in which reference system.
 *
 * <p>They are few because almost all of GeoTIFF lives inside a single one:
 * {@code TAG_GEO_KEY_DIRECTORY} is an array of integers with its own format of nested keys. It is a
 * format inside a tag, and this set only declares the container.
 *
 * <p>It is a singleton: it is obtained with {@link #getInstance}. The tags and their named values
 * were transcribed from the JDK 25 and not by hand; a changed number produces a TIFF that other
 * programs read differently.
 */
public final class GeoTIFFTagSet extends TIFFTagSet {

    /** The only one, built the first time it is asked for. */
    private static GeoTIFFTagSet theInstance = null;

    /** The number of the model pixel scale tag. */
    public static final int TAG_MODEL_PIXEL_SCALE = 33550;

    /** The number of the model transformation tag. */
    public static final int TAG_MODEL_TRANSFORMATION = 34264;

    /** The number of the model tie point tag. */
    public static final int TAG_MODEL_TIE_POINT = 33922;

    /** The number of the geo key directory tag. */
    public static final int TAG_GEO_KEY_DIRECTORY = 34735;

    /** The number of the geo double params tag. */
    public static final int TAG_GEO_DOUBLE_PARAMS = 34736;

    /** The number of the geo ascii params tag. */
    public static final int TAG_GEO_ASCII_PARAMS = 34737;


    /** Reached through {@link #getInstance}. */
    private GeoTIFFTagSet() {
        super(tags());
    }

    /** The set. See the class note. */
    public static synchronized GeoTIFFTagSet getInstance() {
        if (theInstance == null) {
            theInstance = new GeoTIFFTagSet();
        }
        return theInstance;
    }

    /** The tags of this set. */
    private static List<TIFFTag> tags() {
        List<TIFFTag> tags = new ArrayList<TIFFTag>();
        tags.add(new TIFFTag("ModelPixelScaleTag", 33550, 4096, -1));
        tags.add(new TIFFTag("ModelTiepointTag", 33922, 4096, -1));
        tags.add(new TIFFTag("ModelTransformationTag", 34264, 4096, -1));
        tags.add(new TIFFTag("GeoKeyDirectoryTag", 34735, 8, -1));
        tags.add(new TIFFTag("GeoDoubleParamsTag", 34736, 4096, -1));
        tags.add(new TIFFTag("GeoAsciiParamsTag", 34737, 4, -1));
        return tags;
    }
}
