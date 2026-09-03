package java.awt.color;

/**
 * Arma los perfiles ICC integrados a partir de las constantes del estandar.
 *
 * <p>El JDK los trae como archivos de recurso; aca se construyen. No es una imitacion: lo que sale
 * son perfiles ICC.1 validos --cabecera de 128 bytes, tabla de etiquetas, etiquetas `XYZType` y
 * `curveType` bien formadas-- que otro programa puede leer. Los numeros son los del estandar: la
 * matriz sRGB adaptada a D50 por Bradford, la curva de IEC 61966-2-1, los puntos blancos D50 y D65.
 *
 * <p>De paquete: como se arman es asunto de {@link ICC_Profile#getInstance(int)}, no del contrato.
 *
 * <p>Cada perfil se arma **una vez** y se comparte, porque `getInstance(int)` promete devolver
 * siempre el mismo objeto -- {@link ICC_Profile#readResolve} depende de eso.
 */
final class BuiltinProfiles {

    private BuiltinProfiles() {
    }

    // Las firmas de cuatro caracteres que hacen falta y que `ICC_Profile` no declara como
    // constante publica porque no son parte de su API.
    private static final int SIG_XYZ_TYPE = firma("XYZ ");
    private static final int SIG_CURVE_TYPE = firma("curv");
    private static final int SIG_TEXT_TYPE = firma("text");
    private static final int SIG_DESC_TYPE = firma("desc");

    /** Los cuatro caracteres de una firma, empaquetados en un `int` big-endian. */
    private static int firma(String s) {
        return ((s.charAt(0) & 0xFF) << 24) | ((s.charAt(1) & 0xFF) << 16)
                | ((s.charAt(2) & 0xFF) << 8) | (s.charAt(3) & 0xFF);
    }

    // La matriz sRGB a XYZ con blanco D50, por COLUMNAS: cada una es lo que aporta un primario.
    // Es la misma que usa `ColorSpace`, transpuesta, porque ICC guarda una columna por etiqueta.
    private static final float[] COL_ROJO = { 0.4360747f, 0.2225045f, 0.0139322f };
    private static final float[] COL_VERDE = { 0.3850649f, 0.7168786f, 0.0971045f };
    private static final float[] COL_AZUL = { 0.1430804f, 0.0606169f, 0.7141733f };

    /** D50, el iluminante que ICC fija para el espacio de conexion. */
    private static final float[] D50 = { 0.9642f, 1.0f, 0.8249f };

    /** D65, el blanco de un monitor sRGB. Es lo que va en `wtpt`, no D50. */
    private static final float[] D65 = { 0.9505f, 1.0f, 1.0891f };

    /**
     * El blanco que implica la matriz: la suma de sus tres columnas.
     *
     * <p>Es el color que sale de los tres primarios al maximo, o sea el blanco del espacio. Se
     * calcula en vez de escribirse para que siga coincidiendo si algun dia la matriz cambia.
     */
    private static float[] blancoDeLaMatriz() {
        return new float[] {
            COL_ROJO[0] + COL_VERDE[0] + COL_AZUL[0],
            COL_ROJO[1] + COL_VERDE[1] + COL_AZUL[1],
            COL_ROJO[2] + COL_VERDE[2] + COL_AZUL[2] };
    }

    private static ICC_Profile srgb;
    private static ICC_Profile linear;
    private static ICC_Profile gray;
    private static ICC_Profile xyz;

    static synchronized ICC_Profile sRGB() {
        if (srgb == null) {
            // La curva va como TABLA de 1024 puntos, igual que en el JDK: la de sRGB es a trozos
            // --un tramo recto cerca del cero y una potencia despues-- y una gamma sola no la
            // describe. Por eso `getGamma` de este perfil tira y `getTRC` contesta.
            byte[][] cuerpos = {
                xyzTag(COL_ROJO), xyzTag(COL_VERDE), xyzTag(COL_AZUL),
                curvaTabla(1024), curvaTabla(1024), curvaTabla(1024),
                xyzTag(D65), textoTag("KajiJDK sRGB"), descTag("sRGB integrado") };
            int[] firmas = {
                ICC_Profile.icSigRedColorantTag, ICC_Profile.icSigGreenColorantTag,
                ICC_Profile.icSigBlueColorantTag, ICC_Profile.icSigRedTRCTag,
                ICC_Profile.icSigGreenTRCTag, ICC_Profile.icSigBlueTRCTag,
                ICC_Profile.icSigMediaWhitePointTag, ICC_Profile.icSigCopyrightTag,
                ICC_Profile.icSigProfileDescriptionTag };
            srgb = ICC_Profile.getInstance(ICC_Profile.armar(
                    cabecera(ICC_Profile.icSigDisplayClass, ICC_Profile.icSigRgbData),
                    firmas, cuerpos, firmas.length));
        }
        return srgb;
    }

    static synchronized ICC_Profile linearRGB() {
        if (linear == null) {
            // Gamma 1.0 como un solo valor, no como tabla: aca `getGamma` contesta y `getTRC`
            // tira. Es el reparto inverso al de sRGB y es el que tiene el JDK.
            byte[][] cuerpos = {
                xyzTag(COL_ROJO), xyzTag(COL_VERDE), xyzTag(COL_AZUL),
                curvaGamma(1.0f), curvaGamma(1.0f), curvaGamma(1.0f),
                xyzTag(D65), textoTag("KajiJDK Linear RGB"), descTag("RGB lineal integrado") };
            int[] firmas = {
                ICC_Profile.icSigRedColorantTag, ICC_Profile.icSigGreenColorantTag,
                ICC_Profile.icSigBlueColorantTag, ICC_Profile.icSigRedTRCTag,
                ICC_Profile.icSigGreenTRCTag, ICC_Profile.icSigBlueTRCTag,
                ICC_Profile.icSigMediaWhitePointTag, ICC_Profile.icSigCopyrightTag,
                ICC_Profile.icSigProfileDescriptionTag };
            linear = ICC_Profile.getInstance(ICC_Profile.armar(
                    cabecera(ICC_Profile.icSigDisplayClass, ICC_Profile.icSigRgbData),
                    firmas, cuerpos, firmas.length));
        }
        return linear;
    }

    static synchronized ICC_Profile gray() {
        if (gray == null) {
            // El blanco del perfil gris es la SUMA DE LAS COLUMNAS de la matriz RGB, no el D50
            // tabulado. Los dos numeros difieren en la tercera cifra --0.82521 contra 0.8249--
            // porque la matriz sRGB publicada redondea, y esa diferencia no es inocua: un gris
            // convertido a sRGB pasando por XYZ salia con el azul un escalon por debajo del rojo
            // y el verde, o sea un gris con tinte. Haciendo que los dos perfiles integrados
            // compartan el mismo blanco, la ida y vuelta es exacta y un gris sigue siendo gris.
            byte[][] cuerpos = {
                curvaGamma(1.0f), xyzTag(blancoDeLaMatriz()),
                textoTag("KajiJDK Gray"), descTag("gris integrado") };
            int[] firmas = {
                ICC_Profile.icSigGrayTRCTag, ICC_Profile.icSigMediaWhitePointTag,
                ICC_Profile.icSigCopyrightTag, ICC_Profile.icSigProfileDescriptionTag };
            gray = ICC_Profile.getInstance(ICC_Profile.armar(
                    cabecera(ICC_Profile.icSigDisplayClass, ICC_Profile.icSigGrayData),
                    firmas, cuerpos, firmas.length));
        }
        return gray;
    }

    static synchronized ICC_Profile ciexyz() {
        if (xyz == null) {
            // Un perfil abstracto: su espacio de dispositivo ES el de conexion, asi que no lleva
            // matriz ni curvas -- no hay nada que convertir.
            byte[][] cuerpos = {
                xyzTag(D50), textoTag("KajiJDK CIEXYZ"), descTag("CIEXYZ integrado") };
            int[] firmas = {
                ICC_Profile.icSigMediaWhitePointTag, ICC_Profile.icSigCopyrightTag,
                ICC_Profile.icSigProfileDescriptionTag };
            xyz = ICC_Profile.getInstance(ICC_Profile.armar(
                    cabecera(ICC_Profile.icSigAbstractClass, ICC_Profile.icSigXYZData),
                    firmas, cuerpos, firmas.length));
        }
        return xyz;
    }

    /**
     * La cabecera de 128 bytes.
     *
     * <p>El tamano total queda en cero: lo escribe {@link ICC_Profile#armar} cuando ya sabe cuanto
     * ocupan las etiquetas.
     */
    private static byte[] cabecera(int claseDePerfil, int espacio) {
        byte[] h = new byte[128];
        escribirInt(h, ICC_Profile.icHdrCmmId, firma("Kaji"));
        // Version 2.4.0, que es la que declaran los perfiles del JDK.
        escribirInt(h, ICC_Profile.icHdrVersion, 0x02400000);
        escribirInt(h, ICC_Profile.icHdrDeviceClass, claseDePerfil);
        escribirInt(h, ICC_Profile.icHdrColorSpace, espacio);
        // El espacio de conexion es siempre XYZ acá: es el que las conversiones usan de eje.
        escribirInt(h, ICC_Profile.icHdrPcs, SIG_XYZ_TYPE);
        escribirInt(h, ICC_Profile.icHdrMagic, firma("acsp"));
        escribirInt(h, ICC_Profile.icHdrPlatform, 0);
        escribirInt(h, ICC_Profile.icHdrRenderingIntent, ICC_Profile.icPerceptual);
        // El iluminante de la cabecera lo fija el estandar en D50, no en el blanco del medio.
        escribirXyz(h, ICC_Profile.icHdrIlluminant, D50);
        return h;
    }

    /** Una etiqueta `XYZType`: firma, reservado y tres s15Fixed16. */
    private static byte[] xyzTag(float[] v) {
        byte[] t = new byte[20];
        escribirInt(t, 0, SIG_XYZ_TYPE);
        escribirXyz(t, 8, v);
        return t;
    }

    /** Una `curveType` de un solo valor: la gamma, en u8Fixed8. */
    private static byte[] curvaGamma(float gamma) {
        byte[] t = new byte[14];
        escribirInt(t, 0, SIG_CURVE_TYPE);
        escribirInt(t, ICC_Profile.icCurveCount, 1);
        escribirShort(t, ICC_Profile.icCurveData, (int) (gamma * 256.0f + 0.5f));
        return t;
    }

    /**
     * Una `curveType` como tabla de `n` puntos con la curva de sRGB.
     *
     * <p>La tabla va de lineal a con-gamma, que es el sentido en que ICC la define: la entrada `i`
     * es el valor codificado que le corresponde a la luminancia `i/(n-1)`.
     */
    private static byte[] curvaTabla(int n) {
        byte[] t = new byte[ICC_Profile.icCurveData + n * 2];
        escribirInt(t, 0, SIG_CURVE_TYPE);
        escribirInt(t, ICC_Profile.icCurveCount, n);
        for (int i = 0; i < n; i++) {
            float lineal = ((float) i) / (n - 1);
            float conGamma = ColorSpace.aGamma(lineal);
            int v = (int) (conGamma * 65535.0f + 0.5f);
            if (v < 0) {
                v = 0;
            }
            if (v > 65535) {
                v = 65535;
            }
            escribirShort(t, ICC_Profile.icCurveData + i * 2, v);
        }
        return t;
    }

    private static byte[] textoTag(String s) {
        byte[] t = new byte[8 + s.length() + 1];
        escribirInt(t, 0, SIG_TEXT_TYPE);
        for (int i = 0; i < s.length(); i++) {
            t[8 + i] = (byte) s.charAt(i);
        }
        return t;
    }

    /** Una `descType`: firma, reservado, largo con el nulo, y el texto. */
    private static byte[] descTag(String s) {
        byte[] t = new byte[12 + s.length() + 1 + 78];
        escribirInt(t, 0, SIG_DESC_TYPE);
        escribirInt(t, 8, s.length() + 1);
        for (int i = 0; i < s.length(); i++) {
            t[12 + i] = (byte) s.charAt(i);
        }
        return t;
    }

    private static void escribirXyz(byte[] b, int off, float[] v) {
        for (int i = 0; i < 3; i++) {
            escribirInt(b, off + i * 4, (int) (v[i] * 65536.0f + 0.5f));
        }
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
}
