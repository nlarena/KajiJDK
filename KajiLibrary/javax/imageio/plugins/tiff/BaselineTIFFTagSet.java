package javax.imageio.plugins.tiff;

import java.util.ArrayList;
import java.util.List;

/**
 * KajiLibrary's javax.imageio.plugins.tiff.BaselineTIFFTagSet -- las etiquetas del TIFF 6.0 basico.
 *
 * <p>El nucleo del formato: tamano, resolucion, compresion, organizacion de las tiras o teselas,
 * paleta, y los campos de descripcion. Todo TIFF las usa.
 *
 * <p>Las constantes vienen de a dos familias y conviene distinguirlas: las {@code TAG_} son <b>numeros
 * de etiqueta</b>, y las demas son <b>valores</b> que ciertas etiquetas pueden tomar --por ejemplo
 * {@code COMPRESSION_LZW}, que es un valor de {@code TAG_COMPRESSION} y no una etiqueta--.
 *
 * <p>Es un singleton: se pide con {@link #getInstance}. Las etiquetas y sus valores nombrados se
 * transcribieron del JDK 25 y no a mano; un numero cambiado produce un TIFF que otros programas leen
 * distinto.
 */
public final class BaselineTIFFTagSet extends TIFFTagSet {

    /** El unico, armado la primera vez que se pide. */
    private static BaselineTIFFTagSet theInstance = null;

    /** El numero de la etiqueta new subfile type. */
    public static final int TAG_NEW_SUBFILE_TYPE = 254;

    /** Un valor de new. */
    public static final int NEW_SUBFILE_TYPE_REDUCED_RESOLUTION = 1;

    /** Un valor de new. */
    public static final int NEW_SUBFILE_TYPE_SINGLE_PAGE = 2;

    /** Un valor de new. */
    public static final int NEW_SUBFILE_TYPE_TRANSPARENCY = 4;

    /** El numero de la etiqueta subfile type. */
    public static final int TAG_SUBFILE_TYPE = 255;

    /** Un valor de subfile. */
    public static final int SUBFILE_TYPE_FULL_RESOLUTION = 1;

    /** Un valor de subfile. */
    public static final int SUBFILE_TYPE_REDUCED_RESOLUTION = 2;

    /** Un valor de subfile. */
    public static final int SUBFILE_TYPE_SINGLE_PAGE = 3;

    /** El numero de la etiqueta image width. */
    public static final int TAG_IMAGE_WIDTH = 256;

    /** El numero de la etiqueta image length. */
    public static final int TAG_IMAGE_LENGTH = 257;

    /** El numero de la etiqueta bits per sample. */
    public static final int TAG_BITS_PER_SAMPLE = 258;

    /** El numero de la etiqueta compression. */
    public static final int TAG_COMPRESSION = 259;

    /** Un valor de compression. */
    public static final int COMPRESSION_NONE = 1;

    /** Un valor de compression. */
    public static final int COMPRESSION_CCITT_RLE = 2;

    /** Un valor de compression. */
    public static final int COMPRESSION_CCITT_T_4 = 3;

    /** Un valor de compression. */
    public static final int COMPRESSION_CCITT_T_6 = 4;

    /** Un valor de compression. */
    public static final int COMPRESSION_LZW = 5;

    /** Un valor de compression. */
    public static final int COMPRESSION_OLD_JPEG = 6;

    /** Un valor de compression. */
    public static final int COMPRESSION_JPEG = 7;

    /** Un valor de compression. */
    public static final int COMPRESSION_ZLIB = 8;

    /** Un valor de compression. */
    public static final int COMPRESSION_PACKBITS = 32773;

    /** Un valor de compression. */
    public static final int COMPRESSION_DEFLATE = 32946;

    /** El numero de la etiqueta photometric interpretation. */
    public static final int TAG_PHOTOMETRIC_INTERPRETATION = 262;

    /** Un valor de photometric. */
    public static final int PHOTOMETRIC_INTERPRETATION_WHITE_IS_ZERO = 0;

    /** Un valor de photometric. */
    public static final int PHOTOMETRIC_INTERPRETATION_BLACK_IS_ZERO = 1;

    /** Un valor de photometric. */
    public static final int PHOTOMETRIC_INTERPRETATION_RGB = 2;

    /** Un valor de photometric. */
    public static final int PHOTOMETRIC_INTERPRETATION_PALETTE_COLOR = 3;

    /** Un valor de photometric. */
    public static final int PHOTOMETRIC_INTERPRETATION_TRANSPARENCY_MASK = 4;

    /** Un valor de photometric. */
    public static final int PHOTOMETRIC_INTERPRETATION_CMYK = 5;

    /** Un valor de photometric. */
    public static final int PHOTOMETRIC_INTERPRETATION_Y_CB_CR = 6;

    /** Un valor de photometric. */
    public static final int PHOTOMETRIC_INTERPRETATION_CIELAB = 8;

    /** Un valor de photometric. */
    public static final int PHOTOMETRIC_INTERPRETATION_ICCLAB = 9;

    /** El numero de la etiqueta threshholding. */
    public static final int TAG_THRESHHOLDING = 263;

    /** Un valor de threshholding. */
    public static final int THRESHHOLDING_NONE = 1;

    /** Un valor de threshholding. */
    public static final int THRESHHOLDING_ORDERED_DITHER = 2;

    /** Un valor de threshholding. */
    public static final int THRESHHOLDING_RANDOMIZED_DITHER = 3;

    /** El numero de la etiqueta cell width. */
    public static final int TAG_CELL_WIDTH = 264;

    /** El numero de la etiqueta cell length. */
    public static final int TAG_CELL_LENGTH = 265;

    /** El numero de la etiqueta fill order. */
    public static final int TAG_FILL_ORDER = 266;

    /** Un valor de fill. */
    public static final int FILL_ORDER_LEFT_TO_RIGHT = 1;

    /** Un valor de fill. */
    public static final int FILL_ORDER_RIGHT_TO_LEFT = 2;

    /** El numero de la etiqueta document name. */
    public static final int TAG_DOCUMENT_NAME = 269;

    /** El numero de la etiqueta image description. */
    public static final int TAG_IMAGE_DESCRIPTION = 270;

    /** El numero de la etiqueta make. */
    public static final int TAG_MAKE = 271;

    /** El numero de la etiqueta model. */
    public static final int TAG_MODEL = 272;

    /** El numero de la etiqueta strip offsets. */
    public static final int TAG_STRIP_OFFSETS = 273;

    /** El numero de la etiqueta orientation. */
    public static final int TAG_ORIENTATION = 274;

    /** Un valor de orientation. */
    public static final int ORIENTATION_ROW_0_TOP_COLUMN_0_LEFT = 1;

    /** Un valor de orientation. */
    public static final int ORIENTATION_ROW_0_TOP_COLUMN_0_RIGHT = 2;

    /** Un valor de orientation. */
    public static final int ORIENTATION_ROW_0_BOTTOM_COLUMN_0_RIGHT = 3;

    /** Un valor de orientation. */
    public static final int ORIENTATION_ROW_0_BOTTOM_COLUMN_0_LEFT = 4;

    /** Un valor de orientation. */
    public static final int ORIENTATION_ROW_0_LEFT_COLUMN_0_TOP = 5;

    /** Un valor de orientation. */
    public static final int ORIENTATION_ROW_0_RIGHT_COLUMN_0_TOP = 6;

    /** Un valor de orientation. */
    public static final int ORIENTATION_ROW_0_RIGHT_COLUMN_0_BOTTOM = 7;

    /** Un valor de orientation. */
    public static final int ORIENTATION_ROW_0_LEFT_COLUMN_0_BOTTOM = 8;

    /** El numero de la etiqueta samples per pixel. */
    public static final int TAG_SAMPLES_PER_PIXEL = 277;

    /** El numero de la etiqueta rows per strip. */
    public static final int TAG_ROWS_PER_STRIP = 278;

    /** El numero de la etiqueta strip byte counts. */
    public static final int TAG_STRIP_BYTE_COUNTS = 279;

    /** El numero de la etiqueta min sample value. */
    public static final int TAG_MIN_SAMPLE_VALUE = 280;

    /** El numero de la etiqueta max sample value. */
    public static final int TAG_MAX_SAMPLE_VALUE = 281;

    /** El numero de la etiqueta x resolution. */
    public static final int TAG_X_RESOLUTION = 282;

    /** El numero de la etiqueta y resolution. */
    public static final int TAG_Y_RESOLUTION = 283;

    /** El numero de la etiqueta planar configuration. */
    public static final int TAG_PLANAR_CONFIGURATION = 284;

    /** Un valor de planar. */
    public static final int PLANAR_CONFIGURATION_CHUNKY = 1;

    /** Un valor de planar. */
    public static final int PLANAR_CONFIGURATION_PLANAR = 2;

    /** El numero de la etiqueta page name. */
    public static final int TAG_PAGE_NAME = 285;

    /** El numero de la etiqueta x position. */
    public static final int TAG_X_POSITION = 286;

    /** El numero de la etiqueta y position. */
    public static final int TAG_Y_POSITION = 287;

    /** El numero de la etiqueta free offsets. */
    public static final int TAG_FREE_OFFSETS = 288;

    /** El numero de la etiqueta free byte counts. */
    public static final int TAG_FREE_BYTE_COUNTS = 289;

    /** El numero de la etiqueta gray response unit. */
    public static final int TAG_GRAY_RESPONSE_UNIT = 290;

    /** Un valor de gray. */
    public static final int GRAY_RESPONSE_UNIT_TENTHS = 1;

    /** Un valor de gray. */
    public static final int GRAY_RESPONSE_UNIT_HUNDREDTHS = 2;

    /** Un valor de gray. */
    public static final int GRAY_RESPONSE_UNIT_THOUSANDTHS = 3;

    /** Un valor de gray. */
    public static final int GRAY_RESPONSE_UNIT_TEN_THOUSANDTHS = 4;

    /** Un valor de gray. */
    public static final int GRAY_RESPONSE_UNIT_HUNDRED_THOUSANDTHS = 5;

    /** El numero de la etiqueta gray response curve. */
    public static final int TAG_GRAY_RESPONSE_CURVE = 291;

    /** El numero de la etiqueta t4 options. */
    public static final int TAG_T4_OPTIONS = 292;

    /** Un valor de t4. */
    public static final int T4_OPTIONS_2D_CODING = 1;

    /** Un valor de t4. */
    public static final int T4_OPTIONS_UNCOMPRESSED = 2;

    /** Un valor de t4. */
    public static final int T4_OPTIONS_EOL_BYTE_ALIGNED = 4;

    /** El numero de la etiqueta t6 options. */
    public static final int TAG_T6_OPTIONS = 293;

    /** Un valor de t6. */
    public static final int T6_OPTIONS_UNCOMPRESSED = 2;

    /** El numero de la etiqueta resolution unit. */
    public static final int TAG_RESOLUTION_UNIT = 296;

    /** Un valor de resolution. */
    public static final int RESOLUTION_UNIT_NONE = 1;

    /** Un valor de resolution. */
    public static final int RESOLUTION_UNIT_INCH = 2;

    /** Un valor de resolution. */
    public static final int RESOLUTION_UNIT_CENTIMETER = 3;

    /** El numero de la etiqueta page number. */
    public static final int TAG_PAGE_NUMBER = 297;

    /** El numero de la etiqueta transfer function. */
    public static final int TAG_TRANSFER_FUNCTION = 301;

    /** El numero de la etiqueta software. */
    public static final int TAG_SOFTWARE = 305;

    /** El numero de la etiqueta date time. */
    public static final int TAG_DATE_TIME = 306;

    /** El numero de la etiqueta artist. */
    public static final int TAG_ARTIST = 315;

    /** El numero de la etiqueta host computer. */
    public static final int TAG_HOST_COMPUTER = 316;

    /** El numero de la etiqueta predictor. */
    public static final int TAG_PREDICTOR = 317;

    /** Un valor de predictor. */
    public static final int PREDICTOR_NONE = 1;

    /** Un valor de predictor. */
    public static final int PREDICTOR_HORIZONTAL_DIFFERENCING = 2;

    /** El numero de la etiqueta white point. */
    public static final int TAG_WHITE_POINT = 318;

    /** El numero de la etiqueta primary chromaticites. */
    public static final int TAG_PRIMARY_CHROMATICITES = 319;

    /** El numero de la etiqueta color map. */
    public static final int TAG_COLOR_MAP = 320;

    /** El numero de la etiqueta halftone hints. */
    public static final int TAG_HALFTONE_HINTS = 321;

    /** El numero de la etiqueta tile width. */
    public static final int TAG_TILE_WIDTH = 322;

    /** El numero de la etiqueta tile length. */
    public static final int TAG_TILE_LENGTH = 323;

    /** El numero de la etiqueta tile offsets. */
    public static final int TAG_TILE_OFFSETS = 324;

    /** El numero de la etiqueta tile byte counts. */
    public static final int TAG_TILE_BYTE_COUNTS = 325;

    /** El numero de la etiqueta ink set. */
    public static final int TAG_INK_SET = 332;

    /** Un valor de ink. */
    public static final int INK_SET_CMYK = 1;

    /** Un valor de ink. */
    public static final int INK_SET_NOT_CMYK = 2;

    /** El numero de la etiqueta ink names. */
    public static final int TAG_INK_NAMES = 333;

    /** El numero de la etiqueta number of inks. */
    public static final int TAG_NUMBER_OF_INKS = 334;

    /** El numero de la etiqueta dot range. */
    public static final int TAG_DOT_RANGE = 336;

    /** El numero de la etiqueta target printer. */
    public static final int TAG_TARGET_PRINTER = 337;

    /** El numero de la etiqueta extra samples. */
    public static final int TAG_EXTRA_SAMPLES = 338;

    /** Un valor de extra. */
    public static final int EXTRA_SAMPLES_UNSPECIFIED = 0;

    /** Un valor de extra. */
    public static final int EXTRA_SAMPLES_ASSOCIATED_ALPHA = 1;

    /** Un valor de extra. */
    public static final int EXTRA_SAMPLES_UNASSOCIATED_ALPHA = 2;

    /** El numero de la etiqueta sample format. */
    public static final int TAG_SAMPLE_FORMAT = 339;

    /** Un valor de sample. */
    public static final int SAMPLE_FORMAT_UNSIGNED_INTEGER = 1;

    /** Un valor de sample. */
    public static final int SAMPLE_FORMAT_SIGNED_INTEGER = 2;

    /** Un valor de sample. */
    public static final int SAMPLE_FORMAT_FLOATING_POINT = 3;

    /** Un valor de sample. */
    public static final int SAMPLE_FORMAT_UNDEFINED = 4;

    /** El numero de la etiqueta s min sample value. */
    public static final int TAG_S_MIN_SAMPLE_VALUE = 340;

    /** El numero de la etiqueta s max sample value. */
    public static final int TAG_S_MAX_SAMPLE_VALUE = 341;

    /** El numero de la etiqueta transfer range. */
    public static final int TAG_TRANSFER_RANGE = 342;

    /** El numero de la etiqueta jpeg tables. */
    public static final int TAG_JPEG_TABLES = 347;

    /** El numero de la etiqueta jpeg proc. */
    public static final int TAG_JPEG_PROC = 512;

    /** Un valor de jpeg. */
    public static final int JPEG_PROC_BASELINE = 1;

    /** Un valor de jpeg. */
    public static final int JPEG_PROC_LOSSLESS = 14;

    /** El numero de la etiqueta jpeg interchange format. */
    public static final int TAG_JPEG_INTERCHANGE_FORMAT = 513;

    /** El numero de la etiqueta jpeg interchange format length. */
    public static final int TAG_JPEG_INTERCHANGE_FORMAT_LENGTH = 514;

    /** El numero de la etiqueta jpeg restart interval. */
    public static final int TAG_JPEG_RESTART_INTERVAL = 515;

    /** El numero de la etiqueta jpeg lossless predictors. */
    public static final int TAG_JPEG_LOSSLESS_PREDICTORS = 517;

    /** El numero de la etiqueta jpeg point transforms. */
    public static final int TAG_JPEG_POINT_TRANSFORMS = 518;

    /** El numero de la etiqueta jpeg q tables. */
    public static final int TAG_JPEG_Q_TABLES = 519;

    /** El numero de la etiqueta jpeg dc tables. */
    public static final int TAG_JPEG_DC_TABLES = 520;

    /** El numero de la etiqueta jpeg ac tables. */
    public static final int TAG_JPEG_AC_TABLES = 521;

    /** El numero de la etiqueta y cb cr coefficients. */
    public static final int TAG_Y_CB_CR_COEFFICIENTS = 529;

    /** El numero de la etiqueta y cb cr subsampling. */
    public static final int TAG_Y_CB_CR_SUBSAMPLING = 530;

    /** El numero de la etiqueta y cb cr positioning. */
    public static final int TAG_Y_CB_CR_POSITIONING = 531;

    /** Un valor de y. */
    public static final int Y_CB_CR_POSITIONING_CENTERED = 1;

    /** Un valor de y. */
    public static final int Y_CB_CR_POSITIONING_COSITED = 2;

    /** El numero de la etiqueta reference black white. */
    public static final int TAG_REFERENCE_BLACK_WHITE = 532;

    /** El numero de la etiqueta copyright. */
    public static final int TAG_COPYRIGHT = 33432;

    /** El numero de la etiqueta icc profile. */
    public static final int TAG_ICC_PROFILE = 34675;


    /** Se llega por {@link #getInstance}. */
    private BaselineTIFFTagSet() {
        super(tags());
    }

    /** El conjunto. Ver la nota de la clase. */
    public static synchronized BaselineTIFFTagSet getInstance() {
        if (theInstance == null) {
            theInstance = new BaselineTIFFTagSet();
        }
        return theInstance;
    }

    /** Las etiquetas de este conjunto. */
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

    /** {@code NewSubfileType}, con los nombres de sus valores. */
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

    /** {@code SubfileType}, con los nombres de sus valores. */
    private static final class TagSubfileType extends TIFFTag {

        TagSubfileType() {
            super("SubfileType", 255, 8, 1);
            addValueName(1, "FullResolution");
            addValueName(2, "ReducedResolution");
            addValueName(3, "SinglePage");
        }
    }

    /** {@code Compression}, con los nombres de sus valores. */
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

    /** {@code PhotometricInterpretation}, con los nombres de sus valores. */
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

    /** {@code Threshholding}, con los nombres de sus valores. */
    private static final class TagThreshholding extends TIFFTag {

        TagThreshholding() {
            super("Threshholding", 263, 8, 1);
            addValueName(1, "None");
            addValueName(2, "OrderedDither");
            addValueName(3, "RandomizedDither");
        }
    }

    /** {@code FillOrder}, con los nombres de sus valores. */
    private static final class TagFillOrder extends TIFFTag {

        TagFillOrder() {
            super("FillOrder", 266, 8, 1);
            addValueName(1, "LeftToRight");
            addValueName(2, "RightToLeft");
        }
    }

    /** {@code Orientation}, con los nombres de sus valores. */
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

    /** {@code PlanarConfiguration}, con los nombres de sus valores. */
    private static final class TagPlanarConfiguration extends TIFFTag {

        TagPlanarConfiguration() {
            super("PlanarConfiguration", 284, 8, 1);
            addValueName(1, "Chunky");
            addValueName(2, "Planar");
        }
    }

    /** {@code GrayResponseUnit}, con los nombres de sus valores. */
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

    /** {@code T4Options}, con los nombres de sus valores. */
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

    /** {@code T6Options}, con los nombres de sus valores. */
    private static final class TagT6Options extends TIFFTag {

        TagT6Options() {
            super("T6Options", 293, 16, 1);
            addValueName(0, "Default");
            addValueName(2, "Uncompressed");
        }
    }

    /** {@code ResolutionUnit}, con los nombres de sus valores. */
    private static final class TagResolutionUnit extends TIFFTag {

        TagResolutionUnit() {
            super("ResolutionUnit", 296, 8, 1);
            addValueName(1, "None");
            addValueName(2, "Inch");
            addValueName(3, "Centimeter");
        }
    }

    /** {@code Predictor}, con los nombres de sus valores. */
    private static final class TagPredictor extends TIFFTag {

        TagPredictor() {
            super("Predictor", 317, 8, 1);
            addValueName(1, "None");
            addValueName(2, "Horizontal Differencing");
        }
    }

    /** {@code InkSet}, con los nombres de sus valores. */
    private static final class TagInkSet extends TIFFTag {

        TagInkSet() {
            super("InkSet", 332, 8, 1);
            addValueName(1, "CMYK");
            addValueName(2, "Not CMYK");
        }
    }

    /** {@code ExtraSamples}, con los nombres de sus valores. */
    private static final class TagExtraSamples extends TIFFTag {

        TagExtraSamples() {
            super("ExtraSamples", 338, 8, -1);
            addValueName(0, "Unspecified");
            addValueName(1, "Associated Alpha");
            addValueName(2, "Unassociated Alpha");
        }
    }

    /** {@code SampleFormat}, con los nombres de sus valores. */
    private static final class TagSampleFormat extends TIFFTag {

        TagSampleFormat() {
            super("SampleFormat", 339, 8, -1);
            addValueName(1, "Unsigned Integer");
            addValueName(2, "Signed Integer");
            addValueName(3, "Floating Point");
            addValueName(4, "Undefined");
        }
    }

    /** {@code JPEGProc}, con los nombres de sus valores. */
    private static final class TagJPEGProc extends TIFFTag {

        TagJPEGProc() {
            super("JPEGProc", 512, 8, 1);
            addValueName(1, "Baseline sequential process");
            addValueName(14, "Lossless process with Huffman coding");
        }
    }

    /** {@code JPEGLosslessPredictors}, con los nombres de sus valores. */
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

    /** {@code YCbCrPositioning}, con los nombres de sus valores. */
    private static final class TagYCbCrPositioning extends TIFFTag {

        TagYCbCrPositioning() {
            super("YCbCrPositioning", 531, 8, 1);
            addValueName(1, "Centered");
            addValueName(2, "Cosited");
        }
    }
}
