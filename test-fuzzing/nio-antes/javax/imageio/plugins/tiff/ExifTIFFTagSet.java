package javax.imageio.plugins.tiff;

import java.util.ArrayList;
import java.util.List;

/**
 * KajiLibrary's javax.imageio.plugins.tiff.ExifTIFFTagSet -- the Exif tags.
 *
 * <p>What the camera writes: exposure, aperture, sensitivity, focal length, flash, lens model, shot
 * date. It is the largest set after the baseline one.
 *
 * <p>It hangs from a directory pointed to by {@code ExifParentTIFFTagSet.TAG_EXIF_IFD_POINTER}, and
 * in turn points to the interoperability one.
 *
 * <p>It is a singleton: it is obtained with {@link #getInstance}. The tags and their named values
 * were transcribed from the JDK 25 and not by hand; a changed number produces a TIFF that other
 * programs read differently.
 */
public final class ExifTIFFTagSet extends TIFFTagSet {

    /** The only one, built the first time it is asked for. */
    private static ExifTIFFTagSet theInstance = null;

    /** The number of the gps info ifd pointer tag. */
    public static final int TAG_GPS_INFO_IFD_POINTER = 34853;

    /** The number of the interoperability ifd pointer tag. */
    public static final int TAG_INTEROPERABILITY_IFD_POINTER = 40965;

    /** The number of the exif version tag. */
    public static final int TAG_EXIF_VERSION = 36864;

    /** The number of the flashpix version tag. */
    public static final int TAG_FLASHPIX_VERSION = 40960;

    /** The number of the color space tag. */
    public static final int TAG_COLOR_SPACE = 40961;

    /** A value of {@link #TAG_COLOR_SPACE}. */
    public static final int COLOR_SPACE_SRGB = 1;

    /** A value of {@link #TAG_COLOR_SPACE}. */
    public static final int COLOR_SPACE_UNCALIBRATED = 65535;

    /** The number of the components configuration tag. */
    public static final int TAG_COMPONENTS_CONFIGURATION = 37121;

    /** A value of {@link #TAG_COMPONENTS_CONFIGURATION}. */
    public static final int COMPONENTS_CONFIGURATION_DOES_NOT_EXIST = 0;

    /** A value of {@link #TAG_COMPONENTS_CONFIGURATION}. */
    public static final int COMPONENTS_CONFIGURATION_Y = 1;

    /** A value of {@link #TAG_COMPONENTS_CONFIGURATION}. */
    public static final int COMPONENTS_CONFIGURATION_CB = 2;

    /** A value of {@link #TAG_COMPONENTS_CONFIGURATION}. */
    public static final int COMPONENTS_CONFIGURATION_CR = 3;

    /** A value of {@link #TAG_COMPONENTS_CONFIGURATION}. */
    public static final int COMPONENTS_CONFIGURATION_R = 4;

    /** A value of {@link #TAG_COMPONENTS_CONFIGURATION}. */
    public static final int COMPONENTS_CONFIGURATION_G = 5;

    /** A value of {@link #TAG_COMPONENTS_CONFIGURATION}. */
    public static final int COMPONENTS_CONFIGURATION_B = 6;

    /** The number of the compressed bits per pixel tag. */
    public static final int TAG_COMPRESSED_BITS_PER_PIXEL = 37122;

    /** The number of the pixel x dimension tag. */
    public static final int TAG_PIXEL_X_DIMENSION = 40962;

    /** The number of the pixel y dimension tag. */
    public static final int TAG_PIXEL_Y_DIMENSION = 40963;

    /** The number of the maker note tag. */
    public static final int TAG_MAKER_NOTE = 37500;

    /** The number of the marker note tag. */
    public static final int TAG_MARKER_NOTE = 37500;

    /** The number of the user comment tag. */
    public static final int TAG_USER_COMMENT = 37510;

    /** The number of the related sound file tag. */
    public static final int TAG_RELATED_SOUND_FILE = 40964;

    /** The number of the date time original tag. */
    public static final int TAG_DATE_TIME_ORIGINAL = 36867;

    /** The number of the date time digitized tag. */
    public static final int TAG_DATE_TIME_DIGITIZED = 36868;

    /** The number of the sub sec time tag. */
    public static final int TAG_SUB_SEC_TIME = 37520;

    /** The number of the sub sec time original tag. */
    public static final int TAG_SUB_SEC_TIME_ORIGINAL = 37521;

    /** The number of the sub sec time digitized tag. */
    public static final int TAG_SUB_SEC_TIME_DIGITIZED = 37522;

    /** The number of the exposure time tag. */
    public static final int TAG_EXPOSURE_TIME = 33434;

    /** The number of the f number tag. */
    public static final int TAG_F_NUMBER = 33437;

    /** The number of the exposure program tag. */
    public static final int TAG_EXPOSURE_PROGRAM = 34850;

    /** A value of {@link #TAG_EXPOSURE_PROGRAM}. */
    public static final int EXPOSURE_PROGRAM_NOT_DEFINED = 0;

    /** A value of {@link #TAG_EXPOSURE_PROGRAM}. */
    public static final int EXPOSURE_PROGRAM_MANUAL = 1;

    /** A value of {@link #TAG_EXPOSURE_PROGRAM}. */
    public static final int EXPOSURE_PROGRAM_NORMAL_PROGRAM = 2;

    /** A value of {@link #TAG_EXPOSURE_PROGRAM}. */
    public static final int EXPOSURE_PROGRAM_APERTURE_PRIORITY = 3;

    /** A value of {@link #TAG_EXPOSURE_PROGRAM}. */
    public static final int EXPOSURE_PROGRAM_SHUTTER_PRIORITY = 4;

    /** A value of {@link #TAG_EXPOSURE_PROGRAM}. */
    public static final int EXPOSURE_PROGRAM_CREATIVE_PROGRAM = 5;

    /** A value of {@link #TAG_EXPOSURE_PROGRAM}. */
    public static final int EXPOSURE_PROGRAM_ACTION_PROGRAM = 6;

    /** A value of {@link #TAG_EXPOSURE_PROGRAM}. */
    public static final int EXPOSURE_PROGRAM_PORTRAIT_MODE = 7;

    /** A value of {@link #TAG_EXPOSURE_PROGRAM}. */
    public static final int EXPOSURE_PROGRAM_LANDSCAPE_MODE = 8;

    /** A value of {@link #TAG_EXPOSURE_PROGRAM}. */
    public static final int EXPOSURE_PROGRAM_MAX_RESERVED = 255;

    /** The number of the spectral sensitivity tag. */
    public static final int TAG_SPECTRAL_SENSITIVITY = 34852;

    /** The number of the iso speed ratings tag. */
    public static final int TAG_ISO_SPEED_RATINGS = 34855;

    /** The number of the oecf tag. */
    public static final int TAG_OECF = 34856;

    /** The number of the shutter speed value tag. */
    public static final int TAG_SHUTTER_SPEED_VALUE = 37377;

    /** The number of the aperture value tag. */
    public static final int TAG_APERTURE_VALUE = 37378;

    /** The number of the brightness value tag. */
    public static final int TAG_BRIGHTNESS_VALUE = 37379;

    /** The number of the exposure bias value tag. */
    public static final int TAG_EXPOSURE_BIAS_VALUE = 37380;

    /** The number of the max aperture value tag. */
    public static final int TAG_MAX_APERTURE_VALUE = 37381;

    /** The number of the subject distance tag. */
    public static final int TAG_SUBJECT_DISTANCE = 37382;

    /** The number of the metering mode tag. */
    public static final int TAG_METERING_MODE = 37383;

    /** A value of {@link #TAG_METERING_MODE}. */
    public static final int METERING_MODE_UNKNOWN = 0;

    /** A value of {@link #TAG_METERING_MODE}. */
    public static final int METERING_MODE_AVERAGE = 1;

    /** A value of {@link #TAG_METERING_MODE}. */
    public static final int METERING_MODE_CENTER_WEIGHTED_AVERAGE = 2;

    /** A value of {@link #TAG_METERING_MODE}. */
    public static final int METERING_MODE_SPOT = 3;

    /** A value of {@link #TAG_METERING_MODE}. */
    public static final int METERING_MODE_MULTI_SPOT = 4;

    /** A value of {@link #TAG_METERING_MODE}. */
    public static final int METERING_MODE_PATTERN = 5;

    /** A value of {@link #TAG_METERING_MODE}. */
    public static final int METERING_MODE_PARTIAL = 6;

    /** A value of {@link #TAG_METERING_MODE}. */
    public static final int METERING_MODE_MIN_RESERVED = 7;

    /** A value of {@link #TAG_METERING_MODE}. */
    public static final int METERING_MODE_MAX_RESERVED = 254;

    /** A value of {@link #TAG_METERING_MODE}. */
    public static final int METERING_MODE_OTHER = 255;

    /** The number of the light source tag. */
    public static final int TAG_LIGHT_SOURCE = 37384;

    /** A value of {@link #TAG_LIGHT_SOURCE}. */
    public static final int LIGHT_SOURCE_UNKNOWN = 0;

    /** A value of {@link #TAG_LIGHT_SOURCE}. */
    public static final int LIGHT_SOURCE_DAYLIGHT = 1;

    /** A value of {@link #TAG_LIGHT_SOURCE}. */
    public static final int LIGHT_SOURCE_FLUORESCENT = 2;

    /** A value of {@link #TAG_LIGHT_SOURCE}. */
    public static final int LIGHT_SOURCE_TUNGSTEN = 3;

    /** A value of {@link #TAG_LIGHT_SOURCE}. */
    public static final int LIGHT_SOURCE_FLASH = 4;

    /** A value of {@link #TAG_LIGHT_SOURCE}. */
    public static final int LIGHT_SOURCE_FINE_WEATHER = 9;

    /** A value of {@link #TAG_LIGHT_SOURCE}. */
    public static final int LIGHT_SOURCE_CLOUDY_WEATHER = 10;

    /** A value of {@link #TAG_LIGHT_SOURCE}. */
    public static final int LIGHT_SOURCE_SHADE = 11;

    /** A value of {@link #TAG_LIGHT_SOURCE}. */
    public static final int LIGHT_SOURCE_DAYLIGHT_FLUORESCENT = 12;

    /** A value of {@link #TAG_LIGHT_SOURCE}. */
    public static final int LIGHT_SOURCE_DAY_WHITE_FLUORESCENT = 13;

    /** A value of {@link #TAG_LIGHT_SOURCE}. */
    public static final int LIGHT_SOURCE_COOL_WHITE_FLUORESCENT = 14;

    /** A value of {@link #TAG_LIGHT_SOURCE}. */
    public static final int LIGHT_SOURCE_WHITE_FLUORESCENT = 15;

    /** A value of {@link #TAG_LIGHT_SOURCE}. */
    public static final int LIGHT_SOURCE_STANDARD_LIGHT_A = 17;

    /** A value of {@link #TAG_LIGHT_SOURCE}. */
    public static final int LIGHT_SOURCE_STANDARD_LIGHT_B = 18;

    /** A value of {@link #TAG_LIGHT_SOURCE}. */
    public static final int LIGHT_SOURCE_STANDARD_LIGHT_C = 19;

    /** A value of {@link #TAG_LIGHT_SOURCE}. */
    public static final int LIGHT_SOURCE_D55 = 20;

    /** A value of {@link #TAG_LIGHT_SOURCE}. */
    public static final int LIGHT_SOURCE_D65 = 21;

    /** A value of {@link #TAG_LIGHT_SOURCE}. */
    public static final int LIGHT_SOURCE_D75 = 22;

    /** A value of {@link #TAG_LIGHT_SOURCE}. */
    public static final int LIGHT_SOURCE_D50 = 23;

    /** A value of {@link #TAG_LIGHT_SOURCE}. */
    public static final int LIGHT_SOURCE_ISO_STUDIO_TUNGSTEN = 24;

    /** A value of {@link #TAG_LIGHT_SOURCE}. */
    public static final int LIGHT_SOURCE_OTHER = 255;

    /** The number of the flash tag. */
    public static final int TAG_FLASH = 37385;

    /** A value of {@link #TAG_FLASH}. */
    public static final int FLASH_DID_NOT_FIRE = 0;

    /** A value of {@link #TAG_FLASH}. */
    public static final int FLASH_FIRED = 1;

    /** A value of {@link #TAG_FLASH}. */
    public static final int FLASH_STROBE_RETURN_LIGHT_NOT_DETECTED = 5;

    /** A value of {@link #TAG_FLASH}. */
    public static final int FLASH_STROBE_RETURN_LIGHT_DETECTED = 7;

    /** A value of {@link #TAG_FLASH}. */
    public static final int FLASH_MASK_FIRED = 1;

    /** A value of {@link #TAG_FLASH}. */
    public static final int FLASH_MASK_RETURN_NOT_DETECTED = 4;

    /** A value of {@link #TAG_FLASH}. */
    public static final int FLASH_MASK_RETURN_DETECTED = 6;

    /** A value of {@link #TAG_FLASH}. */
    public static final int FLASH_MASK_MODE_FLASH_FIRING = 8;

    /** A value of {@link #TAG_FLASH}. */
    public static final int FLASH_MASK_MODE_FLASH_SUPPRESSION = 16;

    /** A value of {@link #TAG_FLASH}. */
    public static final int FLASH_MASK_MODE_AUTO = 24;

    /** A value of {@link #TAG_FLASH}. */
    public static final int FLASH_MASK_FUNCTION_NOT_PRESENT = 32;

    /** A value of {@link #TAG_FLASH}. */
    public static final int FLASH_MASK_RED_EYE_REDUCTION = 64;

    /** The number of the focal length tag. */
    public static final int TAG_FOCAL_LENGTH = 37386;

    /** The number of the subject area tag. */
    public static final int TAG_SUBJECT_AREA = 37396;

    /** The number of the flash energy tag. */
    public static final int TAG_FLASH_ENERGY = 41483;

    /** The number of the spatial frequency response tag. */
    public static final int TAG_SPATIAL_FREQUENCY_RESPONSE = 41484;

    /** The number of the focal plane x resolution tag. */
    public static final int TAG_FOCAL_PLANE_X_RESOLUTION = 41486;

    /** The number of the focal plane y resolution tag. */
    public static final int TAG_FOCAL_PLANE_Y_RESOLUTION = 41487;

    /** The number of the focal plane resolution unit tag. */
    public static final int TAG_FOCAL_PLANE_RESOLUTION_UNIT = 41488;

    /** A value of {@link #TAG_FOCAL_PLANE_RESOLUTION_UNIT}. */
    public static final int FOCAL_PLANE_RESOLUTION_UNIT_NONE = 1;

    /** A value of {@link #TAG_FOCAL_PLANE_RESOLUTION_UNIT}. */
    public static final int FOCAL_PLANE_RESOLUTION_UNIT_INCH = 2;

    /** A value of {@link #TAG_FOCAL_PLANE_RESOLUTION_UNIT}. */
    public static final int FOCAL_PLANE_RESOLUTION_UNIT_CENTIMETER = 3;

    /** The number of the subject location tag. */
    public static final int TAG_SUBJECT_LOCATION = 41492;

    /** The number of the exposure index tag. */
    public static final int TAG_EXPOSURE_INDEX = 41493;

    /** The number of the sensing method tag. */
    public static final int TAG_SENSING_METHOD = 41495;

    /** A value of {@link #TAG_SENSING_METHOD}. */
    public static final int SENSING_METHOD_NOT_DEFINED = 1;

    /** A value of {@link #TAG_SENSING_METHOD}. */
    public static final int SENSING_METHOD_ONE_CHIP_COLOR_AREA_SENSOR = 2;

    /** A value of {@link #TAG_SENSING_METHOD}. */
    public static final int SENSING_METHOD_TWO_CHIP_COLOR_AREA_SENSOR = 3;

    /** A value of {@link #TAG_SENSING_METHOD}. */
    public static final int SENSING_METHOD_THREE_CHIP_COLOR_AREA_SENSOR = 4;

    /** A value of {@link #TAG_SENSING_METHOD}. */
    public static final int SENSING_METHOD_COLOR_SEQUENTIAL_AREA_SENSOR = 5;

    /** A value of {@link #TAG_SENSING_METHOD}. */
    public static final int SENSING_METHOD_TRILINEAR_SENSOR = 7;

    /** A value of {@link #TAG_SENSING_METHOD}. */
    public static final int SENSING_METHOD_COLOR_SEQUENTIAL_LINEAR_SENSOR = 8;

    /** The number of the file source tag. */
    public static final int TAG_FILE_SOURCE = 41728;

    /** A value of {@link #TAG_FILE_SOURCE}. */
    public static final int FILE_SOURCE_DSC = 3;

    /** The number of the scene type tag. */
    public static final int TAG_SCENE_TYPE = 41729;

    /** A value of {@link #TAG_SCENE_TYPE}. */
    public static final int SCENE_TYPE_DSC = 1;

    /** The number of the cfa pattern tag. */
    public static final int TAG_CFA_PATTERN = 41730;

    /** The number of the custom rendered tag. */
    public static final int TAG_CUSTOM_RENDERED = 41985;

    /** A value of {@link #TAG_CUSTOM_RENDERED}. */
    public static final int CUSTOM_RENDERED_NORMAL = 0;

    /** A value of {@link #TAG_CUSTOM_RENDERED}. */
    public static final int CUSTOM_RENDERED_CUSTOM = 1;

    /** The number of the exposure mode tag. */
    public static final int TAG_EXPOSURE_MODE = 41986;

    /** A value of {@link #TAG_EXPOSURE_MODE}. */
    public static final int EXPOSURE_MODE_AUTO_EXPOSURE = 0;

    /** A value of {@link #TAG_EXPOSURE_MODE}. */
    public static final int EXPOSURE_MODE_MANUAL_EXPOSURE = 1;

    /** A value of {@link #TAG_EXPOSURE_MODE}. */
    public static final int EXPOSURE_MODE_AUTO_BRACKET = 2;

    /** The number of the white balance tag. */
    public static final int TAG_WHITE_BALANCE = 41987;

    /** A value of {@link #TAG_WHITE_BALANCE}. */
    public static final int WHITE_BALANCE_AUTO = 0;

    /** A value of {@link #TAG_WHITE_BALANCE}. */
    public static final int WHITE_BALANCE_MANUAL = 1;

    /** The number of the digital zoom ratio tag. */
    public static final int TAG_DIGITAL_ZOOM_RATIO = 41988;

    /** The number of the focal length in 35mm film tag. */
    public static final int TAG_FOCAL_LENGTH_IN_35MM_FILM = 41989;

    /** The number of the scene capture type tag. */
    public static final int TAG_SCENE_CAPTURE_TYPE = 41990;

    /** A value of {@link #TAG_SCENE_CAPTURE_TYPE}. */
    public static final int SCENE_CAPTURE_TYPE_STANDARD = 0;

    /** A value of {@link #TAG_SCENE_CAPTURE_TYPE}. */
    public static final int SCENE_CAPTURE_TYPE_LANDSCAPE = 1;

    /** A value of {@link #TAG_SCENE_CAPTURE_TYPE}. */
    public static final int SCENE_CAPTURE_TYPE_PORTRAIT = 2;

    /** A value of {@link #TAG_SCENE_CAPTURE_TYPE}. */
    public static final int SCENE_CAPTURE_TYPE_NIGHT_SCENE = 3;

    /** The number of the gain control tag. */
    public static final int TAG_GAIN_CONTROL = 41991;

    /** A value of {@link #TAG_GAIN_CONTROL}. */
    public static final int GAIN_CONTROL_NONE = 0;

    /** A value of {@link #TAG_GAIN_CONTROL}. */
    public static final int GAIN_CONTROL_LOW_GAIN_UP = 1;

    /** A value of {@link #TAG_GAIN_CONTROL}. */
    public static final int GAIN_CONTROL_HIGH_GAIN_UP = 2;

    /** A value of {@link #TAG_GAIN_CONTROL}. */
    public static final int GAIN_CONTROL_LOW_GAIN_DOWN = 3;

    /** A value of {@link #TAG_GAIN_CONTROL}. */
    public static final int GAIN_CONTROL_HIGH_GAIN_DOWN = 4;

    /** The number of the contrast tag. */
    public static final int TAG_CONTRAST = 41992;

    /** A value of {@link #TAG_CONTRAST}. */
    public static final int CONTRAST_NORMAL = 0;

    /** A value of {@link #TAG_CONTRAST}. */
    public static final int CONTRAST_SOFT = 1;

    /** A value of {@link #TAG_CONTRAST}. */
    public static final int CONTRAST_HARD = 2;

    /** The number of the saturation tag. */
    public static final int TAG_SATURATION = 41993;

    /** A value of {@link #TAG_SATURATION}. */
    public static final int SATURATION_NORMAL = 0;

    /** A value of {@link #TAG_SATURATION}. */
    public static final int SATURATION_LOW = 1;

    /** A value of {@link #TAG_SATURATION}. */
    public static final int SATURATION_HIGH = 2;

    /** The number of the sharpness tag. */
    public static final int TAG_SHARPNESS = 41994;

    /** A value of {@link #TAG_SHARPNESS}. */
    public static final int SHARPNESS_NORMAL = 0;

    /** A value of {@link #TAG_SHARPNESS}. */
    public static final int SHARPNESS_SOFT = 1;

    /** A value of {@link #TAG_SHARPNESS}. */
    public static final int SHARPNESS_HARD = 2;

    /** The number of the device setting description tag. */
    public static final int TAG_DEVICE_SETTING_DESCRIPTION = 41995;

    /** The number of the subject distance range tag. */
    public static final int TAG_SUBJECT_DISTANCE_RANGE = 41996;

    /** A value of {@link #TAG_SUBJECT_DISTANCE_RANGE}. */
    public static final int SUBJECT_DISTANCE_RANGE_UNKNOWN = 0;

    /** A value of {@link #TAG_SUBJECT_DISTANCE_RANGE}. */
    public static final int SUBJECT_DISTANCE_RANGE_MACRO = 1;

    /** A value of {@link #TAG_SUBJECT_DISTANCE_RANGE}. */
    public static final int SUBJECT_DISTANCE_RANGE_CLOSE_VIEW = 2;

    /** A value of {@link #TAG_SUBJECT_DISTANCE_RANGE}. */
    public static final int SUBJECT_DISTANCE_RANGE_DISTANT_VIEW = 3;

    /** The number of the image unique id tag. */
    public static final int TAG_IMAGE_UNIQUE_ID = 42016;

    /** A value of {@link #TAG_EXIF_VERSION}. */
    public static final String EXIF_VERSION_2_1 = "0210";

    /** A value of {@link #TAG_EXIF_VERSION}. */
    public static final String EXIF_VERSION_2_2 = "0220";


    /** Reached through {@link #getInstance}. */
    private ExifTIFFTagSet() {
        super(tags());
    }

    /** The set. See the class note. */
    public static synchronized ExifTIFFTagSet getInstance() {
        if (theInstance == null) {
            theInstance = new ExifTIFFTagSet();
        }
        return theInstance;
    }

    /** The tags of this set. */
    private static List<TIFFTag> tags() {
        List<TIFFTag> tags = new ArrayList<TIFFTag>();
        tags.add(new TIFFTag("ExposureTime", 33434, 32, 1));
        tags.add(new TIFFTag("FNumber", 33437, 32, 1));
        tags.add(new TagExposureProgram());
        tags.add(new TIFFTag("SpectralSensitivity", 34852, 4, -1));
        tags.add(new TIFFTag("ISOSpeedRatings", 34855, 8, -1));
        tags.add(new TIFFTag("OECF", 34856, 128, -1));
        tags.add(new TIFFTag("ExifVersion", 36864, 128, 4));
        tags.add(new TIFFTag("DateTimeOriginal", 36867, 4, 20));
        tags.add(new TIFFTag("DateTimeDigitized", 36868, 4, 20));
        tags.add(new TagComponentsConfiguration());
        tags.add(new TIFFTag("CompressedBitsPerPixel", 37122, 32, 1));
        tags.add(new TIFFTag("ShutterSpeedValue", 37377, 1024, 1));
        tags.add(new TIFFTag("ApertureValue", 37378, 32, 1));
        tags.add(new TIFFTag("BrightnessValue", 37379, 1024, 1));
        tags.add(new TIFFTag("ExposureBiasValue", 37380, 1024, 1));
        tags.add(new TIFFTag("MaxApertureValue", 37381, 32, 1));
        tags.add(new TIFFTag("SubjectDistance", 37382, 32, 1));
        tags.add(new TagMeteringMode());
        tags.add(new TagLightSource());
        tags.add(new TagFlash());
        tags.add(new TIFFTag("FocalLength", 37386, 32, 1));
        tags.add(new TIFFTag("SubjectArea", 37396, 8, -1));
        tags.add(new TIFFTag("MakerNote", 37500, 128, -1));
        tags.add(new TIFFTag("UserComment", 37510, 128, -1));
        tags.add(new TIFFTag("SubSecTime", 37520, 4, -1));
        tags.add(new TIFFTag("SubSecTimeOriginal", 37521, 4, -1));
        tags.add(new TIFFTag("SubSecTimeDigitized", 37522, 4, -1));
        tags.add(new TIFFTag("FlashPixVersion", 40960, 128, 4));
        tags.add(new TagColorSpace());
        tags.add(new TIFFTag("PixelXDimension", 40962, 24, 1));
        tags.add(new TIFFTag("PixelYDimension", 40963, 24, 1));
        tags.add(new TIFFTag("RelatedSoundFile", 40964, 4, 13));
        tags.add(new TIFFTag("InteroperabilityIFD", 40965, ExifInteroperabilityTagSet.getInstance()));
        tags.add(new TIFFTag("FlashEnergy", 41483, 32, 1));
        tags.add(new TIFFTag("SpatialFrequencyResponse", 41484, 128, -1));
        tags.add(new TIFFTag("FocalPlaneXResolution", 41486, 32, 1));
        tags.add(new TIFFTag("FocalPlaneYResolution", 41487, 32, 1));
        tags.add(new TagFocalPlaneResolutionUnit());
        tags.add(new TIFFTag("SubjectLocation", 41492, 8, 2));
        tags.add(new TIFFTag("ExposureIndex", 41493, 32, 1));
        tags.add(new TagSensingMethod());
        tags.add(new TagFileSource());
        tags.add(new TagSceneType());
        tags.add(new TIFFTag("CFAPattern", 41730, 128, -1));
        tags.add(new TagCustomRendered());
        tags.add(new TagExposureMode());
        tags.add(new TagWhiteBalance());
        tags.add(new TIFFTag("DigitalZoomRatio", 41988, 32, 1));
        tags.add(new TIFFTag("FocalLengthIn35mmFilm", 41989, 8, 1));
        tags.add(new TagSceneCaptureType());
        tags.add(new TagGainControl());
        tags.add(new TagContrast());
        tags.add(new TagSaturation());
        tags.add(new TagSharpness());
        tags.add(new TIFFTag("DeviceSettingDescription", 41995, 128, -1));
        tags.add(new TagSubjectDistanceRange());
        tags.add(new TIFFTag("ImageUniqueID", 42016, 4, 33));
        return tags;
    }

    /** {@code ExposureProgram}, with the names of its values. */
    private static final class TagExposureProgram extends TIFFTag {

        TagExposureProgram() {
            super("ExposureProgram", 34850, 8, 1);
            addValueName(0, "Not Defined");
            addValueName(1, "Manual");
            addValueName(2, "Normal Program");
            addValueName(3, "Aperture Priority");
            addValueName(4, "Shutter Priority");
            addValueName(5, "Creative Program");
            addValueName(6, "Action Program");
            addValueName(7, "Portrait Mode");
            addValueName(8, "Landscape Mode");
        }
    }

    /** {@code ComponentsConfiguration}, with the names of its values. */
    private static final class TagComponentsConfiguration extends TIFFTag {

        TagComponentsConfiguration() {
            super("ComponentsConfiguration", 37121, 128, 4);
            addValueName(0, "DoesNotExist");
            addValueName(1, "Y");
            addValueName(2, "Cb");
            addValueName(3, "Cr");
            addValueName(4, "R");
            addValueName(5, "G");
            addValueName(6, "B");
        }
    }

    /** {@code MeteringMode}, with the names of its values. */
    private static final class TagMeteringMode extends TIFFTag {

        TagMeteringMode() {
            super("MeteringMode", 37383, 8, 1);
            addValueName(0, "Unknown");
            addValueName(1, "Average");
            addValueName(2, "CenterWeightedAverage");
            addValueName(3, "Spot");
            addValueName(4, "MultiSpot");
            addValueName(5, "Pattern");
            addValueName(6, "Partial");
            addValueName(255, "Other");
        }
    }

    /** {@code LightSource}, with the names of its values. */
    private static final class TagLightSource extends TIFFTag {

        TagLightSource() {
            super("LightSource", 37384, 8, 1);
            addValueName(0, "Unknown");
            addValueName(1, "Daylight");
            addValueName(2, "Fluorescent");
            addValueName(3, "Tungsten");
            addValueName(17, "Standard Light A");
            addValueName(18, "Standard Light B");
            addValueName(19, "Standard Light C");
            addValueName(20, "D55");
            addValueName(21, "D65");
            addValueName(22, "D75");
            addValueName(255, "Other");
        }
    }

    /** {@code Flash}, with the names of its values. */
    private static final class TagFlash extends TIFFTag {

        TagFlash() {
            super("Flash", 37385, 8, 1);
            addValueName(0, "Flash Did Not Fire");
            addValueName(1, "Flash Fired");
            addValueName(5, "Strobe Return Light Not Detected");
            addValueName(7, "Strobe Return Light Detected");
        }
    }

    /** {@code ColorSpace}, with the names of its values. */
    private static final class TagColorSpace extends TIFFTag {

        TagColorSpace() {
            super("ColorSpace", 40961, 8, 1);
            addValueName(1, "sRGB");
            addValueName(65535, "Uncalibrated");
        }
    }

    /** {@code FocalPlaneResolutionUnit}, with the names of its values. */
    private static final class TagFocalPlaneResolutionUnit extends TIFFTag {

        TagFocalPlaneResolutionUnit() {
            super("FocalPlaneResolutionUnit", 41488, 8, 1);
            addValueName(1, "None");
            addValueName(2, "Inch");
            addValueName(3, "Centimeter");
        }
    }

    /** {@code SensingMethod}, with the names of its values. */
    private static final class TagSensingMethod extends TIFFTag {

        TagSensingMethod() {
            super("SensingMethod", 41495, 8, 1);
            addValueName(1, "Not Defined");
            addValueName(2, "One-chip color area sensor");
            addValueName(3, "Two-chip color area sensor");
            addValueName(4, "Three-chip color area sensor");
            addValueName(5, "Color sequential area sensor");
            addValueName(7, "Trilinear sensor");
            addValueName(8, "Color sequential linear sensor");
        }
    }

    /** {@code FileSource}, with the names of its values. */
    private static final class TagFileSource extends TIFFTag {

        TagFileSource() {
            super("FileSource", 41728, 128, 1);
            addValueName(3, "DSC");
        }
    }

    /** {@code SceneType}, with the names of its values. */
    private static final class TagSceneType extends TIFFTag {

        TagSceneType() {
            super("SceneType", 41729, 128, 1);
            addValueName(1, "A directly photographed image");
        }
    }

    /** {@code CustomRendered}, with the names of its values. */
    private static final class TagCustomRendered extends TIFFTag {

        TagCustomRendered() {
            super("CustomRendered", 41985, 8, 1);
            addValueName(0, "Normal process");
            addValueName(1, "Custom process");
        }
    }

    /** {@code ExposureMode}, with the names of its values. */
    private static final class TagExposureMode extends TIFFTag {

        TagExposureMode() {
            super("ExposureMode", 41986, 8, 1);
            addValueName(0, "Auto exposure");
            addValueName(1, "Manual exposure");
            addValueName(2, "Auto bracket");
        }
    }

    /** {@code WhiteBalance}, with the names of its values. */
    private static final class TagWhiteBalance extends TIFFTag {

        TagWhiteBalance() {
            super("WhiteBalance", 41987, 8, 1);
            addValueName(0, "Auto white balance");
            addValueName(1, "Manual white balance");
        }
    }

    /** {@code SceneCaptureType}, with the names of its values. */
    private static final class TagSceneCaptureType extends TIFFTag {

        TagSceneCaptureType() {
            super("SceneCaptureType", 41990, 8, 1);
            addValueName(0, "Standard");
            addValueName(1, "Landscape");
            addValueName(2, "Portrait");
            addValueName(3, "Night scene");
        }
    }

    /** {@code GainControl}, with the names of its values. */
    private static final class TagGainControl extends TIFFTag {

        TagGainControl() {
            super("GainControl", 41991, 8, 1);
            addValueName(0, "None");
            addValueName(1, "Low gain up");
            addValueName(2, "High gain up");
            addValueName(3, "Low gain down");
            addValueName(4, "High gain down");
        }
    }

    /** {@code Contrast}, with the names of its values. */
    private static final class TagContrast extends TIFFTag {

        TagContrast() {
            super("Contrast", 41992, 8, 1);
            addValueName(0, "Normal");
            addValueName(1, "Soft");
            addValueName(2, "Hard");
        }
    }

    /** {@code Saturation}, with the names of its values. */
    private static final class TagSaturation extends TIFFTag {

        TagSaturation() {
            super("Saturation", 41993, 8, 1);
            addValueName(0, "Normal");
            addValueName(1, "Low saturation");
            addValueName(2, "High saturation");
        }
    }

    /** {@code Sharpness}, with the names of its values. */
    private static final class TagSharpness extends TIFFTag {

        TagSharpness() {
            super("Sharpness", 41994, 8, 1);
            addValueName(0, "Normal");
            addValueName(1, "Soft");
            addValueName(2, "Hard");
        }
    }

    /** {@code SubjectDistanceRange}, with the names of its values. */
    private static final class TagSubjectDistanceRange extends TIFFTag {

        TagSubjectDistanceRange() {
            super("SubjectDistanceRange", 41996, 8, 1);
            addValueName(0, "unknown");
            addValueName(1, "Macro");
            addValueName(2, "Close view");
            addValueName(3, "Distant view");
        }
    }
}
