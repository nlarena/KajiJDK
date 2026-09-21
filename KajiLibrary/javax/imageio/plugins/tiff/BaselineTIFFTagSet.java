package javax.imageio.plugins.tiff;

import java.util.ArrayList;
import java.util.List;

/**
 * KajiLibrary's javax.imageio.plugins.tiff.BaselineTIFFTagSet -- the baseline TIFF 6.0 tags.
 *
 * <p>The core of the format: size, resolution, compression, strip or tile layout, palette, and the
 * description fields. Every TIFF uses them.
 *
 * <p>The constants come in two families, worth telling apart: the {@code TAG_} ones are <b>tag
 * numbers</b>, and the rest are <b>values</b> certain tags can take --for example
 * {@code COMPRESSION_LZW}, which is a value of {@code TAG_COMPRESSION} and not a tag.
 *
 * <p>It is a singleton: it is obtained with {@link #getInstance}. The tags and their named values
 * were transcribed from the JDK 25 and not by hand; a changed number produces a TIFF that other
 * programs read differently.
 */
public final class BaselineTIFFTagSet extends TIFFTagSet {

    /** The only one, built the first time it is asked for. */
    private static BaselineTIFFTagSet theInstance = null;

    /** The number of the new subfile type tag. */
    public static final int TAG_NEW_SUBFILE_TYPE = 254;

    /** A value of {@link #TAG_NEW_SUBFILE_TYPE}. */
    public static final int NEW_SUBFILE_TYPE_REDUCED_RESOLUTION = 1;

    /** A value of {@link #TAG_NEW_SUBFILE_TYPE}. */
    public static final int NEW_SUBFILE_TYPE_SINGLE_PAGE = 2;

    /** A value of {@link #TAG_NEW_SUBFILE_TYPE}. */
    public static final int NEW_SUBFILE_TYPE_TRANSPARENCY = 4;

    /** The number of the subfile type tag. */
    public static final int TAG_SUBFILE_TYPE = 255;

    /** A value of {@link #TAG_SUBFILE_TYPE}. */
    public static final int SUBFILE_TYPE_FULL_RESOLUTION = 1;

    /** A value of {@link #TAG_SUBFILE_TYPE}. */
    public static final int SUBFILE_TYPE_REDUCED_RESOLUTION = 2;

    /** A value of {@link #TAG_SUBFILE_TYPE}. */
    public static final int SUBFILE_TYPE_SINGLE_PAGE = 3;

    /** The number of the image width tag. */
    public static final int TAG_IMAGE_WIDTH = 256;

    /** The number of the image length tag. */
    public static final int TAG_IMAGE_LENGTH = 257;

    /** The number of the bits per sample tag. */
    public static final int TAG_BITS_PER_SAMPLE = 258;

    /** The number of the compression tag. */
    public static final int TAG_COMPRESSION = 259;

    /** A value of {@link #TAG_COMPRESSION}. */
    public static final int COMPRESSION_NONE = 1;

    /** A value of {@link #TAG_COMPRESSION}. */
    public static final int COMPRESSION_CCITT_RLE = 2;

    /** A value of {@link #TAG_COMPRESSION}. */
    public static final int COMPRESSION_CCITT_T_4 = 3;

    /** A value of {@link #TAG_COMPRESSION}. */
    public static final int COMPRESSION_CCITT_T_6 = 4;

    /** A value of {@link #TAG_COMPRESSION}. */
    public static final int COMPRESSION_LZW = 5;

    /** A value of {@link #TAG_COMPRESSION}. */
    public static final int COMPRESSION_OLD_JPEG = 6;

    /** A value of {@link #TAG_COMPRESSION}. */
    public static final int COMPRESSION_JPEG = 7;

    /** A value of {@link #TAG_COMPRESSION}. */
    public static final int COMPRESSION_ZLIB = 8;

    /** A value of {@link #TAG_COMPRESSION}. */
    public static final int COMPRESSION_PACKBITS = 32773;

    /** A value of {@link #TAG_COMPRESSION}. */
    public static final int COMPRESSION_DEFLATE = 32946;

    /** The number of the photometric interpretation tag. */
    public static final int TAG_PHOTOMETRIC_INTERPRETATION = 262;

    /** A value of {@link #TAG_PHOTOMETRIC_INTERPRETATION}. */
    public static final int PHOTOMETRIC_INTERPRETATION_WHITE_IS_ZERO = 0;

    /** A value of {@link #TAG_PHOTOMETRIC_INTERPRETATION}. */
    public static final int PHOTOMETRIC_INTERPRETATION_BLACK_IS_ZERO = 1;

    /** A value of {@link #TAG_PHOTOMETRIC_INTERPRETATION}. */
    public static final int PHOTOMETRIC_INTERPRETATION_RGB = 2;

    /** A value of {@link #TAG_PHOTOMETRIC_INTERPRETATION}. */
    public static final int PHOTOMETRIC_INTERPRETATION_PALETTE_COLOR = 3;

    /** A value of {@link #TAG_PHOTOMETRIC_INTERPRETATION}. */
    public static final int PHOTOMETRIC_INTERPRETATION_TRANSPARENCY_MASK = 4;

    /** A value of {@link #TAG_PHOTOMETRIC_INTERPRETATION}. */
    public static final int PHOTOMETRIC_INTERPRETATION_CMYK = 5;

    /** A value of {@link #TAG_PHOTOMETRIC_INTERPRETATION}. */
    public static final int PHOTOMETRIC_INTERPRETATION_Y_CB_CR = 6;

    /** A value of {@link #TAG_PHOTOMETRIC_INTERPRETATION}. */
    public static final int PHOTOMETRIC_INTERPRETATION_CIELAB = 8;

    /** A value of {@link #TAG_PHOTOMETRIC_INTERPRETATION}. */
    public static final int PHOTOMETRIC_INTERPRETATION_ICCLAB = 9;

    /** The number of the threshholding tag. */
    public static final int TAG_THRESHHOLDING = 263;

    /** A value of {@link #TAG_THRESHHOLDING}. */
    public static final int THRESHHOLDING_NONE = 1;

    /** A value of {@link #TAG_THRESHHOLDING}. */
    public static final int THRESHHOLDING_ORDERED_DITHER = 2;

    /** A value of {@link #TAG_THRESHHOLDING}. */
    public static final int THRESHHOLDING_RANDOMIZED_DITHER = 3;

    /** The number of the cell width tag. */
    public static final int TAG_CELL_WIDTH = 264;

    /** The number of the cell length tag. */
    public static final int TAG_CELL_LENGTH = 265;

    /** The number of the fill order tag. */
    public static final int TAG_FILL_ORDER = 266;

    /** A value of {@link #TAG_FILL_ORDER}. */
    public static final int FILL_ORDER_LEFT_TO_RIGHT = 1;

    /** A value of {@link #TAG_FILL_ORDER}. */
    public static final int FILL_ORDER_RIGHT_TO_LEFT = 2;

    /** The number of the document name tag. */
    public static final int TAG_DOCUMENT_NAME = 269;

    /** The number of the image description tag. */
    public static final int TAG_IMAGE_DESCRIPTION = 270;

    /** The number of the make tag. */
    public static final int TAG_MAKE = 271;

    /** The number of the model tag. */
    public static final int TAG_MODEL = 272;

    /** The number of the strip offsets tag. */
    public static final int TAG_STRIP_OFFSETS = 273;

    /** The number of the orientation tag. */
    public static final int TAG_ORIENTATION = 274;

    /** A value of {@link #TAG_ORIENTATION}. */
    public static final int ORIENTATION_ROW_0_TOP_COLUMN_0_LEFT = 1;

    /** A value of {@link #TAG_ORIENTATION}. */
    public static final int ORIENTATION_ROW_0_TOP_COLUMN_0_RIGHT = 2;

    /** A value of {@link #TAG_ORIENTATION}. */
    public static final int ORIENTATION_ROW_0_BOTTOM_COLUMN_0_RIGHT = 3;

    /** A value of {@link #TAG_ORIENTATION}. */
    public static final int ORIENTATION_ROW_0_BOTTOM_COLUMN_0_LEFT = 4;

    /** A value of {@link #TAG_ORIENTATION}. */
    public static final int ORIENTATION_ROW_0_LEFT_COLUMN_0_TOP = 5;

    /** A value of {@link #TAG_ORIENTATION}. */
    public static final int ORIENTATION_ROW_0_RIGHT_COLUMN_0_TOP = 6;

    /** A value of {@link #TAG_ORIENTATION}. */
    public static final int ORIENTATION_ROW_0_RIGHT_COLUMN_0_BOTTOM = 7;

    /** A value of {@link #TAG_ORIENTATION}. */
    public static final int ORIENTATION_ROW_0_LEFT_COLUMN_0_BOTTOM = 8;

    /** The number of the samples per pixel tag. */
    public static final int TAG_SAMPLES_PER_PIXEL = 277;

    /** The number of the rows per strip tag. */
    public static final int TAG_ROWS_PER_STRIP = 278;

    /** The number of the strip byte counts tag. */
    public static final int TAG_STRIP_BYTE_COUNTS = 279;

    /** The number of the min sample value tag. */
    public static final int TAG_MIN_SAMPLE_VALUE = 280;

    /** The number of the max sample value tag. */
    public static final int TAG_MAX_SAMPLE_VALUE = 281;

    /** The number of the x resolution tag. */
    public static final int TAG_X_RESOLUTION = 282;

    /** The number of the y resolution tag. */
    public static final int TAG_Y_RESOLUTION = 283;

    /** The number of the planar configuration tag. */
    public static final int TAG_PLANAR_CONFIGURATION = 284;

    /** A value of {@link #TAG_PLANAR_CONFIGURATION}. */
    public static final int PLANAR_CONFIGURATION_CHUNKY = 1;

    /** A value of {@link #TAG_PLANAR_CONFIGURATION}. */
    public static final int PLANAR_CONFIGURATION_PLANAR = 2;

    /** The number of the page name tag. */
    public static final int TAG_PAGE_NAME = 285;

    /** The number of the x position tag. */
    public static final int TAG_X_POSITION = 286;

    /** The number of the y position tag. */
    public static final int TAG_Y_POSITION = 287;

    /** The number of the free offsets tag. */
    public static final int TAG_FREE_OFFSETS = 288;

    /** The number of the free byte counts tag. */
    public static final int TAG_FREE_BYTE_COUNTS = 289;

    /** The number of the gray response unit tag. */
    public static final int TAG_GRAY_RESPONSE_UNIT = 290;

    /** A value of {@link #TAG_GRAY_RESPONSE_UNIT}. */
    public static final int GRAY_RESPONSE_UNIT_TENTHS = 1;

    /** A value of {@link #TAG_GRAY_RESPONSE_UNIT}. */
    public static final int GRAY_RESPONSE_UNIT_HUNDREDTHS = 2;

    /** A value of {@link #TAG_GRAY_RESPONSE_UNIT}. */
    public static final int GRAY_RESPONSE_UNIT_THOUSANDTHS = 3;

    /** A value of {@link #TAG_GRAY_RESPONSE_UNIT}. */
    public static final int GRAY_RESPONSE_UNIT_TEN_THOUSANDTHS = 4;

    /** A value of {@link #TAG_GRAY_RESPONSE_UNIT}. */
    public static final int GRAY_RESPONSE_UNIT_HUNDRED_THOUSANDTHS = 5;

    /** The number of the gray response curve tag. */
    public static final int TAG_GRAY_RESPONSE_CURVE = 291;

    /** The number of the t4 options tag. */
    public static final int TAG_T4_OPTIONS = 292;

    /** A value of {@link #TAG_T4_OPTIONS}. */
    public static final int T4_OPTIONS_2D_CODING = 1;

    /** A value of {@link #TAG_T4_OPTIONS}. */
    public static final int T4_OPTIONS_UNCOMPRESSED = 2;

    /** A value of {@link #TAG_T4_OPTIONS}. */
    public static final int T4_OPTIONS_EOL_BYTE_ALIGNED = 4;

    /** The number of the t6 options tag. */
    public static final int TAG_T6_OPTIONS = 293;

    /** A value of {@link #TAG_T6_OPTIONS}. */
    public static final int T6_OPTIONS_UNCOMPRESSED = 2;

    /** The number of the resolution unit tag. */
    public static final int TAG_RESOLUTION_UNIT = 296;

    /** A value of {@link #TAG_RESOLUTION_UNIT}. */
    public static final int RESOLUTION_UNIT_NONE = 1;

    /** A value of {@link #TAG_RESOLUTION_UNIT}. */
    public static final int RESOLUTION_UNIT_INCH = 2;

    /** A value of {@link #TAG_RESOLUTION_UNIT}. */
    public static final int RESOLUTION_UNIT_CENTIMETER = 3;

    /** The number of the page number tag. */
    public static final int TAG_PAGE_NUMBER = 297;

    /** The number of the transfer function tag. */
    public static final int TAG_TRANSFER_FUNCTION = 301;

    /** The number of the software tag. */
    public static final int TAG_SOFTWARE = 305;

    /** The number of the date time tag. */
    public static final int TAG_DATE_TIME = 306;

    /** The number of the artist tag. */
    public static final int TAG_ARTIST = 315;

    /** The number of the host computer tag. */
    public static final int TAG_HOST_COMPUTER = 316;

    /** The number of the predictor tag. */
    public static final int TAG_PREDICTOR = 317;

    /** A value of {@link #TAG_PREDICTOR}. */
    public static final int PREDICTOR_NONE = 1;

    /** A value of {@link #TAG_PREDICTOR}. */
    public static final int PREDICTOR_HORIZONTAL_DIFFERENCING = 2;

    /** The number of the white point tag. */
    public static final int TAG_WHITE_POINT = 318;

    /** The number of the primary chromaticites tag. */
    public static final int TAG_PRIMARY_CHROMATICITES = 319;

    /** The number of the color map tag. */
    public static final int TAG_COLOR_MAP = 320;

    /** The number of the halftone hints tag. */
    public static final int TAG_HALFTONE_HINTS = 321;

    /** The number of the tile width tag. */
    public static final int TAG_TILE_WIDTH = 322;

    /** The number of the tile length tag. */
    public static final int TAG_TILE_LENGTH = 323;

    /** The number of the tile offsets tag. */
    public static final int TAG_TILE_OFFSETS = 324;

    /** The number of the tile byte counts tag. */
    public static final int TAG_TILE_BYTE_COUNTS = 325;

    /** The number of the ink set tag. */
    public static final int TAG_INK_SET = 332;

    /** A value of {@link #TAG_INK_SET}. */
    public static final int INK_SET_CMYK = 1;

    /** A value of {@link #TAG_INK_SET}. */
    public static final int INK_SET_NOT_CMYK = 2;

    /** The number of the ink names tag. */
    public static final int TAG_INK_NAMES = 333;

    /** The number of the number of inks tag. */
    public static final int TAG_NUMBER_OF_INKS = 334;

    /** The number of the dot range tag. */
    public static final int TAG_DOT_RANGE = 336;

    /** The number of the target printer tag. */
    public static final int TAG_TARGET_PRINTER = 337;

    /** The number of the extra samples tag. */
    public static final int TAG_EXTRA_SAMPLES = 338;

    /** A value of {@link #TAG_EXTRA_SAMPLES}. */
    public static final int EXTRA_SAMPLES_UNSPECIFIED = 0;

    /** A value of {@link #TAG_EXTRA_SAMPLES}. */
    public static final int EXTRA_SAMPLES_ASSOCIATED_ALPHA = 1;

    /** A value of {@link #TAG_EXTRA_SAMPLES}. */
    public static final int EXTRA_SAMPLES_UNASSOCIATED_ALPHA = 2;

    /** The number of the sample format tag. */
    public static final int TAG_SAMPLE_FORMAT = 339;

    /** A value of {@link #TAG_SAMPLE_FORMAT}. */
    public static final int SAMPLE_FORMAT_UNSIGNED_INTEGER = 1;

    /** A value of {@link #TAG_SAMPLE_FORMAT}. */
    public static final int SAMPLE_FORMAT_SIGNED_INTEGER = 2;

    /** A value of {@link #TAG_SAMPLE_FORMAT}. */
    public static final int SAMPLE_FORMAT_FLOATING_POINT = 3;

    /** A value of {@link #TAG_SAMPLE_FORMAT}. */
    public static final int SAMPLE_FORMAT_UNDEFINED = 4;

    /** The number of the s min sample value tag. */
    public static final int TAG_S_MIN_SAMPLE_VALUE = 340;

    /** The number of the s max sample value tag. */
    public static final int TAG_S_MAX_SAMPLE_VALUE = 341;

    /** The number of the transfer range tag. */
    public static final int TAG_TRANSFER_RANGE = 342;

    /** The number of the jpeg tables tag. */
    public static final int TAG_JPEG_TABLES = 347;

    /** The number of the jpeg proc tag. */
    public static final int TAG_JPEG_PROC = 512;

    /** A value of {@link #TAG_JPEG_PROC}. */
    public static final int JPEG_PROC_BASELINE = 1;

    /** A value of {@link #TAG_JPEG_PROC}. */
    public static final int JPEG_PROC_LOSSLESS = 14;

    /** The number of the jpeg interchange format tag. */
    public static final int TAG_JPEG_INTERCHANGE_FORMAT = 513;

    /** The number of the jpeg interchange format length tag. */
    public static final int TAG_JPEG_INTERCHANGE_FORMAT_LENGTH = 514;

    /** The number of the jpeg restart interval tag. */
    public static final int TAG_JPEG_RESTART_INTERVAL = 515;

    /** The number of the jpeg lossless predictors tag. */
    public static final int TAG_JPEG_LOSSLESS_PREDICTORS = 517;

    /** The number of the jpeg point transforms tag. */
    public static final int TAG_JPEG_POINT_TRANSFORMS = 518;

    /** The number of the jpeg q tables tag. */
    public static final int TAG_JPEG_Q_TABLES = 519;

    /** The number of the jpeg dc tables tag. */
    public static final int TAG_JPEG_DC_TABLES = 520;

    /** The number of the jpeg ac tables tag. */
    public static final int TAG_JPEG_AC_TABLES = 521;

    /** The number of the y cb cr coefficients tag. */
    public static final int TAG_Y_CB_CR_COEFFICIENTS = 529;

    /** The number of the y cb cr subsampling tag. */
    public static final int TAG_Y_CB_CR_SUBSAMPLING = 530;

    /** The number of the y cb cr positioning tag. */
    public static final int TAG_Y_CB_CR_POSITIONING = 531;

    /** A value of {@link #TAG_Y_CB_CR_POSITIONING}. */
    public static final int Y_CB_CR_POSITIONING_CENTERED = 1;

    /** A value of {@link #TAG_Y_CB_CR_POSITIONING}. */
    public static final int Y_CB_CR_POSITIONING_COSITED = 2;

    /** The number of the reference black white tag. */
    public static final int TAG_REFERENCE_BLACK_WHITE = 532;

    /** The number of the copyright tag. */
    public static final int TAG_COPYRIGHT = 33432;

    /** The number of the icc profile tag. */
    public static final int TAG_ICC_PROFILE = 34675;


    /** Reached through {@link #getInstance}. */
    private BaselineTIFFTagSet() {
        super(tags());
    }

    /** The set. See the class note. */
    public static synchronized BaselineTIFFTagSet getInstance() {
        if (theInstance == null) {
            theInstance = new BaselineTIFFTagSet();
        }
        return theInstance;
    }

    /** The tags of this set. */
    private static List<TIFFTag> tags() {
        List<TIFFTag> tags = new ArrayList<TIFFTag>();
        tags.add(new TagNewSubfileType());
        tags.add(new TagSubfileType());
        tags.add(new TIFFTag("ImageWidth", 256, 24, 1));
        tags.add(new TIFFTag("ImageLength", 257, 24, 1));
        tags.add(new TIFFTag("BitsPerSample", 258, 8, -1));
        tags.add(new TagCompression());
        tags.add(new TagPhotometricInterpretation());
        tags.add(new TagThreshholding());
        tags.add(new TIFFTag("CellWidth", 264, 8, 1));
        tags.add(new TIFFTag("CellLength", 265, 8, 1));
        tags.add(new TagFillOrder());
        tags.add(new TIFFTag("DocumentName", 269, 4, -1));
        tags.add(new TIFFTag("ImageDescription", 270, 4, -1));
        tags.add(new TIFFTag("Make", 271, 4, -1));
        tags.add(new TIFFTag("Model", 272, 4, -1));
        tags.add(new TIFFTag("StripOffsets", 273, 24, -1));
        tags.add(new TagOrientation());
        tags.add(new TIFFTag("SamplesPerPixel", 277, 8, 1));
        tags.add(new TIFFTag("RowsPerStrip", 278, 24, 1));
        tags.add(new TIFFTag("StripByteCounts", 279, 24, -1));
        tags.add(new TIFFTag("MinSampleValue", 280, 8, -1));
        tags.add(new TIFFTag("MaxSampleValue", 281, 8, -1));
        tags.add(new TIFFTag("XResolution", 282, 32, 1));
        tags.add(new TIFFTag("YResolution", 283, 32, 1));
        tags.add(new TagPlanarConfiguration());
        tags.add(new TIFFTag("PageName", 285, 4, -1));
        tags.add(new TIFFTag("XPosition", 286, 32, 1));
        tags.add(new TIFFTag("YPosition", 287, 32, 1));
        tags.add(new TIFFTag("FreeOffsets", 288, 16, -1));
        tags.add(new TIFFTag("FreeByteCounts", 289, 16, -1));
        tags.add(new TagGrayResponseUnit());
        tags.add(new TIFFTag("GrayResponseCurve", 291, 8, -1));
        tags.add(new TagT4Options());
        tags.add(new TagT6Options());
        tags.add(new TagResolutionUnit());
        tags.add(new TIFFTag("PageNumber", 297, 8, -1));
        tags.add(new TIFFTag("TransferFunction", 301, 8, -1));
        tags.add(new TIFFTag("Software", 305, 4, -1));
        tags.add(new TIFFTag("DateTime", 306, 4, 20));
        tags.add(new TIFFTag("Artist", 315, 4, -1));
        tags.add(new TIFFTag("HostComputer", 316, 4, -1));
        tags.add(new TagPredictor());
        tags.add(new TIFFTag("WhitePoint", 318, 32, 2));
        tags.add(new TIFFTag("PrimaryChromaticities", 319, 32, 6));
        tags.add(new TIFFTag("ColorMap", 320, 8, -1));
        tags.add(new TIFFTag("HalftoneHints", 321, 8, 2));
        tags.add(new TIFFTag("TileWidth", 322, 24, 1));
        tags.add(new TIFFTag("TileLength", 323, 24, 1));
        tags.add(new TIFFTag("TileOffsets", 324, 16, -1));
        tags.add(new TIFFTag("TileByteCounts", 325, 24, -1));
        tags.add(new TagInkSet());
        tags.add(new TIFFTag("InkNames", 333, 4, -1));
        tags.add(new TIFFTag("NumberOfInks", 334, 8, 1));
        tags.add(new TIFFTag("DotRange", 336, 10, -1));
        tags.add(new TIFFTag("TargetPrinter", 337, 4, -1));
        tags.add(new TagExtraSamples());
        tags.add(new TagSampleFormat());
        tags.add(new TIFFTag("SMinSampleValue", 340, 8058, -1));
        tags.add(new TIFFTag("SMaxSampleValue", 341, 8058, -1));
        tags.add(new TIFFTag("TransferRange", 342, 8, 6));
        tags.add(new TIFFTag("JPEGTables", 347, 128, -1));
        tags.add(new TagJPEGProc());
        tags.add(new TIFFTag("JPEGInterchangeFormat", 513, 16, 1));
        tags.add(new TIFFTag("JPEGInterchangeFormatLength", 514, 16, 1));
        tags.add(new TIFFTag("JPEGRestartInterval", 515, 8, 1));
        tags.add(new TagJPEGLosslessPredictors());
        tags.add(new TIFFTag("JPEGPointTransforms", 518, 8, -1));
        tags.add(new TIFFTag("JPEGQTables", 519, 16, -1));
        tags.add(new TIFFTag("JPEGDCTables", 520, 16, -1));
        tags.add(new TIFFTag("JPEGACTables", 521, 16, -1));
        tags.add(new TIFFTag("YCbCrCoefficients", 529, 32, 3));
        tags.add(new TIFFTag("YCbCrSubSampling", 530, 8, 2));
        tags.add(new TagYCbCrPositioning());
        tags.add(new TIFFTag("ReferenceBlackWhite", 532, 32, -1));
        tags.add(new TIFFTag("Copyright", 33432, 4, -1));
        tags.add(new TIFFTag("ICC Profile", 34675, 128, -1));
        return tags;
    }

    /** {@code NewSubfileType}, with the names of its values. */
    private static final class TagNewSubfileType extends TIFFTag {

        TagNewSubfileType() {
            super("NewSubfileType", 254, 16, 1);
            addValueName(0, "Default");
            addValueName(1, "ReducedResolution");
            addValueName(2, "SinglePage");
            addValueName(3, "SinglePage+ReducedResolution");
            addValueName(4, "Transparency");
            addValueName(5, "Transparency+ReducedResolution");
            addValueName(6, "Transparency+SinglePage");
            addValueName(7, "Transparency+SinglePage+ReducedResolution");
        }
    }

    /** {@code SubfileType}, with the names of its values. */
    private static final class TagSubfileType extends TIFFTag {

        TagSubfileType() {
            super("SubfileType", 255, 8, 1);
            addValueName(1, "FullResolution");
            addValueName(2, "ReducedResolution");
            addValueName(3, "SinglePage");
        }
    }

    /** {@code Compression}, with the names of its values. */
    private static final class TagCompression extends TIFFTag {

        TagCompression() {
            super("Compression", 259, 8, 1);
            addValueName(1, "Uncompressed");
            addValueName(2, "CCITT RLE");
            addValueName(3, "CCITT T.4");
            addValueName(4, "CCITT T.6");
            addValueName(5, "LZW");
            addValueName(6, "Old JPEG");
            addValueName(7, "JPEG");
            addValueName(8, "ZLib");
            addValueName(32773, "PackBits");
            addValueName(32946, "Deflate");
        }
    }

    /** {@code PhotometricInterpretation}, with the names of its values. */
    private static final class TagPhotometricInterpretation extends TIFFTag {

        TagPhotometricInterpretation() {
            super("PhotometricInterpretation", 262, 8, 1);
            addValueName(0, "WhiteIsZero");
            addValueName(1, "BlackIsZero");
            addValueName(2, "RGB");
            addValueName(3, "Palette Color");
            addValueName(4, "Transparency Mask");
            addValueName(5, "CMYK");
            addValueName(6, "YCbCr");
            addValueName(8, "CIELAB");
            addValueName(9, "ICCLAB");
        }
    }

    /** {@code Threshholding}, with the names of its values. */
    private static final class TagThreshholding extends TIFFTag {

        TagThreshholding() {
            super("Threshholding", 263, 8, 1);
            addValueName(1, "None");
            addValueName(2, "OrderedDither");
            addValueName(3, "RandomizedDither");
        }
    }

    /** {@code FillOrder}, with the names of its values. */
    private static final class TagFillOrder extends TIFFTag {

        TagFillOrder() {
            super("FillOrder", 266, 8, 1);
            addValueName(1, "LeftToRight");
            addValueName(2, "RightToLeft");
        }
    }

    /** {@code Orientation}, with the names of its values. */
    private static final class TagOrientation extends TIFFTag {

        TagOrientation() {
            super("Orientation", 274, 8, 1);
            addValueName(1, "Row 0=Top, Column 0=Left");
            addValueName(2, "Row 0=Top, Column 0=Right");
            addValueName(3, "Row 0=Bottom, Column 0=Right");
            addValueName(4, "Row 0=Bottom, Column 0=Left");
            addValueName(5, "Row 0=Left, Column 0=Top");
            addValueName(6, "Row 0=Right, Column 0=Top");
            addValueName(7, "Row 0=Right, Column 0=Bottom");
        }
    }

    /** {@code PlanarConfiguration}, with the names of its values. */
    private static final class TagPlanarConfiguration extends TIFFTag {

        TagPlanarConfiguration() {
            super("PlanarConfiguration", 284, 8, 1);
            addValueName(1, "Chunky");
            addValueName(2, "Planar");
        }
    }

    /** {@code GrayResponseUnit}, with the names of its values. */
    private static final class TagGrayResponseUnit extends TIFFTag {

        TagGrayResponseUnit() {
            super("GrayResponseUnit", 290, 8, 1);
            addValueName(1, "Tenths");
            addValueName(2, "Hundredths");
            addValueName(3, "Thousandths");
            addValueName(4, "Ten-Thousandths");
            addValueName(5, "Hundred-Thousandths");
        }
    }

    /** {@code T4Options}, with the names of its values. */
    private static final class TagT4Options extends TIFFTag {

        TagT4Options() {
            super("T4Options", 292, 16, 1);
            addValueName(0, "Default 1DCoding");
            addValueName(1, "2DCoding");
            addValueName(2, "Uncompressed");
            addValueName(3, "2DCoding+Uncompressed");
            addValueName(4, "EOLByteAligned");
            addValueName(5, "2DCoding+EOLByteAligned");
            addValueName(6, "Uncompressed+EOLByteAligned");
            addValueName(7, "2DCoding+Uncompressed+EOLByteAligned");
        }
    }

    /** {@code T6Options}, with the names of its values. */
    private static final class TagT6Options extends TIFFTag {

        TagT6Options() {
            super("T6Options", 293, 16, 1);
            addValueName(0, "Default");
            addValueName(2, "Uncompressed");
        }
    }

    /** {@code ResolutionUnit}, with the names of its values. */
    private static final class TagResolutionUnit extends TIFFTag {

        TagResolutionUnit() {
            super("ResolutionUnit", 296, 8, 1);
            addValueName(1, "None");
            addValueName(2, "Inch");
            addValueName(3, "Centimeter");
        }
    }

    /** {@code Predictor}, with the names of its values. */
    private static final class TagPredictor extends TIFFTag {

        TagPredictor() {
            super("Predictor", 317, 8, 1);
            addValueName(1, "None");
            addValueName(2, "Horizontal Differencing");
        }
    }

    /** {@code InkSet}, with the names of its values. */
    private static final class TagInkSet extends TIFFTag {

        TagInkSet() {
            super("InkSet", 332, 8, 1);
            addValueName(1, "CMYK");
            addValueName(2, "Not CMYK");
        }
    }

    /** {@code ExtraSamples}, with the names of its values. */
    private static final class TagExtraSamples extends TIFFTag {

        TagExtraSamples() {
            super("ExtraSamples", 338, 8, -1);
            addValueName(0, "Unspecified");
            addValueName(1, "Associated Alpha");
            addValueName(2, "Unassociated Alpha");
        }
    }

    /** {@code SampleFormat}, with the names of its values. */
    private static final class TagSampleFormat extends TIFFTag {

        TagSampleFormat() {
            super("SampleFormat", 339, 8, -1);
            addValueName(1, "Unsigned Integer");
            addValueName(2, "Signed Integer");
            addValueName(3, "Floating Point");
            addValueName(4, "Undefined");
        }
    }

    /** {@code JPEGProc}, with the names of its values. */
    private static final class TagJPEGProc extends TIFFTag {

        TagJPEGProc() {
            super("JPEGProc", 512, 8, 1);
            addValueName(1, "Baseline sequential process");
            addValueName(14, "Lossless process with Huffman coding");
        }
    }

    /** {@code JPEGLosslessPredictors}, with the names of its values. */
    private static final class TagJPEGLosslessPredictors extends TIFFTag {

        TagJPEGLosslessPredictors() {
            super("JPEGLosslessPredictors", 517, 8, -1);
            addValueName(1, "A");
            addValueName(2, "B");
            addValueName(3, "C");
            addValueName(4, "A+B-C");
            addValueName(5, "A+((B-C)/2)");
            addValueName(6, "B+((A-C)/2)");
            addValueName(7, "(A+B)/2");
        }
    }

    /** {@code YCbCrPositioning}, with the names of its values. */
    private static final class TagYCbCrPositioning extends TIFFTag {

        TagYCbCrPositioning() {
            super("YCbCrPositioning", 531, 8, 1);
            addValueName(1, "Centered");
            addValueName(2, "Cosited");
        }
    }
}
