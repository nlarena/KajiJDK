package javax.imageio.plugins.tiff;

import java.util.ArrayList;
import java.util.List;

/**
 * KajiLibrary's javax.imageio.plugins.tiff.ExifGPSTagSet -- Exif's position tags.
 *
 * <p>Latitude, longitude, altitude, bearing, satellite time. They hang from a directory of their
 * own pointed to by {@code ExifParentTIFFTagSet.TAG_GPS_INFO_IFD_POINTER}.
 *
 * <p>Latitude and longitude are stored as <b>three rationals</b> --degrees, minutes, seconds-- and
 * the hemisphere goes in a separate tag, as a letter. A reader that forgets that letter puts half
 * the world's photos in the wrong hemisphere.
 *
 * <p>It is a singleton: it is obtained with {@link #getInstance}. The tags and their named values
 * were transcribed from the JDK 25 and not by hand; a changed number produces a TIFF that other
 * programs read differently.
 */
public final class ExifGPSTagSet extends TIFFTagSet {

    /** The only one, built the first time it is asked for. */
    private static ExifGPSTagSet theInstance = null;

    /** The number of the gps version id tag. */
    public static final int TAG_GPS_VERSION_ID = 0;

    /** The number of the gps latitude ref tag. */
    public static final int TAG_GPS_LATITUDE_REF = 1;

    /** The number of the gps latitude tag. */
    public static final int TAG_GPS_LATITUDE = 2;

    /** The number of the gps longitude ref tag. */
    public static final int TAG_GPS_LONGITUDE_REF = 3;

    /** The number of the gps longitude tag. */
    public static final int TAG_GPS_LONGITUDE = 4;

    /** The number of the gps altitude ref tag. */
    public static final int TAG_GPS_ALTITUDE_REF = 5;

    /** The number of the gps altitude tag. */
    public static final int TAG_GPS_ALTITUDE = 6;

    /** The number of the gps time stamp tag. */
    public static final int TAG_GPS_TIME_STAMP = 7;

    /** The number of the gps satellites tag. */
    public static final int TAG_GPS_SATELLITES = 8;

    /** The number of the gps status tag. */
    public static final int TAG_GPS_STATUS = 9;

    /** The number of the gps measure mode tag. */
    public static final int TAG_GPS_MEASURE_MODE = 10;

    /** The number of the gps dop tag. */
    public static final int TAG_GPS_DOP = 11;

    /** The number of the gps speed ref tag. */
    public static final int TAG_GPS_SPEED_REF = 12;

    /** The number of the gps speed tag. */
    public static final int TAG_GPS_SPEED = 13;

    /** The number of the gps track ref tag. */
    public static final int TAG_GPS_TRACK_REF = 14;

    /** The number of the gps track tag. */
    public static final int TAG_GPS_TRACK = 15;

    /** The number of the gps img direction ref tag. */
    public static final int TAG_GPS_IMG_DIRECTION_REF = 16;

    /** The number of the gps img direction tag. */
    public static final int TAG_GPS_IMG_DIRECTION = 17;

    /** The number of the gps map datum tag. */
    public static final int TAG_GPS_MAP_DATUM = 18;

    /** The number of the gps dest latitude ref tag. */
    public static final int TAG_GPS_DEST_LATITUDE_REF = 19;

    /** The number of the gps dest latitude tag. */
    public static final int TAG_GPS_DEST_LATITUDE = 20;

    /** The number of the gps dest longitude ref tag. */
    public static final int TAG_GPS_DEST_LONGITUDE_REF = 21;

    /** The number of the gps dest longitude tag. */
    public static final int TAG_GPS_DEST_LONGITUDE = 22;

    /** The number of the gps dest bearing ref tag. */
    public static final int TAG_GPS_DEST_BEARING_REF = 23;

    /** The number of the gps dest bearing tag. */
    public static final int TAG_GPS_DEST_BEARING = 24;

    /** The number of the gps dest distance ref tag. */
    public static final int TAG_GPS_DEST_DISTANCE_REF = 25;

    /** The number of the gps dest distance tag. */
    public static final int TAG_GPS_DEST_DISTANCE = 26;

    /** The number of the gps processing method tag. */
    public static final int TAG_GPS_PROCESSING_METHOD = 27;

    /** The number of the gps area information tag. */
    public static final int TAG_GPS_AREA_INFORMATION = 28;

    /** The number of the gps date stamp tag. */
    public static final int TAG_GPS_DATE_STAMP = 29;

    /** The number of the gps differential tag. */
    public static final int TAG_GPS_DIFFERENTIAL = 30;

    /** A value of {@link #TAG_GPS_ALTITUDE_REF}. */
    public static final int ALTITUDE_REF_SEA_LEVEL = 0;

    /** A value of {@link #TAG_GPS_ALTITUDE_REF}. */
    public static final int ALTITUDE_REF_SEA_LEVEL_REFERENCE = 1;

    /** A value of {@link #TAG_GPS_DIFFERENTIAL}. */
    public static final int DIFFERENTIAL_CORRECTION_NONE = 0;

    /** A value of {@link #TAG_GPS_DIFFERENTIAL}. */
    public static final int DIFFERENTIAL_CORRECTION_APPLIED = 1;

    /** A value of {@link #TAG_GPS_VERSION_ID}. */
    public static final String GPS_VERSION_2_2 = "2200";

    /** A value of {@link #TAG_GPS_LATITUDE_REF}. */
    public static final String LATITUDE_REF_NORTH = "N";

    /** A value of {@link #TAG_GPS_LATITUDE_REF}. */
    public static final String LATITUDE_REF_SOUTH = "S";

    /** A value of {@link #TAG_GPS_LONGITUDE_REF}. */
    public static final String LONGITUDE_REF_EAST = "E";

    /** A value of {@link #TAG_GPS_LONGITUDE_REF}. */
    public static final String LONGITUDE_REF_WEST = "W";

    /** A value of {@link #TAG_GPS_STATUS}. */
    public static final String STATUS_MEASUREMENT_IN_PROGRESS = "A";

    /** A value of {@link #TAG_GPS_STATUS}. */
    public static final String STATUS_MEASUREMENT_INTEROPERABILITY = "V";

    /** A value of {@link #TAG_GPS_MEASURE_MODE}. */
    public static final String MEASURE_MODE_2D = "2";

    /** A value of {@link #TAG_GPS_MEASURE_MODE}. */
    public static final String MEASURE_MODE_3D = "3";

    /** A value of {@link #TAG_GPS_SPEED_REF}. */
    public static final String SPEED_REF_KILOMETERS_PER_HOUR = "K";

    /** A value of {@link #TAG_GPS_SPEED_REF}. */
    public static final String SPEED_REF_MILES_PER_HOUR = "M";

    /** A value of {@link #TAG_GPS_SPEED_REF}. */
    public static final String SPEED_REF_KNOTS = "N";

    /**
     * A value of {@link #TAG_GPS_TRACK_REF}, {@link #TAG_GPS_IMG_DIRECTION_REF} and
     * {@link #TAG_GPS_DEST_BEARING_REF}.
     */
    public static final String DIRECTION_REF_TRUE = "T";

    /**
     * A value of {@link #TAG_GPS_TRACK_REF}, {@link #TAG_GPS_IMG_DIRECTION_REF} and
     * {@link #TAG_GPS_DEST_BEARING_REF}.
     */
    public static final String DIRECTION_REF_MAGNETIC = "M";

    /** A value of {@link #TAG_GPS_DEST_DISTANCE_REF}. */
    public static final String DEST_DISTANCE_REF_KILOMETERS = "K";

    /** A value of {@link #TAG_GPS_DEST_DISTANCE_REF}. */
    public static final String DEST_DISTANCE_REF_MILES = "M";

    /** A value of {@link #TAG_GPS_DEST_DISTANCE_REF}. */
    public static final String DEST_DISTANCE_REF_KNOTS = "N";


    /** Reached through {@link #getInstance}. */
    private ExifGPSTagSet() {
        super(tags());
    }

    /** The set. See the class note. */
    public static synchronized ExifGPSTagSet getInstance() {
        if (theInstance == null) {
            theInstance = new ExifGPSTagSet();
        }
        return theInstance;
    }

    /** The tags of this set. */
    private static List<TIFFTag> tags() {
        List<TIFFTag> tags = new ArrayList<TIFFTag>();
        tags.add(new TIFFTag("GPSVersionID", 0, 2, -1));
        tags.add(new TIFFTag("GPSLatitudeRef", 1, 4, -1));
        tags.add(new TIFFTag("GPSLatitude", 2, 32, -1));
        tags.add(new TIFFTag("GPSLongitudeRef", 3, 4, -1));
        tags.add(new TIFFTag("GPSLongitude", 4, 32, -1));
        tags.add(new TagGPSAltitudeRef());
        tags.add(new TIFFTag("GPSAltitude", 6, 32, -1));
        tags.add(new TIFFTag("GPSTimeStamp", 7, 32, -1));
        tags.add(new TIFFTag("GPSSatellites", 8, 4, -1));
        tags.add(new TIFFTag("GPSStatus", 9, 4, -1));
        tags.add(new TIFFTag("GPSMeasureMode", 10, 4, -1));
        tags.add(new TIFFTag("GPSDOP", 11, 32, -1));
        tags.add(new TIFFTag("GPSSpeedRef", 12, 4, -1));
        tags.add(new TIFFTag("GPSSpeed", 13, 32, -1));
        tags.add(new TIFFTag("GPSTrackRef", 14, 4, -1));
        tags.add(new TIFFTag("GPSTrack", 15, 32, -1));
        tags.add(new TIFFTag("GPSImgDirectionRef", 16, 4, -1));
        tags.add(new TIFFTag("GPSImgDirection", 17, 32, -1));
        tags.add(new TIFFTag("GPSMapDatum", 18, 4, -1));
        tags.add(new TIFFTag("GPSDestLatitudeRef", 19, 4, -1));
        tags.add(new TIFFTag("GPSDestLatitude", 20, 32, -1));
        tags.add(new TIFFTag("GPSDestLongitudeRef", 21, 4, -1));
        tags.add(new TIFFTag("GPSDestLongitude", 22, 32, -1));
        tags.add(new TIFFTag("GPSDestBearingRef", 23, 4, -1));
        tags.add(new TIFFTag("GPSDestBearing", 24, 32, -1));
        tags.add(new TIFFTag("GPSDestDistanceRef", 25, 4, -1));
        tags.add(new TIFFTag("GPSDestDistance", 26, 32, -1));
        tags.add(new TIFFTag("GPSProcessingMethod", 27, 128, -1));
        tags.add(new TIFFTag("GPSAreaInformation", 28, 128, -1));
        tags.add(new TIFFTag("GPSDateStamp", 29, 4, -1));
        tags.add(new TagGPSDifferential());
        return tags;
    }

    /** {@code GPSAltitudeRef}, with the names of its values. */
    private static final class TagGPSAltitudeRef extends TIFFTag {

        TagGPSAltitudeRef() {
            super("GPSAltitudeRef", 5, 2, -1);
            addValueName(0, "Sea level");
            addValueName(1, "Sea level reference (negative value)");
        }
    }

    /** {@code GPSDifferential}, with the names of its values. */
    private static final class TagGPSDifferential extends TIFFTag {

        TagGPSDifferential() {
            super("GPSDifferential", 30, 8, -1);
            addValueName(0, "Measurement without differential correction");
            addValueName(1, "Differential correction applied");
        }
    }
}
