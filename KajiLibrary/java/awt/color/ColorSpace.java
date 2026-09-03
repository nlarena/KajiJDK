package java.awt.color;

import java.io.Serializable;

/**
 * Un espacio de color: cuántos componentes tiene, qué rango tiene cada uno, y cómo se convierte a
 * sRGB y a CIEXYZ.
 *
 * <p>CIEXYZ es el eje de todo el diseño y conviene decir por qué: es un espacio **absoluto**, atado
 * a cómo ve el ojo humano y no a ningún dispositivo. Cualquier par de espacios se convierte entre sí
 * pasando por él, y por eso todo espacio tiene que saber ir y volver de XYZ aunque no sepa nada de
 * los demás. `toRGB`/`fromRGB` existen aparte porque el camino a sRGB es el que más se usa y hacerlo
 * en dos pasos sería más caro y menos exacto.
 *
 * <h2>Los espacios estándar salen de perfiles ICC</h2>
 *
 * <p>{@link #getInstance} devuelve un {@link ICC_ColorSpace}, como el JDK: detrás de cada uno hay
 * un {@link ICC_Profile} de verdad, con su matriz, sus curvas y su punto blanco. La diferencia con
 * el JDK es de dónde salen los bytes del perfil — el JDK los trae como archivo de recurso y acá se
 * **construyen** a partir de las constantes del estándar. El resultado es un perfil ICC válido que
 * se puede escribir a un archivo y que otro programa lee.
 *
 * <p>La consecuencia observable es que los números difieren en los últimos dígitos: los dos caminos
 * cuantizan a 16 bits pero no en los mismos puntos, así que el `toRGB({0.5,0.5,0.5})` del JDK sobre
 * sRGB da `0.5000076` y el de acá otro valor igual de cercano a 0.5. Una prueba que compare bit a
 * bit contra el JDK va a fallar; las de esta casa comparan con tolerancia y verifican
 * **propiedades** — ida y vuelta, puntos conocidos — en vez de dígitos.
 *
 * <p><strong>Lo único que falta es {@link #CS_PYCC}</strong>, y el motivo es concreto: PhotoYCC no
 * se define por fórmulas sino por tablas de interpolación de 230 KB que viven dentro de su perfil.
 * Sin ese archivo no hay nada que construir, y armar un perfil vacío con su firma sería un objeto
 * que dice ser PhotoYCC y no convierte como PhotoYCC. `getInstance(CS_PYCC)` tira diciendo eso.
 */
public abstract class ColorSpace implements Serializable {

    private static final long serialVersionUID = -409452704308689724L;

    /** CIEXYZ. */
    public static final int TYPE_XYZ = 0;
    /** CIELab. */
    public static final int TYPE_Lab = 1;
    /** CIELuv. */
    public static final int TYPE_Luv = 2;
    /** YCbCr. */
    public static final int TYPE_YCbCr = 3;
    /** CIEYxy. */
    public static final int TYPE_Yxy = 4;
    /** RGB. */
    public static final int TYPE_RGB = 5;
    /** Escala de grises. */
    public static final int TYPE_GRAY = 6;
    /** HSV. */
    public static final int TYPE_HSV = 7;
    /** HLS. */
    public static final int TYPE_HLS = 8;
    /** CMYK. */
    public static final int TYPE_CMYK = 9;
    /** CMY. */
    public static final int TYPE_CMY = 11;
    /** Genérico de 2 componentes. */
    public static final int TYPE_2CLR = 12;
    /** Genérico de 3 componentes. */
    public static final int TYPE_3CLR = 13;
    /** Genérico de 4 componentes. */
    public static final int TYPE_4CLR = 14;
    /** Genérico de 5 componentes. */
    public static final int TYPE_5CLR = 15;
    /** Genérico de 6 componentes. */
    public static final int TYPE_6CLR = 16;
    /** Genérico de 7 componentes. */
    public static final int TYPE_7CLR = 17;
    /** Genérico de 8 componentes. */
    public static final int TYPE_8CLR = 18;
    /** Genérico de 9 componentes. */
    public static final int TYPE_9CLR = 19;
    /** Genérico de 10 componentes. */
    public static final int TYPE_ACLR = 20;
    /** Genérico de 11 componentes. */
    public static final int TYPE_BCLR = 21;
    /** Genérico de 12 componentes. */
    public static final int TYPE_CCLR = 22;
    /** Genérico de 13 componentes. */
    public static final int TYPE_DCLR = 23;
    /** Genérico de 14 componentes. */
    public static final int TYPE_ECLR = 24;
    /** Genérico de 15 componentes. */
    public static final int TYPE_FCLR = 25;

    /** El sRGB de siempre, con su curva de gamma. */
    public static final int CS_sRGB = 1000;
    /** RGB **lineal**: los mismos primarios que sRGB pero sin la curva. */
    public static final int CS_LINEAR_RGB = 1004;
    /** CIEXYZ con blanco D50, que es el que usa ICC. */
    public static final int CS_CIEXYZ = 1001;
    /** PhotoYCC. **No disponible acá**; ver la nota de la clase. */
    public static final int CS_PYCC = 1002;
    /** Escala de grises lineal. */
    public static final int CS_GRAY = 1003;

    // Los 20 y pico `TYPE_` que faltan (10 no existe: el JDK saltea el hueco entre CMYK y CMY) no
    // son un olvido -- el estándar tampoco define un tipo 10.

    private final int type;
    private final int numComponents;

    /**
     * Un espacio de ese tipo y con esa cantidad de componentes.
     *
     * @throws IllegalArgumentException si la cantidad de componentes es menor que 1
     */
    protected ColorSpace(int type, int numcomponents) {
        if (numcomponents < 1) {
            throw new IllegalArgumentException("numComponents < 1");
        }
        this.type = type;
        this.numComponents = numcomponents;
    }

    // Las instancias son únicas por identificador: `getInstance(CS_sRGB) == getInstance(CS_sRGB)`,
    // como en el JDK. Se crean tarde porque construir las cinco al cargar la clase costaría el
    // trabajo de las cuatro que nadie pidió.
    private static ColorSpace sRGBcs;
    private static ColorSpace linearRGBcs;
    private static ColorSpace xyzCS;
    private static ColorSpace grayCS;

    /**
     * Uno de los espacios estándar.
     *
     * @throws IllegalArgumentException si el identificador no es uno de los `CS_`, o si es
     *     {@link #CS_PYCC} — que existe como constante pero no como espacio en esta biblioteca
     */
    public static ColorSpace getInstance(int colorspace) {
        if (colorspace == CS_sRGB) {
            synchronized (ColorSpace.class) {
                if (sRGBcs == null) {
                    sRGBcs = new ICC_ColorSpace(ICC_Profile.getInstance(CS_sRGB));
                }
                return sRGBcs;
            }
        }
        if (colorspace == CS_LINEAR_RGB) {
            synchronized (ColorSpace.class) {
                if (linearRGBcs == null) {
                    linearRGBcs = new ICC_ColorSpace(ICC_Profile.getInstance(CS_LINEAR_RGB));
                }
                return linearRGBcs;
            }
        }
        if (colorspace == CS_CIEXYZ) {
            synchronized (ColorSpace.class) {
                if (xyzCS == null) {
                    xyzCS = new ICC_ColorSpace(ICC_Profile.getInstance(CS_CIEXYZ));
                }
                return xyzCS;
            }
        }
        if (colorspace == CS_GRAY) {
            synchronized (ColorSpace.class) {
                if (grayCS == null) {
                    grayCS = new ICC_ColorSpace(ICC_Profile.getInstance(CS_GRAY));
                }
                return grayCS;
            }
        }
        if (colorspace == CS_PYCC) {
            // PhotoYCC se define **como perfil ICC** y no por una fórmula: sin el archivo del
            // perfil no hay nada que calcular. Tirar es decir eso; devolver un sRGB disfrazado
            // sería el miembro que miente.
            throw new IllegalArgumentException(
                    "CS_PYCC no está disponible: hace falta su perfil ICC, que esta biblioteca "
                            + "no trae");
        }
        throw new IllegalArgumentException("Unknown color space");
    }

    /** Si es el sRGB estándar. */
    public boolean isCS_sRGB() {
        return this == sRGBcs;
    }

    /**
     * Este color, en sRGB.
     *
     * @param colorvalue los componentes en este espacio
     */
    public abstract float[] toRGB(float[] colorvalue);

    /**
     * Un color sRGB, en este espacio.
     *
     * @param rgbvalue los tres componentes sRGB
     */
    public abstract float[] fromRGB(float[] rgbvalue);

    /**
     * Este color, en CIEXYZ con blanco D50.
     *
     * <p>D50 y no D65 porque es lo que usa ICC, y con eso las conversiones encadenadas no necesitan
     * una adaptación cromática en el medio.
     */
    public abstract float[] toCIEXYZ(float[] colorvalue);

    /** Un color CIEXYZ (D50), en este espacio. */
    public abstract float[] fromCIEXYZ(float[] colorvalue);

    /** El `TYPE_` de este espacio. */
    public int getType() {
        return this.type;
    }

    /** Cuántos componentes tiene un color de este espacio. */
    public int getNumComponents() {
        return this.numComponents;
    }

    /**
     * El nombre del componente `idx`.
     *
     * <p>Por omisión, un nombre genérico. Las implementaciones que saben decir "Red" lo redefinen.
     *
     * @throws IllegalArgumentException si el índice no es un componente de este espacio
     */
    public String getName(int idx) {
        this.rangeCheck(idx);
        return "Unnamed color component(" + idx + ")";
    }

    /**
     * El valor mínimo del componente `idx`. Por omisión 0.
     *
     * @throws IllegalArgumentException si el índice no es un componente de este espacio
     */
    public float getMinValue(int component) {
        this.rangeCheck(component);
        return 0.0f;
    }

    /**
     * El valor máximo del componente `idx`. Por omisión 1.
     *
     * @throws IllegalArgumentException si el índice no es un componente de este espacio
     */
    public float getMaxValue(int component) {
        this.rangeCheck(component);
        return 1.0f;
    }

    // De paquete, como en el JDK: la usan las subclases de acá y no es API.
    final void rangeCheck(int component) {
        if (component < 0 || component > this.numComponents - 1) {
            throw new IllegalArgumentException(
                    "Component index out of range: " + component);
        }
    }

    // ---- la matemática compartida ----------------------------------------------------------
    //
    // Las matrices son las del perfil sRGB de ICC, adaptadas a D50 por Bradford. Están escritas y
    // no calculadas porque son constantes del estándar: recalcularlas en cada arranque sería
    // trabajo para llegar a los mismos números con menos dígitos.

    static final float[] RGB_A_XYZ = {
        0.4360747f, 0.3850649f, 0.1430804f,
        0.2225045f, 0.7168786f, 0.0606169f,
        0.0139322f, 0.0971045f, 0.7141733f };

    static final float[] XYZ_A_RGB = {
        3.1338561f, -1.6168667f, -0.4906146f,
        -0.9787684f, 1.9161415f, 0.0334540f,
        0.0719453f, -0.2289914f, 1.4052427f };

    /** El blanco D50, que es el punto blanco de ICC. */
    static final float[] BLANCO_D50 = { 0.9642f, 1.0f, 0.8249f };

    /**
     * El techo de un componente XYZ.
     *
     * <p>No es 2 sino `2 - 1/32768`: XYZ se codifica en ICC como punto fijo de 16 bits con el 1 en
     * 0x8000, así que el valor más grande representable es 0xFFFF/0x8000. El JDK contesta este
     * mismo número.
     */
    static final float XYZ_MAX = 1.0f + (32767.0f / 32768.0f);

    /** La curva de sRGB: de un componente con gamma a uno lineal (IEC 61966-2-1). */
    static float aLineal(float c) {
        if (c <= 0.04045f) {
            return c / 12.92f;
        }
        return (float) Math.pow((c + 0.055) / 1.055, 2.4);
    }

    /** La inversa: de lineal a sRGB. */
    static float aGamma(float c) {
        if (c <= 0.0031308f) {
            return c * 12.92f;
        }
        return (float) (1.055 * Math.pow(c, 1.0 / 2.4) - 0.055);
    }

    static float[] multiplicar(float[] m, float[] v) {
        float[] out = new float[3];
        out[0] = m[0] * v[0] + m[1] * v[1] + m[2] * v[2];
        out[1] = m[3] * v[0] + m[4] * v[1] + m[5] * v[2];
        out[2] = m[6] * v[0] + m[7] * v[1] + m[8] * v[2];
        return out;
    }

    static void exigir(float[] v, int n) {
        if (v == null) {
            throw new NullPointerException("el color no puede ser nulo");
        }
        if (v.length < n) {
            throw new ArrayIndexOutOfBoundsException(
                    "el color necesita " + n + " componentes y tiene " + v.length);
        }
    }
}
