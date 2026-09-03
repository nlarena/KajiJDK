package java.awt.color;

/**
 * Un espacio de color definido por un perfil ICC.
 *
 * <p>Es lo que {@link ColorSpace#getInstance} devuelve para los cinco espacios estandar, y lo que
 * hay que usar para un perfil propio: `new ICC_ColorSpace(ICC_Profile.getInstance("miperfil.icc"))`.
 *
 * <p><strong>Como convierte, y que no hace.</strong> Un motor de gestion de color completo aplica
 * el perfil entero, tablas de interpolacion incluidas. Aca la conversion se hace con la **matriz y
 * las curvas** del perfil, que es exactamente lo correcto para un perfil de matriz --los cinco
 * integrados y la enorme mayoria de los perfiles de monitor lo son-- y no alcanza para uno basado
 * en tablas, como los de impresora CMYK.
 *
 * <p>Un perfil de tablas se puede **leer** igual --{@link ICC_Profile} lo parsea entero-- pero
 * pedirle una conversion a este espacio tira {@link ProfileDataException} en vez de devolver un
 * numero inventado. Es la diferencia entre no tener un motor de tablas y fingir que se tiene.
 */
public class ICC_ColorSpace extends ColorSpace {

    private static final long serialVersionUID = 3455889114070431483L;

    private final ICC_Profile profile;

    // La matriz y su inversa se calculan una vez: convertir un pixel es la operacion mas repetida
    // de todo esto, y recalcular la inversa en cada llamada seria pagar un determinante por pixel.
    private transient float[] aXyz;
    private transient float[] desdeXyz;
    private transient short[][] curvas;
    private transient float[] gammas;

    /**
     * El espacio que describe ese perfil.
     *
     * @throws IllegalArgumentException si el perfil es nulo, o si su espacio no coincide con la
     *     cantidad de componentes que declara
     */
    public ICC_ColorSpace(ICC_Profile profile) {
        super(tipoDe(profile), profile.getNumComponents());
        this.profile = profile;
    }

    private static int tipoDe(ICC_Profile p) {
        if (p == null) {
            throw new IllegalArgumentException("el perfil no puede ser nulo");
        }
        return p.getColorSpaceType();
    }

    /** El perfil de este espacio. */
    public ICC_Profile getProfile() {
        return this.profile;
    }

    /**
     * El techo de un componente.
     *
     * <p>1 para casi todo; para XYZ el `2 - 1/32768` que la codificacion de ICC permite.
     */
    public float getMaxValue(int component) {
        this.rangeCheck(component);
        if (this.getType() == TYPE_XYZ) {
            return XYZ_MAX;
        }
        return 1.0f;
    }

    /** El piso de un componente: siempre 0 en los espacios que esta clase maneja. */
    public float getMinValue(int component) {
        this.rangeCheck(component);
        return 0.0f;
    }

    public String getName(int idx) {
        this.rangeCheck(idx);
        int t = this.getType();
        if (t == TYPE_RGB) {
            String[] n = { "Red", "Green", "Blue" };
            return n[idx];
        }
        if (t == TYPE_GRAY) {
            return "Gray";
        }
        if (t == TYPE_XYZ) {
            String[] n = { "X", "Y", "Z" };
            return n[idx];
        }
        return super.getName(idx);
    }

    public float[] toCIEXYZ(float[] colorvalue) {
        int t = this.getType();
        if (t == TYPE_XYZ) {
            exigir(colorvalue, 3);
            return new float[] { colorvalue[0], colorvalue[1], colorvalue[2] };
        }
        if (t == TYPE_GRAY) {
            exigir(colorvalue, 1);
            float y = this.aLinealComponente(0, colorvalue[0]);
            float[] blanco = this.profile.getMediaWhitePoint();
            return new float[] { blanco[0] * y, y, blanco[2] * y };
        }
        if (t == TYPE_RGB) {
            exigir(colorvalue, 3);
            float[] lineal = new float[3];
            for (int i = 0; i < 3; i++) {
                lineal[i] = this.aLinealComponente(i, colorvalue[i]);
            }
            return multiplicar(this.matrizAXyz(), lineal);
        }
        throw new ProfileDataException(
                "este perfil no es de matriz y curvas: la conversion necesita un motor de tablas, "
                        + "que esta biblioteca no tiene");
    }

    public float[] fromCIEXYZ(float[] colorvalue) {
        int t = this.getType();
        if (t == TYPE_XYZ) {
            exigir(colorvalue, 3);
            return new float[] { colorvalue[0], colorvalue[1], colorvalue[2] };
        }
        if (t == TYPE_GRAY) {
            exigir(colorvalue, 3);
            return new float[] { this.desdeLinealComponente(0, colorvalue[1]) };
        }
        if (t == TYPE_RGB) {
            exigir(colorvalue, 3);
            float[] lineal = multiplicar(this.matrizDesdeXyz(), colorvalue);
            float[] out = new float[3];
            for (int i = 0; i < 3; i++) {
                out[i] = this.desdeLinealComponente(i, lineal[i]);
            }
            return out;
        }
        throw new ProfileDataException(
                "este perfil no es de matriz y curvas: la conversion necesita un motor de tablas, "
                        + "que esta biblioteca no tiene");
    }

    /**
     * A sRGB.
     *
     * <p>Pasa por CIEXYZ, salvo que este espacio ya sea sRGB. Es un paso mas que el camino directo
     * y a cambio no hay una segunda implementacion que pueda contradecir a la primera.
     */
    public float[] toRGB(float[] colorvalue) {
        ColorSpace srgb = ColorSpace.getInstance(CS_sRGB);
        if (this == srgb) {
            exigir(colorvalue, 3);
            return new float[] { colorvalue[0], colorvalue[1], colorvalue[2] };
        }
        return srgb.fromCIEXYZ(this.toCIEXYZ(colorvalue));
    }

    /** Desde sRGB. Ver {@link #toRGB}. */
    public float[] fromRGB(float[] rgbvalue) {
        ColorSpace srgb = ColorSpace.getInstance(CS_sRGB);
        if (this == srgb) {
            exigir(rgbvalue, 3);
            return new float[] { rgbvalue[0], rgbvalue[1], rgbvalue[2] };
        }
        return this.fromCIEXYZ(srgb.toCIEXYZ(rgbvalue));
    }

    // ---- las curvas del perfil -----------------------------------------------------------------
    //
    // Cada componente tiene su curva, guardada como gamma o como tabla. Se leen una vez y se
    // guardan: `getTRC` copia el arreglo en cada llamada, y hacerlo por pixel seria absurdo.

    private void cargarCurvas() {
        if (this.curvas != null) {
            return;
        }
        int n = this.getNumComponents();
        short[][] tablas = new short[n][];
        float[] gs = new float[n];
        int[] tags = this.getType() == TYPE_GRAY
                ? new int[] { ICC_Profile.icSigGrayTRCTag }
                : new int[] { ICC_Profile.icSigRedTRCTag, ICC_Profile.icSigGreenTRCTag,
                    ICC_Profile.icSigBlueTRCTag };
        for (int i = 0; i < n && i < tags.length; i++) {
            try {
                gs[i] = this.profile.gammaOfTag(tags[i]);
                tablas[i] = null;
            } catch (ProfileDataException e) {
                tablas[i] = this.profile.trcOfTag(tags[i]);
                gs[i] = 0.0f;
            }
        }
        this.gammas = gs;
        this.curvas = tablas;
    }

    /** De valor codificado a lineal, con la curva de ese componente. */
    private float aLinealComponente(int i, float v) {
        this.cargarCurvas();
        if (this.curvas[i] == null) {
            if (this.gammas[i] == 1.0f) {
                return v;
            }
            return (float) Math.pow(v, this.gammas[i]);
        }
        // La tabla va de lineal a codificado, asi que para el otro sentido hay que buscarla al
        // reves. Se hace con busqueda binaria y una interpolacion, que es lo que da una inversa
        // continua sin guardar una segunda tabla.
        short[] t = this.curvas[i];
        int objetivo = (int) (v * 65535.0f + 0.5f);
        int lo = 0;
        int hi = t.length - 1;
        while (lo < hi - 1) {
            int med = (lo + hi) / 2;
            if ((t[med] & 0xFFFF) <= objetivo) {
                lo = med;
            } else {
                hi = med;
            }
        }
        int a = t[lo] & 0xFFFF;
        int b = t[hi] & 0xFFFF;
        float frac = b == a ? 0.0f : ((float) (objetivo - a)) / (b - a);
        return (lo + frac) / (t.length - 1);
    }

    /** De lineal a valor codificado. */
    private float desdeLinealComponente(int i, float v) {
        this.cargarCurvas();
        if (this.curvas[i] == null) {
            if (this.gammas[i] == 1.0f) {
                return v;
            }
            return (float) Math.pow(v, 1.0 / this.gammas[i]);
        }
        short[] t = this.curvas[i];
        float pos = v * (t.length - 1);
        if (pos <= 0) {
            return (t[0] & 0xFFFF) / 65535.0f;
        }
        if (pos >= t.length - 1) {
            return (t[t.length - 1] & 0xFFFF) / 65535.0f;
        }
        int lo = (int) pos;
        float frac = pos - lo;
        int a = t[lo] & 0xFFFF;
        int b = t[lo + 1] & 0xFFFF;
        return (a + (b - a) * frac) / 65535.0f;
    }

    private float[] matrizAXyz() {
        if (this.aXyz == null) {
            float[] r = this.profile.getXYZTag(ICC_Profile.icSigRedColorantTag);
            float[] g = this.profile.getXYZTag(ICC_Profile.icSigGreenColorantTag);
            float[] b = this.profile.getXYZTag(ICC_Profile.icSigBlueColorantTag);
            this.aXyz = new float[] {
                r[0], g[0], b[0],
                r[1], g[1], b[1],
                r[2], g[2], b[2] };
        }
        return this.aXyz;
    }

    private float[] matrizDesdeXyz() {
        if (this.desdeXyz == null) {
            this.desdeXyz = invertir(this.matrizAXyz());
        }
        return this.desdeXyz;
    }

    /**
     * La inversa de una matriz de 3x3, por cofactores.
     *
     * @throws ProfileDataException si es singular -- un perfil cuyos tres primarios son coplanares
     *     no describe ningun espacio, y no hay forma de volver de XYZ
     */
    private static float[] invertir(float[] m) {
        float det = m[0] * (m[4] * m[8] - m[5] * m[7])
                - m[1] * (m[3] * m[8] - m[5] * m[6])
                + m[2] * (m[3] * m[7] - m[4] * m[6]);
        if (det == 0.0f) {
            throw new ProfileDataException("la matriz del perfil no se puede invertir");
        }
        float[] out = new float[9];
        out[0] = (m[4] * m[8] - m[5] * m[7]) / det;
        out[1] = (m[2] * m[7] - m[1] * m[8]) / det;
        out[2] = (m[1] * m[5] - m[2] * m[4]) / det;
        out[3] = (m[5] * m[6] - m[3] * m[8]) / det;
        out[4] = (m[0] * m[8] - m[2] * m[6]) / det;
        out[5] = (m[2] * m[3] - m[0] * m[5]) / det;
        out[6] = (m[3] * m[7] - m[4] * m[6]) / det;
        out[7] = (m[1] * m[6] - m[0] * m[7]) / det;
        out[8] = (m[0] * m[4] - m[1] * m[3]) / det;
        return out;
    }
}
