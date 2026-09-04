package javax.imageio.plugins.tiff;

import java.util.ArrayList;
import java.util.List;

/**
 * KajiLibrary's javax.imageio.plugins.tiff.ExifGPSTagSet -- las etiquetas de posicion de Exif.
 *
 * <p>Latitud, longitud, altura, rumbo, hora del satelite. Cuelgan de un directorio propio al que
 * apunta {@code ExifParentTIFFTagSet.TAG_GPS_INFO_IFD_POINTER}.
 *
 * <p>La latitud y la longitud se guardan como <b>tres racionales</b> --grados, minutos, segundos-- y
 * el hemisferio va en una etiqueta aparte, como una letra. Un lector que se olvide de esa letra pone
 * la mitad de las fotos del mundo en el hemisferio equivocado.
 *
 * <p>Es un singleton: se pide con {@link #getInstance}. Las etiquetas y sus valores nombrados se
 * transcribieron del JDK 25 y no a mano; un numero cambiado produce un TIFF que otros programas leen
 * distinto.
 */
public final class ExifGPSTagSet extends TIFFTagSet {

    /** El unico, armado la primera vez que se pide. */
    private static ExifGPSTagSet theInstance = null;

    /** El numero de la etiqueta gps version id. */
    public static final int TAG_GPS_VERSION_ID = 0;

    /** El numero de la etiqueta gps latitude ref. */
    public static final int TAG_GPS_LATITUDE_REF = 1;

    /** El numero de la etiqueta gps latitude. */
    public static final int TAG_GPS_LATITUDE = 2;

    /** El numero de la etiqueta gps longitude ref. */
    public static final int TAG_GPS_LONGITUDE_REF = 3;

    /** El numero de la etiqueta gps longitude. */
    public static final int TAG_GPS_LONGITUDE = 4;

    /** El numero de la etiqueta gps altitude ref. */
    public static final int TAG_GPS_ALTITUDE_REF = 5;

    /** El numero de la etiqueta gps altitude. */
    public static final int TAG_GPS_ALTITUDE = 6;

    /** El numero de la etiqueta gps time stamp. */
    public static final int TAG_GPS_TIME_STAMP = 7;

    /** El numero de la etiqueta gps satellites. */
    public static final int TAG_GPS_SATELLITES = 8;

    /** El numero de la etiqueta gps status. */
    public static final int TAG_GPS_STATUS = 9;

    /** El numero de la etiqueta gps measure mode. */
    public static final int TAG_GPS_MEASURE_MODE = 10;

    /** El numero de la etiqueta gps dop. */
    public static final int TAG_GPS_DOP = 11;

    /** El numero de la etiqueta gps speed ref. */
    public static final int TAG_GPS_SPEED_REF = 12;

    /** El numero de la etiqueta gps speed. */
    public static final int TAG_GPS_SPEED = 13;

    /** El numero de la etiqueta gps track ref. */
    public static final int TAG_GPS_TRACK_REF = 14;

    /** El numero de la etiqueta gps track. */
    public static final int TAG_GPS_TRACK = 15;

    /** El numero de la etiqueta gps img direction ref. */
    public static final int TAG_GPS_IMG_DIRECTION_REF = 16;

    /** El numero de la etiqueta gps img direction. */
    public static final int TAG_GPS_IMG_DIRECTION = 17;

    /** El numero de la etiqueta gps map datum. */
    public static final int TAG_GPS_MAP_DATUM = 18;

    /** El numero de la etiqueta gps dest latitude ref. */
    public static final int TAG_GPS_DEST_LATITUDE_REF = 19;

    /** El numero de la etiqueta gps dest latitude. */
    public static final int TAG_GPS_DEST_LATITUDE = 20;

    /** El numero de la etiqueta gps dest longitude ref. */
    public static final int TAG_GPS_DEST_LONGITUDE_REF = 21;

    /** El numero de la etiqueta gps dest longitude. */
    public static final int TAG_GPS_DEST_LONGITUDE = 22;

    /** El numero de la etiqueta gps dest bearing ref. */
    public static final int TAG_GPS_DEST_BEARING_REF = 23;

    /** El numero de la etiqueta gps dest bearing. */
    public static final int TAG_GPS_DEST_BEARING = 24;

    /** El numero de la etiqueta gps dest distance ref. */
    public static final int TAG_GPS_DEST_DISTANCE_REF = 25;

    /** El numero de la etiqueta gps dest distance. */
    public static final int TAG_GPS_DEST_DISTANCE = 26;

    /** El numero de la etiqueta gps processing method. */
    public static final int TAG_GPS_PROCESSING_METHOD = 27;

    /** El numero de la etiqueta gps area information. */
    public static final int TAG_GPS_AREA_INFORMATION = 28;

    /** El numero de la etiqueta gps date stamp. */
    public static final int TAG_GPS_DATE_STAMP = 29;

    /** El numero de la etiqueta gps differential. */
    public static final int TAG_GPS_DIFFERENTIAL = 30;

    /** Un valor de altitude. */
    public static final int ALTITUDE_REF_SEA_LEVEL = 0;

    /** Un valor de altitude. */
    public static final int ALTITUDE_REF_SEA_LEVEL_REFERENCE = 1;

    /** Un valor de differential. */
    public static final int DIFFERENTIAL_CORRECTION_NONE = 0;

    /** Un valor de differential. */
    public static final int DIFFERENTIAL_CORRECTION_APPLIED = 1;

    /** Un valor de gps. */
    public static final String GPS_VERSION_2_2 = "2200";

    /** Un valor de latitude. */
    public static final String LATITUDE_REF_NORTH = "N";

    /** Un valor de latitude. */
    public static final String LATITUDE_REF_SOUTH = "S";

    /** Un valor de longitude. */
    public static final String LONGITUDE_REF_EAST = "E";

    /** Un valor de longitude. */
    public static final String LONGITUDE_REF_WEST = "W";

    /** Un valor de status. */
    public static final String STATUS_MEASUREMENT_IN_PROGRESS = "A";

    /** Un valor de status. */
    public static final String STATUS_MEASUREMENT_INTEROPERABILITY = "V";

    /** Un valor de measure. */
    public static final String MEASURE_MODE_2D = "2";

    /** Un valor de measure. */
    public static final String MEASURE_MODE_3D = "3";

    /** Un valor de speed. */
    public static final String SPEED_REF_KILOMETERS_PER_HOUR = "K";

    /** Un valor de speed. */
    public static final String SPEED_REF_MILES_PER_HOUR = "M";

    /** Un valor de speed. */
    public static final String SPEED_REF_KNOTS = "N";

    /** Un valor de direction. */
    public static final String DIRECTION_REF_TRUE = "T";

    /** Un valor de direction. */
    public static final String DIRECTION_REF_MAGNETIC = "M";

    /** Un valor de dest. */
    public static final String DEST_DISTANCE_REF_KILOMETERS = "K";

    /** Un valor de dest. */
    public static final String DEST_DISTANCE_REF_MILES = "M";

    /** Un valor de dest. */
    public static final String DEST_DISTANCE_REF_KNOTS = "N";


    /** Se llega por {@link #getInstance}. */
    private ExifGPSTagSet() {
        super(tags());
    }

    /** El conjunto. Ver la nota de la clase. */
    public static synchronized ExifGPSTagSet getInstance() {
        if (theInstance == null) {
            theInstance = new ExifGPSTagSet();
        }
        return theInstance;
    }

    /** Las etiquetas de este conjunto. */
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

    /** {@code GPSAltitudeRef}, con los nombres de sus valores. */
    private static final class TagGPSAltitudeRef extends TIFFTag {

        TagGPSAltitudeRef() {
            super("GPSAltitudeRef", 5, 2, -1);
            addValueName(0, "Sea level");
            addValueName(1, "Sea level reference (negative value)");
        }
    }

    /** {@code GPSDifferential}, con los nombres de sus valores. */
    private static final class TagGPSDifferential extends TIFFTag {

        TagGPSDifferential() {
            super("GPSDifferential", 30, 8, -1);
            addValueName(0, "Measurement without differential correction");
            addValueName(1, "Differential correction applied");
        }
    }
}
