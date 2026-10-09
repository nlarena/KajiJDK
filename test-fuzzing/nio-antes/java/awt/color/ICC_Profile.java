package java.awt.color;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * An ICC profile: the description of a colour space in the ICC.1 specification's format.
 *
 * <p>A profile is bytes with a fixed structure: a **128-byte header** with the essentials --which
 * space it describes, against which connection space, which version-- and then a **tag table**,
 * each entry with its four-character signature, its offset and its size. Everything else --an RGB
 * profile's matrix, its response curves, the white point-- lives in those tags.
 *
 * <p>This class really reads and writes that format: {@link #getInstance(byte[])} validates the
 * `acsp` signature and walks the table, {@link #getData(int)} returns a tag's bytes,
 * {@link #setData} replaces it by rebuilding the table, and {@link #write(OutputStream)} dumps the
 * whole profile. A profile read and written back out comes out byte for byte the same.
 *
 * <h2>The built-in profiles</h2>
 *
 * <p>{@link #getInstance(int)} returns one of the standard spaces. The JDK ships them as resource
 * files; here they are **built** out of the standard's constants --the sRGB matrix adapted to D50,
 * IEC 61966-2-1's curve, the white point. They are valid ICC profiles, not imitations: they can be
 * written to a file and another program reads them.
 *
 * <p>The shape of each follows the JDK's, checked against it:
 *
 * <ul>
 * <li><b>sRGB</b> is a display-class {@link ICC_ProfileRGB} with the curves as a 1024-point
 *     **table**, so {@code getGamma} throws and {@code getTRC} answers.</li>
 * <li><b>Linear RGB</b> is the same but with the curves as **gamma 1.0**, so it is the other way
 *     round: {@code getGamma} answers and {@code getTRC} throws.</li>
 * <li><b>Grey</b> is an {@link ICC_ProfileGray} with gamma 1.0.</li>
 * <li><b>CIEXYZ</b> is an abstract profile with neither matrix nor curves.</li>
 * </ul>
 *
 * <p><strong>{@link ColorSpace#CS_PYCC} is not here</strong>, and it is the only absence. PhotoYCC
 * is defined not by formulas but by 230 KB of **interpolation tables** that come inside the
 * profile: without that file there is nothing to build, and assembling an empty profile with its
 * signature would be an object claiming to be PhotoYCC that does not convert like PhotoYCC. {@code
 * getInstance(CS_PYCC)} throws saying so.
 */
public class ICC_Profile implements Serializable {

    private static final long serialVersionUID = -3938515861990936766L;


    // ---- The seven profile classes, in this API's numbering (0..6). They are not the ICC
    // signatures: the `icSig...Class` constants are for that.

    /** `CLASS_INPUT`. */
    public static final int CLASS_INPUT = 0;
    /** `CLASS_DISPLAY`. */
    public static final int CLASS_DISPLAY = 1;
    /** `CLASS_OUTPUT`. */
    public static final int CLASS_OUTPUT = 2;
    /** `CLASS_DEVICELINK`. */
    public static final int CLASS_DEVICELINK = 3;
    /** `CLASS_COLORSPACECONVERSION`. */
    public static final int CLASS_COLORSPACECONVERSION = 4;
    /** `CLASS_ABSTRACT`. */
    public static final int CLASS_ABSTRACT = 5;
    /** `CLASS_NAMEDCOLOR`. */
    public static final int CLASS_NAMEDCOLOR = 6;

    // ---- The colour spaces, by their four-character ICC signature packed into
    // an `int`. `icSigRgbData` is 'RGB ' read as big-endian.

    /** `icSigXYZData`. */
    public static final int icSigXYZData = 1482250784;
    /** `icSigLabData`. */
    public static final int icSigLabData = 1281450528;
    /** `icSigLuvData`. */
    public static final int icSigLuvData = 1282766368;
    /** `icSigYCbCrData`. */
    public static final int icSigYCbCrData = 1497588338;
    /** `icSigYxyData`. */
    public static final int icSigYxyData = 1501067552;
    /** `icSigRgbData`. */
    public static final int icSigRgbData = 1380401696;
    /** `icSigGrayData`. */
    public static final int icSigGrayData = 1196573017;
    /** `icSigHsvData`. */
    public static final int icSigHsvData = 1213421088;
    /** `icSigHlsData`. */
    public static final int icSigHlsData = 1212961568;
    /** `icSigCmykData`. */
    public static final int icSigCmykData = 1129142603;
    /** `icSigCmyData`. */
    public static final int icSigCmyData = 1129142560;
    /** `icSigSpace2CLR`. */
    public static final int icSigSpace2CLR = 843271250;
    /** `icSigSpace3CLR`. */
    public static final int icSigSpace3CLR = 860048466;
    /** `icSigSpace4CLR`. */
    public static final int icSigSpace4CLR = 876825682;
    /** `icSigSpace5CLR`. */
    public static final int icSigSpace5CLR = 893602898;
    /** `icSigSpace6CLR`. */
    public static final int icSigSpace6CLR = 910380114;
    /** `icSigSpace7CLR`. */
    public static final int icSigSpace7CLR = 927157330;
    /** `icSigSpace8CLR`. */
    public static final int icSigSpace8CLR = 943934546;
    /** `icSigSpace9CLR`. */
    public static final int icSigSpace9CLR = 960711762;
    /** `icSigSpaceACLR`. */
    public static final int icSigSpaceACLR = 1094929490;
    /** `icSigSpaceBCLR`. */
    public static final int icSigSpaceBCLR = 1111706706;
    /** `icSigSpaceCCLR`. */
    public static final int icSigSpaceCCLR = 1128483922;
    /** `icSigSpaceDCLR`. */
    public static final int icSigSpaceDCLR = 1145261138;
    /** `icSigSpaceECLR`. */
    public static final int icSigSpaceECLR = 1162038354;
    /** `icSigSpaceFCLR`. */
    public static final int icSigSpaceFCLR = 1178815570;

    // ---- The profile classes, by their signature.

    /** `icSigInputClass`. */
    public static final int icSigInputClass = 1935896178;
    /** `icSigDisplayClass`. */
    public static final int icSigDisplayClass = 1835955314;
    /** `icSigOutputClass`. */
    public static final int icSigOutputClass = 1886549106;
    /** `icSigLinkClass`. */
    public static final int icSigLinkClass = 1818848875;
    /** `icSigAbstractClass`. */
    public static final int icSigAbstractClass = 1633842036;
    /** `icSigColorSpaceClass`. */
    public static final int icSigColorSpaceClass = 1936744803;
    /** `icSigNamedColorClass`. */
    public static final int icSigNamedColorClass = 1852662636;

    // ---- The rendering intents (ICC.1 §6.1.11).

    /** `icPerceptual`. */
    public static final int icPerceptual = 0;
    /** `icRelativeColorimetric`. */
    public static final int icRelativeColorimetric = 1;
    /** `icMediaRelativeColorimetric`. */
    public static final int icMediaRelativeColorimetric = 1;
    /** `icSaturation`. */
    public static final int icSaturation = 2;
    /** `icAbsoluteColorimetric`. */
    public static final int icAbsoluteColorimetric = 3;
    /** `icICCAbsoluteColorimetric`. */
    public static final int icICCAbsoluteColorimetric = 3;

    // ---- The tags. `icSigHead` is not a real tag: it is the value passed to `getData`/`setData`
    // to reach the 128-byte header.

    /** The header's pseudo-tag; see the group's comment. */
    public static final int icSigHead = 1751474532;
    /** `icSigAToB0Tag`. */
    public static final int icSigAToB0Tag = 1093812784;
    /** `icSigAToB1Tag`. */
    public static final int icSigAToB1Tag = 1093812785;
    /** `icSigAToB2Tag`. */
    public static final int icSigAToB2Tag = 1093812786;
    /** `icSigBlueColorantTag`. */
    public static final int icSigBlueColorantTag = 1649957210;
    /** `icSigBlueMatrixColumnTag`. */
    public static final int icSigBlueMatrixColumnTag = 1649957210;
    /** `icSigBlueTRCTag`. */
    public static final int icSigBlueTRCTag = 1649693251;
    /** `icSigBToA0Tag`. */
    public static final int icSigBToA0Tag = 1110589744;
    /** `icSigBToA1Tag`. */
    public static final int icSigBToA1Tag = 1110589745;
    /** `icSigBToA2Tag`. */
    public static final int icSigBToA2Tag = 1110589746;
    /** `icSigCalibrationDateTimeTag`. */
    public static final int icSigCalibrationDateTimeTag = 1667329140;
    /** `icSigCharTargetTag`. */
    public static final int icSigCharTargetTag = 1952543335;
    /** `icSigCopyrightTag`. */
    public static final int icSigCopyrightTag = 1668313716;
    /** `icSigCrdInfoTag`. */
    public static final int icSigCrdInfoTag = 1668441193;
    /** `icSigDeviceMfgDescTag`. */
    public static final int icSigDeviceMfgDescTag = 1684893284;
    /** `icSigDeviceModelDescTag`. */
    public static final int icSigDeviceModelDescTag = 1684890724;
    /** `icSigDeviceSettingsTag`. */
    public static final int icSigDeviceSettingsTag = 1684371059;
    /** `icSigGamutTag`. */
    public static final int icSigGamutTag = 1734438260;
    /** `icSigGrayTRCTag`. */
    public static final int icSigGrayTRCTag = 1800688195;
    /** `icSigGreenColorantTag`. */
    public static final int icSigGreenColorantTag = 1733843290;
    /** `icSigGreenMatrixColumnTag`. */
    public static final int icSigGreenMatrixColumnTag = 1733843290;
    /** `icSigGreenTRCTag`. */
    public static final int icSigGreenTRCTag = 1733579331;
    /** `icSigLuminanceTag`. */
    public static final int icSigLuminanceTag = 1819635049;
    /** `icSigMeasurementTag`. */
    public static final int icSigMeasurementTag = 1835360627;
    /** `icSigMediaBlackPointTag`. */
    public static final int icSigMediaBlackPointTag = 1651208308;
    /** `icSigMediaWhitePointTag`. */
    public static final int icSigMediaWhitePointTag = 2004119668;
    /** `icSigNamedColor2Tag`. */
    public static final int icSigNamedColor2Tag = 1852009522;
    /** `icSigOutputResponseTag`. */
    public static final int icSigOutputResponseTag = 1919251312;
    /** `icSigPreview0Tag`. */
    public static final int icSigPreview0Tag = 1886545200;
    /** `icSigPreview1Tag`. */
    public static final int icSigPreview1Tag = 1886545201;
    /** `icSigPreview2Tag`. */
    public static final int icSigPreview2Tag = 1886545202;
    /** `icSigProfileDescriptionTag`. */
    public static final int icSigProfileDescriptionTag = 1684370275;
    /** `icSigProfileSequenceDescTag`. */
    public static final int icSigProfileSequenceDescTag = 1886610801;
    /** `icSigPs2CRD0Tag`. */
    public static final int icSigPs2CRD0Tag = 1886610480;
    /** `icSigPs2CRD1Tag`. */
    public static final int icSigPs2CRD1Tag = 1886610481;
    /** `icSigPs2CRD2Tag`. */
    public static final int icSigPs2CRD2Tag = 1886610482;
    /** `icSigPs2CRD3Tag`. */
    public static final int icSigPs2CRD3Tag = 1886610483;
    /** `icSigPs2CSATag`. */
    public static final int icSigPs2CSATag = 1886597747;
    /** `icSigPs2RenderingIntentTag`. */
    public static final int icSigPs2RenderingIntentTag = 1886597737;
    /** `icSigRedColorantTag`. */
    public static final int icSigRedColorantTag = 1918392666;
    /** `icSigRedMatrixColumnTag`. */
    public static final int icSigRedMatrixColumnTag = 1918392666;
    /** `icSigRedTRCTag`. */
    public static final int icSigRedTRCTag = 1918128707;
    /** `icSigScreeningDescTag`. */
    public static final int icSigScreeningDescTag = 1935897188;
    /** `icSigScreeningTag`. */
    public static final int icSigScreeningTag = 1935897198;
    /** `icSigTechnologyTag`. */
    public static final int icSigTechnologyTag = 1952801640;
    /** `icSigUcrBgTag`. */
    public static final int icSigUcrBgTag = 1650877472;
    /** `icSigViewingCondDescTag`. */
    public static final int icSigViewingCondDescTag = 1987405156;
    /** `icSigViewingConditionsTag`. */
    public static final int icSigViewingConditionsTag = 1986618743;
    /** `icSigChromaticityTag`. */
    public static final int icSigChromaticityTag = 1667789421;
    /** `icSigChromaticAdaptationTag`. */
    public static final int icSigChromaticAdaptationTag = 1667785060;
    /** `icSigColorantOrderTag`. */
    public static final int icSigColorantOrderTag = 1668051567;
    /** `icSigColorantTableTag`. */
    public static final int icSigColorantTableTag = 1668051572;

    // ---- The offsets inside the 128-byte header.

    /** Offset of the profile's total size. */
    public static final int icHdrSize = 0;
    /** `icHdrCmmId`. */
    public static final int icHdrCmmId = 4;
    /** Offset of the version. */
    public static final int icHdrVersion = 8;
    /** Offset of the profile class. */
    public static final int icHdrDeviceClass = 12;
    /** Offset of the device's colour space. */
    public static final int icHdrColorSpace = 16;
    /** Offset of the connection space (always XYZ or Lab). */
    public static final int icHdrPcs = 20;
    /** `icHdrDate`. */
    public static final int icHdrDate = 24;
    /** Offset of the `acsp` signature, which is what makes a profile valid. */
    public static final int icHdrMagic = 36;
    /** `icHdrPlatform`. */
    public static final int icHdrPlatform = 40;
    /** `icHdrFlags`. */
    public static final int icHdrFlags = 44;
    /** `icHdrManufacturer`. */
    public static final int icHdrManufacturer = 48;
    /** `icHdrModel`. */
    public static final int icHdrModel = 52;
    /** `icHdrAttributes`. */
    public static final int icHdrAttributes = 56;
    /** `icHdrRenderingIntent`. */
    public static final int icHdrRenderingIntent = 64;
    /** Offset of the illuminant, which ICC fixes at D50. */
    public static final int icHdrIlluminant = 68;
    /** `icHdrCreator`. */
    public static final int icHdrCreator = 80;
    /** `icHdrProfileID`. */
    public static final int icHdrProfileID = 84;

    // ---- The offsets inside a tag.

    /** `icTagType`. */
    public static final int icTagType = 0;
    /** `icTagReserved`. */
    public static final int icTagReserved = 4;
    /** Offset of a `curveType`'s point count. */
    public static final int icCurveCount = 8;
    /** Offset of a `curveType`'s first point. */
    public static final int icCurveData = 12;
    /** Offset of the X inside an `XYZType`. */
    public static final int icXYZNumberX = 8;

    // The whole profile. It is the single source of truth: the accessors read from here instead of
    // keeping separate fields, so that `setData` cannot leave the object contradicting itself.
    private byte[] data;

    ICC_Profile(byte[] data) {
        this.data = data;
    }

    // ---- reading big-endian integers, which is how ICC stores everything ------------------------

    private static int readInt(byte[] b, int off) {
        return ((b[off] & 0xFF) << 24) | ((b[off + 1] & 0xFF) << 16)
                | ((b[off + 2] & 0xFF) << 8) | (b[off + 3] & 0xFF);
    }

    private static int readShort(byte[] b, int off) {
        return ((b[off] & 0xFF) << 8) | (b[off + 1] & 0xFF);
    }

    private static void writeInt(byte[] b, int off, int v) {
        b[off] = (byte) (v >> 24);
        b[off + 1] = (byte) (v >> 16);
        b[off + 2] = (byte) (v >> 8);
        b[off + 3] = (byte) v;
    }

    private static void writeShort(byte[] b, int off, int v) {
        b[off] = (byte) (v >> 8);
        b[off + 1] = (byte) v;
    }

    /** The header's size, which the format fixes. */
    private static final int HEADER_LEN = 128;

    /** The `acsp` signature, without which the bytes are not a profile. */
    private static final int MAGIC = 0x61637370;

    // ---- factories ------------------------------------------------------------------------------

    /**
     * The profile those bytes describe.
     *
     * <p>The declared size and the `acsp` signature are checked. What is **not** checked is that
     * each tag makes sense: that is discovered by whoever asks for it, and doing it up front would
     * mean understanding tags this library does not interpret.
     *
     * @throws IllegalArgumentException if the bytes are not a valid profile
     */
    public static ICC_Profile getInstance(byte[] data) {
        if (data == null || data.length < HEADER_LEN) {
            throw new IllegalArgumentException("Invalid ICC Profile Data");
        }
        int declared = readInt(data, icHdrSize);
        if (declared < HEADER_LEN || declared > data.length) {
            throw new IllegalArgumentException("Invalid ICC Profile Data");
        }
        if (readInt(data, icHdrMagic) != MAGIC) {
            throw new IllegalArgumentException("Invalid ICC Profile Data");
        }
        byte[] copy = new byte[declared];
        System.arraycopy(data, 0, copy, 0, declared);
        return withClassForSpace(copy);
    }

    // The subclass that suits the space and the tags present. The JDK does the same: an RGB profile
    // with matrix and curves is an `ICC_ProfileRGB`, a grey one with a curve is an
    // `ICC_ProfileGray`, and anything else is a plain `ICC_Profile`.
    private static ICC_Profile withClassForSpace(byte[] d) {
        ICC_Profile raw = new ICC_Profile(d);
        int space = raw.getColorSpaceType();
        if (space == ColorSpace.TYPE_RGB
                && raw.getData(icSigRedColorantTag) != null
                && raw.getData(icSigGreenColorantTag) != null
                && raw.getData(icSigBlueColorantTag) != null
                && raw.getData(icSigRedTRCTag) != null) {
            return new ICC_ProfileRGB(d);
        }
        if (space == ColorSpace.TYPE_GRAY && raw.getData(icSigGrayTRCTag) != null) {
            return new ICC_ProfileGray(d);
        }
        return raw;
    }

    /**
     * One of the built-in profiles.
     *
     * @throws IllegalArgumentException if the identifier is not one of the `ColorSpace.CS_`, or if
     *     it is {@link ColorSpace#CS_PYCC} -- see the class's note
     */
    public static ICC_Profile getInstance(int cspace) {
        if (cspace == ColorSpace.CS_sRGB) {
            return BuiltinProfiles.sRGB();
        }
        if (cspace == ColorSpace.CS_LINEAR_RGB) {
            return BuiltinProfiles.linearRGB();
        }
        if (cspace == ColorSpace.CS_GRAY) {
            return BuiltinProfiles.gray();
        }
        if (cspace == ColorSpace.CS_CIEXYZ) {
            return BuiltinProfiles.ciexyz();
        }
        if (cspace == ColorSpace.CS_PYCC) {
            throw new IllegalArgumentException(
                    "CS_PYCC is not available: its profile is interpolation tables that this "
                            + "library does not bring, and they cannot be derived from any "
                            + "formula");
        }
        throw new IllegalArgumentException("Unknown color space");
    }

    /**
     * The profile in that file.
     *
     * @throws IOException if it cannot be read
     * @throws IllegalArgumentException if the contents are not a valid profile
     */
    public static ICC_Profile getInstance(String fileName) throws IOException {
        FileInputStream in = new FileInputStream(new File(fileName));
        try {
            return getInstance(in);
        } finally {
            in.close();
        }
    }

    /**
     * The profile coming over that stream.
     *
     * <p>It reads **exactly** the bytes the header declares and not one more: a profile may come
     * embedded in a larger file --inside a JPEG, say-- and consuming to the end would take what
     * follows with it.
     *
     * @throws IOException if the stream is cut short
     * @throws IllegalArgumentException if what arrives is not a valid profile
     */
    public static ICC_Profile getInstance(InputStream s) throws IOException {
        byte[] header = new byte[HEADER_LEN];
        readFully(s, header, 0, HEADER_LEN);
        int total = readInt(header, icHdrSize);
        if (total < HEADER_LEN) {
            throw new IllegalArgumentException("Invalid ICC Profile Data");
        }
        byte[] all = new byte[total];
        System.arraycopy(header, 0, all, 0, HEADER_LEN);
        readFully(s, all, HEADER_LEN, total - HEADER_LEN);
        return getInstance(all);
    }

    private static void readFully(InputStream s, byte[] buf, int off, int len)
            throws IOException {
        int placed = 0;
        while (placed < len) {
            int n = s.read(buf, off + placed, len - placed);
            if (n < 0) {
                throw new IOException("the profile is cut short of what its header declares");
            }
            placed = placed + n;
        }
    }

    // ---- the header ----------------------------------------------------------------------------

    /** The version's major part. A 2.4 profile returns 2. */
    public int getMajorVersion() {
        return this.data[icHdrVersion] & 0xFF;
    }

    /**
     * The version's minor part, with both digits together.
     *
     * <p>The byte carries two four-bit numbers --minor and fix-- and this method returns them as
     * written: a 0x48 is 48, that is, version 2.4.8. It is what the JDK answers.
     */
    public int getMinorVersion() {
        return this.data[icHdrVersion + 1] & 0xFF;
    }

    /**
     * The profile's class, as one of the `CLASS_` constants.
     *
     * @throws IllegalArgumentException if the header's signature is none of the known ones
     */
    public int getProfileClass() {
        int sig = readInt(this.data, icHdrDeviceClass);
        if (sig == icSigInputClass) {
            return CLASS_INPUT;
        }
        if (sig == icSigDisplayClass) {
            return CLASS_DISPLAY;
        }
        if (sig == icSigOutputClass) {
            return CLASS_OUTPUT;
        }
        if (sig == icSigLinkClass) {
            return CLASS_DEVICELINK;
        }
        if (sig == icSigColorSpaceClass) {
            return CLASS_COLORSPACECONVERSION;
        }
        if (sig == icSigAbstractClass) {
            return CLASS_ABSTRACT;
        }
        if (sig == icSigNamedColorClass) {
            return CLASS_NAMEDCOLOR;
        }
        throw new IllegalArgumentException("Unknown profile class");
    }

    /** The device's space, as a `ColorSpace.TYPE_` constant. */
    public int getColorSpaceType() {
        return typeOfSignature(readInt(this.data, icHdrColorSpace));
    }

    /** The connection space, as a `ColorSpace.TYPE_` constant. */
    public int getPCSType() {
        return typeOfSignature(readInt(this.data, icHdrPcs));
    }

    private static int typeOfSignature(int sig) {
        if (sig == icSigXYZData) {
            return ColorSpace.TYPE_XYZ;
        }
        if (sig == icSigLabData) {
            return ColorSpace.TYPE_Lab;
        }
        if (sig == icSigLuvData) {
            return ColorSpace.TYPE_Luv;
        }
        if (sig == icSigYCbCrData) {
            return ColorSpace.TYPE_YCbCr;
        }
        if (sig == icSigYxyData) {
            return ColorSpace.TYPE_Yxy;
        }
        if (sig == icSigRgbData) {
            return ColorSpace.TYPE_RGB;
        }
        if (sig == icSigGrayData) {
            return ColorSpace.TYPE_GRAY;
        }
        if (sig == icSigHsvData) {
            return ColorSpace.TYPE_HSV;
        }
        if (sig == icSigHlsData) {
            return ColorSpace.TYPE_HLS;
        }
        if (sig == icSigCmykData) {
            return ColorSpace.TYPE_CMYK;
        }
        if (sig == icSigCmyData) {
            return ColorSpace.TYPE_CMY;
        }
        // The generic `nCLR` ones run consecutively, and so do their signatures: 2CLR..FCLR map to
        // TYPE_2CLR..TYPE_FCLR with no table.
        if (sig >= icSigSpace2CLR && sig <= icSigSpaceFCLR) {
            int i = 0;
            int[] generics = {
                icSigSpace2CLR, icSigSpace3CLR, icSigSpace4CLR, icSigSpace5CLR, icSigSpace6CLR,
                icSigSpace7CLR, icSigSpace8CLR, icSigSpace9CLR, icSigSpaceACLR, icSigSpaceBCLR,
                icSigSpaceCCLR, icSigSpaceDCLR, icSigSpaceECLR, icSigSpaceFCLR };
            while (i < generics.length) {
                if (generics[i] == sig) {
                    return ColorSpace.TYPE_2CLR + i;
                }
                i = i + 1;
            }
        }
        throw new IllegalArgumentException("Unknown color space");
    }

    /** How many components the device's space has. */
    public int getNumComponents() {
        int t = this.getColorSpaceType();
        if (t == ColorSpace.TYPE_GRAY) {
            return 1;
        }
        if (t == ColorSpace.TYPE_CMYK) {
            return 4;
        }
        if (t >= ColorSpace.TYPE_2CLR && t <= ColorSpace.TYPE_FCLR) {
            return t - ColorSpace.TYPE_2CLR + 2;
        }
        return 3;
    }

    // ---- the tags ------------------------------------------------------------------------------

    /** A copy of the whole profile. */
    public byte[] getData() {
        byte[] out = new byte[this.data.length];
        System.arraycopy(this.data, 0, out, 0, this.data.length);
        return out;
    }

    /**
     * That tag's bytes, or **null if the profile does not have it**.
     *
     * <p>Returning null rather than throwing is what allows an optional tag to be asked for without
     * wrapping the call in a `try`. With {@link #icSigHead} it returns the header.
     */
    public byte[] getData(int tagSignature) {
        if (tagSignature == icSigHead) {
            byte[] out = new byte[HEADER_LEN];
            System.arraycopy(this.data, 0, out, 0, HEADER_LEN);
            return out;
        }
        int n = readInt(this.data, HEADER_LEN);
        for (int i = 0; i < n; i++) {
            int e = HEADER_LEN + 4 + i * 12;
            if (readInt(this.data, e) == tagSignature) {
                int off = readInt(this.data, e + 4);
                int len = readInt(this.data, e + 8);
                if (off < 0 || len < 0 || off + len > this.data.length) {
                    throw new IllegalArgumentException(
                            "a tag of the profile points outside its data");
                }
                byte[] out = new byte[len];
                System.arraycopy(this.data, off, out, 0, len);
                return out;
            }
        }
        return null;
    }

    /**
     * Replaces that tag, or adds it if it was not there.
     *
     * <p>It rebuilds the whole profile: the tag table stores offsets, so changing one's size moves
     * every one that comes after it. Doing it in place would only work when the size happened to
     * match, and that asymmetry is exactly the kind of thing that breaks once in a thousand times.
     *
     * @throws IllegalArgumentException if {@link #icSigHead} is passed with something that is not
     *     128 bytes long
     */
    public void setData(int tagSignature, byte[] tagData) {
        if (tagSignature == icSigHead) {
            if (tagData == null || tagData.length != HEADER_LEN) {
                throw new IllegalArgumentException("la header mide 128 bytes");
            }
            System.arraycopy(tagData, 0, this.data, 0, HEADER_LEN);
            return;
        }
        int n = readInt(this.data, HEADER_LEN);
        int[] signatures = new int[n + 1];
        byte[][] bodies = new byte[n + 1][];
        int howMany = 0;
        boolean replaced = false;
        for (int i = 0; i < n; i++) {
            int e = HEADER_LEN + 4 + i * 12;
            int sig = readInt(this.data, e);
            if (sig == tagSignature) {
                if (tagData == null) {
                    // A `null` deletes the tag, which is what the JDK does.
                    replaced = true;
                    continue;
                }
                signatures[howMany] = sig;
                bodies[howMany] = tagData;
                howMany = howMany + 1;
                replaced = true;
                continue;
            }
            signatures[howMany] = sig;
            bodies[howMany] = this.getData(sig);
            howMany = howMany + 1;
        }
        if (!replaced && tagData != null) {
            signatures[howMany] = tagSignature;
            bodies[howMany] = tagData;
            howMany = howMany + 1;
        }
        byte[] header = new byte[HEADER_LEN];
        System.arraycopy(this.data, 0, header, 0, HEADER_LEN);
        this.data = assemble(header, signatures, bodies, howMany);
    }

    /**
     * Assembles a profile from its header and its tags.
     *
     * <p>Each tag is aligned to four bytes, as the format requires, and the padding is left at
     * zero. The total size is written into the header at the end, once it is known.
     */
    static byte[] assemble(byte[] header, int[] signatures, byte[][] bodies, int howMany) {
        int off = HEADER_LEN + 4 + howMany * 12;
        int[] offsets = new int[howMany];
        for (int i = 0; i < howMany; i++) {
            offsets[i] = off;
            off = off + ((bodies[i].length + 3) & ~3);
        }
        byte[] out = new byte[off];
        System.arraycopy(header, 0, out, 0, HEADER_LEN);
        writeInt(out, HEADER_LEN, howMany);
        for (int i = 0; i < howMany; i++) {
            int e = HEADER_LEN + 4 + i * 12;
            writeInt(out, e, signatures[i]);
            writeInt(out, e + 4, offsets[i]);
            writeInt(out, e + 8, bodies[i].length);
            System.arraycopy(bodies[i], 0, out, offsets[i], bodies[i].length);
        }
        writeInt(out, icHdrSize, off);
        return out;
    }

    /**
     * Writes the profile to that file.
     *
     * @throws IOException if it cannot be written
     */
    public void write(String fileName) throws IOException {
        FileOutputStream out = new FileOutputStream(new File(fileName));
        try {
            this.write(out);
        } finally {
            out.close();
        }
    }

    /**
     * Writes the profile to that stream. It does not close it.
     *
     * @throws IOException if the write fails
     */
    public void write(OutputStream s) throws IOException {
        s.write(this.data);
    }

    // ---- what the subclasses expose ------------------------------------------------------------
    //
    // Package-private, as in the JDK: `ICC_Profile` has them so that `ICC_ProfileRGB` and
    // `ICC_ProfileGray` can publish them with the type suiting their space. A CMYK profile has no
    // matrix, and publishing them here would oblige it to answer something.

    float[] getMediaWhitePoint() {
        return this.getXYZTag(icSigMediaWhitePointTag);
    }

    /**
     * The three numbers of an `XYZType` tag.
     *
     * <p>They come as s15Fixed16: signed integers where 1.0 is 0x10000. Dividing by 65536 is the
     * whole conversion.
     */
    final float[] getXYZTag(int tagSignature) {
        byte[] t = this.getData(tagSignature);
        if (t == null || t.length < icXYZNumberX + 12) {
            throw new ProfileDataException("falta la etiqueta XYZ pedida");
        }
        float[] out = new float[3];
        for (int i = 0; i < 3; i++) {
            out[i] = readInt(t, icXYZNumberX + i * 4) / 65536.0f;
        }
        return out;
    }

    /**
     * The gamma of a single-valued curve.
     *
     * @throws ProfileDataException if the curve is a **table** and not a gamma -- they are two
     *     different forms of the same `curveType` and only one has a gamma
     */
    float getGamma(int tagSignature) {
        return this.gammaOfTag(tagSignature);
    }

    /**
     * The same as {@link #getGamma}, with a name that **cannot be confused**.
     *
     * <p>It exists because of a trap in the inherited contract: `ICC_ProfileRGB.getGamma(int)`
     * takes a COMPONENT number and `ICC_Profile.getGamma(int)` takes a tag SIGNATURE -- the same
     * method signature, different meanings -- so the second is overridden by the first. Calling
     * `profile.getGamma(icSigRedTRCTag)` on an RGB profile does not read the red curve: it reads
     * the signature as a component and throws "Must be Red, Green, or Blue".
     *
     * <p>Whoever wants the curve by its tag uses this one. The collision comes from the JDK and
     * cannot be fixed without changing the public surface.
     */
    final float gammaOfTag(int tagSignature) {
        byte[] t = this.getData(tagSignature);
        if (t == null || t.length < icCurveData) {
            throw new ProfileDataException("falta la curva pedida");
        }
        int n = readInt(t, icCurveCount);
        if (n != 1) {
            throw new ProfileDataException(
                    "the curve is a table of " + n + " points and not a gamma");
        }
        // u8Fixed8: the 1.0 is 0x100.
        return readShort(t, icCurveData) / 256.0f;
    }

    /**
     * A curve's table.
     *
     * @throws ProfileDataException if the curve is a gamma and not a table
     */
    short[] getTRC(int tagSignature) {
        return this.trcOfTag(tagSignature);
    }

    /** Like {@link #gammaOfTag}, for the table. The same trap, the same way out. */
    final short[] trcOfTag(int tagSignature) {
        byte[] t = this.getData(tagSignature);
        if (t == null || t.length < icCurveData) {
            throw new ProfileDataException("falta la curva pedida");
        }
        int n = readInt(t, icCurveCount);
        if (n <= 1) {
            throw new ProfileDataException("the curve is a gamma and not a table");
        }
        short[] out = new short[n];
        for (int i = 0; i < n; i++) {
            out[i] = (short) readShort(t, icCurveData + i * 2);
        }
        return out;
    }

    /**
     * On deserializing, a built-in profile becomes **the same instance** again.
     *
     * <p>Without this, a `ColorSpace.getInstance(CS_sRGB).getProfile()` travelling through
     * serialization would stop being identical to the one here, and comparisons by identity would
     * start failing silently. A profile that is none of the built-in ones comes back as it stands.
     */
    protected Object readResolve() throws ObjectStreamException {
        int[] ids = { ColorSpace.CS_sRGB, ColorSpace.CS_LINEAR_RGB, ColorSpace.CS_GRAY,
            ColorSpace.CS_CIEXYZ };
        for (int i = 0; i < ids.length; i++) {
            ICC_Profile p = getInstance(ids[i]);
            if (sameContent(p.data, this.data)) {
                return p;
            }
        }
        return this;
    }

    private static boolean sameContent(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }
}
