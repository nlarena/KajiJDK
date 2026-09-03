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
 * Un perfil ICC: la descripcion de un espacio de color en el formato de la especificacion ICC.1.
 *
 * <p>Un perfil son bytes con una estructura fija: una **cabecera de 128 bytes** con lo esencial
 * --que espacio describe, contra que espacio de conexion, que version-- y despues una **tabla de
 * etiquetas**, cada una con su firma de cuatro caracteres, su offset y su tamano. Todo lo demas
 * --la matriz de un perfil RGB, sus curvas de respuesta, el punto blanco-- vive en esas etiquetas.
 *
 * <p>Esta clase lee y escribe ese formato de verdad: {@link #getInstance(byte[])} valida la firma
 * `acsp` y recorre la tabla, {@link #getData(int)} devuelve los bytes de una etiqueta,
 * {@link #setData} la reemplaza reconstruyendo la tabla, y {@link #write(OutputStream)} vuelca el
 * perfil entero. Un perfil leido y vuelto a escribir sale byte a byte igual.
 *
 * <h2>Los perfiles integrados</h2>
 *
 * <p>{@link #getInstance(int)} devuelve uno de los espacios estandar. El JDK los trae como archivos
 * de recurso; aca se **construyen** a partir de las constantes del estandar --la matriz sRGB
 * adaptada a D50, la curva de IEC 61966-2-1, el punto blanco--. Son perfiles ICC validos, no
 * imitaciones: se pueden escribir a un archivo y otro programa los lee.
 *
 * <p>La forma de cada uno sigue la del JDK, comprobada contra el:
 *
 * <ul>
 * <li><b>sRGB</b> es un {@link ICC_ProfileRGB} de clase display con las curvas como **tabla** de
 *     1024 puntos, asi que {@code getGamma} tira y {@code getTRC} contesta.</li>
 * <li><b>RGB lineal</b> es igual pero con las curvas como **gamma 1.0**, asi que es al reves:
 *     {@code getGamma} contesta y {@code getTRC} tira.</li>
 * <li><b>Gris</b> es un {@link ICC_ProfileGray} con gamma 1.0.</li>
 * <li><b>CIEXYZ</b> es un perfil abstracto sin matriz ni curvas.</li>
 * </ul>
 *
 * <p><strong>{@link ColorSpace#CS_PYCC} no esta</strong>, y es la unica ausencia. PhotoYCC no se
 * define por formulas sino por **tablas de interpolacion** de 230 KB que vienen dentro del perfil:
 * sin ese archivo no hay nada que construir, y armar un perfil vacio con su firma seria un objeto
 * que dice ser PhotoYCC y no convierte como PhotoYCC. {@code getInstance(CS_PYCC)} tira diciendo
 * eso.
 */
public class ICC_Profile implements Serializable {

    private static final long serialVersionUID = -3938515861990936766L;


    // ---- Las siete clases de perfil, en la numeracion de esta API (0..6). No son las
    // firmas ICC: para eso estan las `icSig...Class`.

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

    // ---- Los espacios de color, por su firma ICC de cuatro caracteres empaquetada en
    // un `int`. `icSigRgbData` es 'RGB ' leido como big-endian.

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

    // ---- Las clases de perfil, por su firma.

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

    // ---- Los propositos de renderizado (§6.1.11 de ICC.1).

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

    // ---- Las etiquetas. `icSigHead` no es una etiqueta de verdad: es el valor que se le
    // pasa a `getData`/`setData` para llegar a la cabecera de 128 bytes.

    /** El pseudo-tag de la cabecera; ver el comentario del grupo. */
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

    // ---- Los offsets dentro de la cabecera de 128 bytes.

    /** Offset del tamano total del perfil. */
    public static final int icHdrSize = 0;
    /** `icHdrCmmId`. */
    public static final int icHdrCmmId = 4;
    /** Offset de la version. */
    public static final int icHdrVersion = 8;
    /** Offset de la clase de perfil. */
    public static final int icHdrDeviceClass = 12;
    /** Offset del espacio de color del dispositivo. */
    public static final int icHdrColorSpace = 16;
    /** Offset del espacio de conexion (siempre XYZ o Lab). */
    public static final int icHdrPcs = 20;
    /** `icHdrDate`. */
    public static final int icHdrDate = 24;
    /** Offset de la firma `acsp`, que es lo que hace valido a un perfil. */
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
    /** Offset del iluminante, que ICC fija en D50. */
    public static final int icHdrIlluminant = 68;
    /** `icHdrCreator`. */
    public static final int icHdrCreator = 80;
    /** `icHdrProfileID`. */
    public static final int icHdrProfileID = 84;

    // ---- Los offsets dentro de una etiqueta.

    /** `icTagType`. */
    public static final int icTagType = 0;
    /** `icTagReserved`. */
    public static final int icTagReserved = 4;
    /** Offset de la cantidad de puntos de un `curveType`. */
    public static final int icCurveCount = 8;
    /** Offset del primer punto de un `curveType`. */
    public static final int icCurveData = 12;
    /** Offset de la X dentro de un `XYZType`. */
    public static final int icXYZNumberX = 8;

    // El perfil entero. Es la unica fuente de verdad: los accesores leen de aca en vez de guardar
    // campos aparte, para que `setData` no pueda dejar el objeto contradiciendose consigo mismo.
    private byte[] data;

    ICC_Profile(byte[] data) {
        this.data = data;
    }

    // ---- lectura de enteros big-endian, que es como ICC guarda todo -----------------------------

    private static int leerInt(byte[] b, int off) {
        return ((b[off] & 0xFF) << 24) | ((b[off + 1] & 0xFF) << 16)
                | ((b[off + 2] & 0xFF) << 8) | (b[off + 3] & 0xFF);
    }

    private static int leerShort(byte[] b, int off) {
        return ((b[off] & 0xFF) << 8) | (b[off + 1] & 0xFF);
    }

    private static void escribirInt(byte[] b, int off, int v) {
        b[off] = (byte) (v >> 24);
        b[off + 1] = (byte) (v >> 16);
        b[off + 2] = (byte) (v >> 8);
        b[off + 3] = (byte) v;
    }

    private static void escribirShort(byte[] b, int off, int v) {
        b[off] = (byte) (v >> 8);
        b[off + 1] = (byte) v;
    }

    /** El tamano de la cabecera, que el formato fija. */
    private static final int LARGO_CABECERA = 128;

    /** La firma `acsp`, sin la cual los bytes no son un perfil. */
    private static final int MAGIC = 0x61637370;

    // ---- fabricas ------------------------------------------------------------------------------

    /**
     * El perfil que describen esos bytes.
     *
     * <p>Se comprueban el tamano declarado y la firma `acsp`. Lo que **no** se comprueba es que
     * cada etiqueta tenga sentido: eso lo descubre quien la pida, y adelantarlo obligaria a
     * entender etiquetas que esta biblioteca no interpreta.
     *
     * @throws IllegalArgumentException si los bytes no son un perfil valido
     */
    public static ICC_Profile getInstance(byte[] data) {
        if (data == null || data.length < LARGO_CABECERA) {
            throw new IllegalArgumentException("Invalid ICC Profile Data");
        }
        int declarado = leerInt(data, icHdrSize);
        if (declarado < LARGO_CABECERA || declarado > data.length) {
            throw new IllegalArgumentException("Invalid ICC Profile Data");
        }
        if (leerInt(data, icHdrMagic) != MAGIC) {
            throw new IllegalArgumentException("Invalid ICC Profile Data");
        }
        byte[] copia = new byte[declarado];
        System.arraycopy(data, 0, copia, 0, declarado);
        return conClaseSegunEspacio(copia);
    }

    // La subclase que corresponde segun el espacio y las etiquetas presentes. El JDK hace lo mismo:
    // un perfil RGB con matriz y curvas es un `ICC_ProfileRGB`, uno gris con curva es un
    // `ICC_ProfileGray`, y cualquier otro es un `ICC_Profile` a secas.
    private static ICC_Profile conClaseSegunEspacio(byte[] d) {
        ICC_Profile crudo = new ICC_Profile(d);
        int espacio = crudo.getColorSpaceType();
        if (espacio == ColorSpace.TYPE_RGB
                && crudo.getData(icSigRedColorantTag) != null
                && crudo.getData(icSigGreenColorantTag) != null
                && crudo.getData(icSigBlueColorantTag) != null
                && crudo.getData(icSigRedTRCTag) != null) {
            return new ICC_ProfileRGB(d);
        }
        if (espacio == ColorSpace.TYPE_GRAY && crudo.getData(icSigGrayTRCTag) != null) {
            return new ICC_ProfileGray(d);
        }
        return crudo;
    }

    /**
     * Uno de los perfiles integrados.
     *
     * @throws IllegalArgumentException si el identificador no es uno de los `ColorSpace.CS_`, o si
     *     es {@link ColorSpace#CS_PYCC} -- ver la nota de la clase
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
                    "CS_PYCC no esta disponible: su perfil son tablas de interpolacion que esta "
                            + "biblioteca no trae, y no se pueden derivar de ninguna formula");
        }
        throw new IllegalArgumentException("Unknown color space");
    }

    /**
     * El perfil que hay en ese archivo.
     *
     * @throws IOException si no se puede leer
     * @throws IllegalArgumentException si el contenido no es un perfil valido
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
     * El perfil que viene por ese flujo.
     *
     * <p>Lee **exactamente** los bytes que la cabecera declara y no uno mas: un perfil puede venir
     * incrustado en un archivo mas grande --dentro de un JPEG, por ejemplo-- y consumir hasta el
     * final se llevaria puesto lo que sigue.
     *
     * @throws IOException si el flujo se corta antes de tiempo
     * @throws IllegalArgumentException si lo que llega no es un perfil valido
     */
    public static ICC_Profile getInstance(InputStream s) throws IOException {
        byte[] cabecera = new byte[LARGO_CABECERA];
        leerCompleto(s, cabecera, 0, LARGO_CABECERA);
        int total = leerInt(cabecera, icHdrSize);
        if (total < LARGO_CABECERA) {
            throw new IllegalArgumentException("Invalid ICC Profile Data");
        }
        byte[] todo = new byte[total];
        System.arraycopy(cabecera, 0, todo, 0, LARGO_CABECERA);
        leerCompleto(s, todo, LARGO_CABECERA, total - LARGO_CABECERA);
        return getInstance(todo);
    }

    private static void leerCompleto(InputStream s, byte[] buf, int off, int len)
            throws IOException {
        int puestos = 0;
        while (puestos < len) {
            int n = s.read(buf, off + puestos, len - puestos);
            if (n < 0) {
                throw new IOException("el perfil se corta antes de lo que declara su cabecera");
            }
            puestos = puestos + n;
        }
    }

    // ---- la cabecera ---------------------------------------------------------------------------

    /** La parte mayor de la version. Un perfil 2.4 devuelve 2. */
    public int getMajorVersion() {
        return this.data[icHdrVersion] & 0xFF;
    }

    /**
     * La parte menor de la version, con los dos digitos juntos.
     *
     * <p>El byte trae dos numeros de cuatro bits --menor y correccion-- y este metodo los devuelve
     * como esta escrito: un 0x48 es 48, o sea version 2.4.8. Es lo que contesta el JDK.
     */
    public int getMinorVersion() {
        return this.data[icHdrVersion + 1] & 0xFF;
    }

    /**
     * La clase de perfil, como una de las constantes `CLASS_`.
     *
     * @throws IllegalArgumentException si la firma de la cabecera no es ninguna conocida
     */
    public int getProfileClass() {
        int sig = leerInt(this.data, icHdrDeviceClass);
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

    /** El espacio del dispositivo, como una constante `ColorSpace.TYPE_`. */
    public int getColorSpaceType() {
        return tipoDeFirma(leerInt(this.data, icHdrColorSpace));
    }

    /** El espacio de conexion, como una constante `ColorSpace.TYPE_`. */
    public int getPCSType() {
        return tipoDeFirma(leerInt(this.data, icHdrPcs));
    }

    private static int tipoDeFirma(int sig) {
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
        // Los genericos `nCLR` van seguidos, y su firma tambien: 2CLR..FCLR se mapean a
        // TYPE_2CLR..TYPE_FCLR sin tabla.
        if (sig >= icSigSpace2CLR && sig <= icSigSpaceFCLR) {
            int i = 0;
            int[] genericos = {
                icSigSpace2CLR, icSigSpace3CLR, icSigSpace4CLR, icSigSpace5CLR, icSigSpace6CLR,
                icSigSpace7CLR, icSigSpace8CLR, icSigSpace9CLR, icSigSpaceACLR, icSigSpaceBCLR,
                icSigSpaceCCLR, icSigSpaceDCLR, icSigSpaceECLR, icSigSpaceFCLR };
            while (i < genericos.length) {
                if (genericos[i] == sig) {
                    return ColorSpace.TYPE_2CLR + i;
                }
                i = i + 1;
            }
        }
        throw new IllegalArgumentException("Unknown color space");
    }

    /** Cuantos componentes tiene el espacio del dispositivo. */
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

    // ---- las etiquetas -------------------------------------------------------------------------

    /** Una copia del perfil entero. */
    public byte[] getData() {
        byte[] out = new byte[this.data.length];
        System.arraycopy(this.data, 0, out, 0, this.data.length);
        return out;
    }

    /**
     * Los bytes de esa etiqueta, o **nulo si el perfil no la tiene**.
     *
     * <p>Devolver nulo y no tirar es lo que permite preguntar por una etiqueta opcional sin
     * envolver la llamada en un `try`. Con {@link #icSigHead} devuelve la cabecera.
     */
    public byte[] getData(int tagSignature) {
        if (tagSignature == icSigHead) {
            byte[] out = new byte[LARGO_CABECERA];
            System.arraycopy(this.data, 0, out, 0, LARGO_CABECERA);
            return out;
        }
        int n = leerInt(this.data, LARGO_CABECERA);
        for (int i = 0; i < n; i++) {
            int e = LARGO_CABECERA + 4 + i * 12;
            if (leerInt(this.data, e) == tagSignature) {
                int off = leerInt(this.data, e + 4);
                int len = leerInt(this.data, e + 8);
                if (off < 0 || len < 0 || off + len > this.data.length) {
                    throw new IllegalArgumentException(
                            "una etiqueta del perfil apunta fuera de sus datos");
                }
                byte[] out = new byte[len];
                System.arraycopy(this.data, off, out, 0, len);
                return out;
            }
        }
        return null;
    }

    /**
     * Reemplaza esa etiqueta, o la agrega si no estaba.
     *
     * <p>Reconstruye el perfil entero: la tabla de etiquetas guarda offsets, asi que cambiar el
     * tamano de una mueve a todas las que van despues. Hacerlo en el lugar solo funcionaria cuando
     * el tamano coincidiera, y esa asimetria es justo la clase de cosa que rompe una vez cada mil.
     *
     * @throws IllegalArgumentException si se pasa {@link #icSigHead} con algo que no mida 128
     *     bytes
     */
    public void setData(int tagSignature, byte[] tagData) {
        if (tagSignature == icSigHead) {
            if (tagData == null || tagData.length != LARGO_CABECERA) {
                throw new IllegalArgumentException("la cabecera mide 128 bytes");
            }
            System.arraycopy(tagData, 0, this.data, 0, LARGO_CABECERA);
            return;
        }
        int n = leerInt(this.data, LARGO_CABECERA);
        int[] firmas = new int[n + 1];
        byte[][] cuerpos = new byte[n + 1][];
        int cuantas = 0;
        boolean reemplazada = false;
        for (int i = 0; i < n; i++) {
            int e = LARGO_CABECERA + 4 + i * 12;
            int sig = leerInt(this.data, e);
            if (sig == tagSignature) {
                if (tagData == null) {
                    // Un `null` borra la etiqueta, que es lo que hace el JDK.
                    reemplazada = true;
                    continue;
                }
                firmas[cuantas] = sig;
                cuerpos[cuantas] = tagData;
                cuantas = cuantas + 1;
                reemplazada = true;
                continue;
            }
            firmas[cuantas] = sig;
            cuerpos[cuantas] = this.getData(sig);
            cuantas = cuantas + 1;
        }
        if (!reemplazada && tagData != null) {
            firmas[cuantas] = tagSignature;
            cuerpos[cuantas] = tagData;
            cuantas = cuantas + 1;
        }
        byte[] cabecera = new byte[LARGO_CABECERA];
        System.arraycopy(this.data, 0, cabecera, 0, LARGO_CABECERA);
        this.data = armar(cabecera, firmas, cuerpos, cuantas);
    }

    /**
     * Arma un perfil desde su cabecera y sus etiquetas.
     *
     * <p>Cada etiqueta se alinea a cuatro bytes, como pide el formato, y el relleno queda en cero.
     * El tamano total se escribe en la cabecera al final, cuando ya se sabe.
     */
    static byte[] armar(byte[] cabecera, int[] firmas, byte[][] cuerpos, int cuantas) {
        int off = LARGO_CABECERA + 4 + cuantas * 12;
        int[] offsets = new int[cuantas];
        for (int i = 0; i < cuantas; i++) {
            offsets[i] = off;
            off = off + ((cuerpos[i].length + 3) & ~3);
        }
        byte[] out = new byte[off];
        System.arraycopy(cabecera, 0, out, 0, LARGO_CABECERA);
        escribirInt(out, LARGO_CABECERA, cuantas);
        for (int i = 0; i < cuantas; i++) {
            int e = LARGO_CABECERA + 4 + i * 12;
            escribirInt(out, e, firmas[i]);
            escribirInt(out, e + 4, offsets[i]);
            escribirInt(out, e + 8, cuerpos[i].length);
            System.arraycopy(cuerpos[i], 0, out, offsets[i], cuerpos[i].length);
        }
        escribirInt(out, icHdrSize, off);
        return out;
    }

    /**
     * Escribe el perfil a ese archivo.
     *
     * @throws IOException si no se puede escribir
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
     * Escribe el perfil a ese flujo. No lo cierra.
     *
     * @throws IOException si falla la escritura
     */
    public void write(OutputStream s) throws IOException {
        s.write(this.data);
    }

    // ---- lo que las subclases exponen ----------------------------------------------------------
    //
    // De paquete, como en el JDK: `ICC_Profile` las tiene para que `ICC_ProfileRGB` y
    // `ICC_ProfileGray` las publiquen con el tipo que corresponde a su espacio. Un perfil CMYK no
    // tiene matriz, y publicarlas aca obligaria a que contestara algo.

    float[] getMediaWhitePoint() {
        return this.getXYZTag(icSigMediaWhitePointTag);
    }

    /**
     * Los tres numeros de una etiqueta `XYZType`.
     *
     * <p>Vienen como s15Fixed16: enteros con signo donde el 1.0 es 0x10000. Dividir por 65536 es
     * toda la conversion.
     */
    final float[] getXYZTag(int tagSignature) {
        byte[] t = this.getData(tagSignature);
        if (t == null || t.length < icXYZNumberX + 12) {
            throw new ProfileDataException("falta la etiqueta XYZ pedida");
        }
        float[] out = new float[3];
        for (int i = 0; i < 3; i++) {
            out[i] = leerInt(t, icXYZNumberX + i * 4) / 65536.0f;
        }
        return out;
    }

    /**
     * La gamma de una curva de un solo valor.
     *
     * @throws ProfileDataException si la curva es una **tabla** y no una gamma -- son dos formas
     *     distintas del mismo `curveType` y solo una tiene gamma
     */
    float getGamma(int tagSignature) {
        return this.gammaOfTag(tagSignature);
    }

    /**
     * Lo mismo que {@link #getGamma}, con un nombre que **no se puede confundir**.
     *
     * <p>Existe por una trampa del contrato heredado: `ICC_ProfileRGB.getGamma(int)` toma un
     * numero de COMPONENTE y `ICC_Profile.getGamma(int)` toma una FIRMA de etiqueta -- misma
     * firma de metodo, significados distintos--, asi que el segundo queda redefinido por el
     * primero. Llamar a `perfil.getGamma(icSigRedTRCTag)` sobre un perfil RGB no lee la curva
     * roja: interpreta la firma como componente y tira "Must be Red, Green, or Blue".
     *
     * <p>Quien quiera la curva por su etiqueta usa esta. La colision viene del JDK y no se puede
     * arreglar sin cambiar la superficie publica.
     */
    final float gammaOfTag(int tagSignature) {
        byte[] t = this.getData(tagSignature);
        if (t == null || t.length < icCurveData) {
            throw new ProfileDataException("falta la curva pedida");
        }
        int n = leerInt(t, icCurveCount);
        if (n != 1) {
            throw new ProfileDataException(
                    "la curva es una tabla de " + n + " puntos y no una gamma");
        }
        // u8Fixed8: el 1.0 es 0x100.
        return leerShort(t, icCurveData) / 256.0f;
    }

    /**
     * La tabla de una curva.
     *
     * @throws ProfileDataException si la curva es una gamma y no una tabla
     */
    short[] getTRC(int tagSignature) {
        return this.trcOfTag(tagSignature);
    }

    /** Como {@link #gammaOfTag}, para la tabla. Misma trampa, misma salida. */
    final short[] trcOfTag(int tagSignature) {
        byte[] t = this.getData(tagSignature);
        if (t == null || t.length < icCurveData) {
            throw new ProfileDataException("falta la curva pedida");
        }
        int n = leerInt(t, icCurveCount);
        if (n <= 1) {
            throw new ProfileDataException("la curva es una gamma y no una tabla");
        }
        short[] out = new short[n];
        for (int i = 0; i < n; i++) {
            out[i] = (short) leerShort(t, icCurveData + i * 2);
        }
        return out;
    }

    /**
     * Al deserializar, un perfil integrado vuelve a ser **la misma instancia**.
     *
     * <p>Sin esto, un `ColorSpace.getInstance(CS_sRGB).getProfile()` que viaje por serializacion
     * dejaria de ser identico al de acá, y las comparaciones por identidad empezarian a fallar en
     * silencio. Un perfil que no es ninguno de los integrados vuelve tal cual.
     */
    protected Object readResolve() throws ObjectStreamException {
        int[] ids = { ColorSpace.CS_sRGB, ColorSpace.CS_LINEAR_RGB, ColorSpace.CS_GRAY,
            ColorSpace.CS_CIEXYZ };
        for (int i = 0; i < ids.length; i++) {
            ICC_Profile p = getInstance(ids[i]);
            if (mismoContenido(p.data, this.data)) {
                return p;
            }
        }
        return this;
    }

    private static boolean mismoContenido(byte[] a, byte[] b) {
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
