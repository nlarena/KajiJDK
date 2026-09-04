package java.awt;

import java.awt.geom.AffineTransform;

/**
 * La base de los degradés de **varias paradas**.
 *
 * <p>Un {@link GradientPaint} va de un color a otro. Éstos van por una lista de colores, cada uno
 * anclado a una fracción del recorrido, y entre dos paradas se interpola. Es la diferencia entre un
 * degradé y un arcoíris.
 *
 * <p>Tres decisiones se declaran acá porque valen para todos:
 *
 * <ul>
 *   <li>el <strong>ciclo</strong>, que dice qué pasa más allá del recorrido: se estira el color del
 *       extremo, se repite el degradé, o se repite en espejo, que es la única de las tres que no
 *       deja una costura visible;
 *   <li>el <strong>espacio de color</strong> en el que se interpola. Interpolar en sRGB de negro a
 *       blanco da un medio más claro que el gris de verdad, porque sRGB no es lineal en luz;
 *       interpolar en RGB lineal da el gris físicamente correcto, que a veces se ve más sucio.
 *       Ninguna de las dos es la buena siempre, por eso se elige;
 *   <li>la <strong>transformación</strong> propia del degradé, que se compone con la del dibujado y
 *       permite rotar o estirar el degradé sin tocar la figura.
 * </ul>
 */
public abstract class MultipleGradientPaint implements Paint {

    /** Qué pasa fuera del recorrido del degradé. */
    public static enum CycleMethod {

        /** Se estira el color del extremo. */
        NO_CYCLE,

        /** El degradé se repite, con una costura en cada vuelta. */
        REFLECT,

        /** El degradé se repite en espejo, sin costura. */
        REPEAT
    }

    /** En qué espacio de color se interpola. */
    public static enum ColorSpaceType {

        /** En sRGB, tal como se ven los colores. */
        SRGB,

        /** En RGB lineal, proporcional a la luz. */
        LINEAR_RGB
    }

    final float[] fractions;
    final Color[] colors;
    final AffineTransform gradientTransform;
    final CycleMethod cycleMethod;
    final ColorSpaceType colorSpace;
    final int transparency;

    /**
     * Con las paradas, el ciclo, el espacio y la transformación.
     *
     * @throws NullPointerException si falta alguno de los cinco
     * @throws IllegalArgumentException si hay menos de dos paradas, si los arreglos no miden lo
     *     mismo, o si las fracciones no están en 0..1 y en orden estrictamente creciente
     */
    MultipleGradientPaint(float[] fractions, Color[] colors, CycleMethod cycleMethod,
            ColorSpaceType colorSpace, AffineTransform gradientTransform) {
        if (fractions == null) {
            throw new NullPointerException("Fractions array cannot be null");
        }
        if (colors == null) {
            throw new NullPointerException("Colors array cannot be null");
        }
        if (cycleMethod == null) {
            throw new NullPointerException("Cycle method cannot be null");
        }
        if (colorSpace == null) {
            throw new NullPointerException("Color space cannot be null");
        }
        if (gradientTransform == null) {
            throw new NullPointerException("Gradient transform cannot be null");
        }
        if (fractions.length != colors.length) {
            throw new IllegalArgumentException("Colors and fractions must have equal size");
        }
        if (colors.length < 2) {
            throw new IllegalArgumentException("User must specify at least 2 colors");
        }
        // Las fracciones tienen que crecer **estrictamente**: dos paradas en el mismo lugar serian
        // un salto de ancho cero y no hay forma de interpolar entre ellas.
        float previous = -1.0f;
        for (int i = 0; i < fractions.length; i++) {
            if (fractions[i] < 0.0f || fractions[i] > 1.0f) {
                throw new IllegalArgumentException(
                        "Fraction values must be in the range 0 to 1: " + fractions[i]);
            }
            if (fractions[i] <= previous) {
                throw new IllegalArgumentException("Keyframe fractions must be increasing: "
                        + fractions[i]);
            }
            previous = fractions[i];
            if (colors[i] == null) {
                throw new NullPointerException("Colors array cannot have null entries");
            }
        }
        this.fractions = fractions.clone();
        this.colors = colors.clone();
        this.cycleMethod = cycleMethod;
        this.colorSpace = colorSpace;
        this.gradientTransform = (AffineTransform) gradientTransform.clone();
        boolean opaco = true;
        for (int i = 0; i < colors.length; i++) {
            if (colors[i].getAlpha() != 0xFF) {
                opaco = false;
                break;
            }
        }
        this.transparency = opaco ? Transparency.OPAQUE : Transparency.TRANSLUCENT;
    }

    /** Dónde está cada parada, de 0 a 1. */
    public final float[] getFractions() {
        return this.fractions.clone();
    }

    /** El color de cada parada. */
    public final Color[] getColors() {
        return this.colors.clone();
    }

    /** Qué pasa fuera del recorrido. */
    public final CycleMethod getCycleMethod() {
        return this.cycleMethod;
    }

    /** En qué espacio se interpola. */
    public final ColorSpaceType getColorSpace() {
        return this.colorSpace;
    }

    /** La transformación propia del degradé. */
    public final AffineTransform getTransform() {
        return (AffineTransform) this.gradientTransform.clone();
    }

    /** `OPAQUE` si todas las paradas son opacas, `TRANSLUCENT` si alguna no. */
    public final int getTransparency() {
        return this.transparency;
    }

    /**
     * El color a la fracción `t` del recorrido, ya resuelto el ciclo.
     *
     * <p>Lo usan las dos subclases: lo único que cambia entre un degradé lineal y uno radial es cómo
     * se calcula `t`.
     */
    final int colorEn(float t) {
        float f = t;
        if (this.cycleMethod == CycleMethod.NO_CYCLE) {
            if (f < 0.0f) {
                f = 0.0f;
            } else if (f > 1.0f) {
                f = 1.0f;
            }
        } else {
            // El resto de dividir por 1 deja la parte fraccionaria; para los negativos hay que
            // sumarle uno, porque el resto de Java conserva el signo del dividendo.
            f = f - (float) Math.floor(f);
            if (this.cycleMethod == CycleMethod.REFLECT) {
                float doble = (t - (float) Math.floor(t / 2) * 2);
                if (doble > 1.0f) {
                    f = 2.0f - doble;
                } else {
                    f = doble;
                }
            }
        }
        int i = 0;
        while (i < this.fractions.length - 1 && f > this.fractions[i + 1]) {
            i = i + 1;
        }
        if (f <= this.fractions[0]) {
            return this.colors[0].getRGB();
        }
        if (f >= this.fractions[this.fractions.length - 1]) {
            return this.colors[this.colors.length - 1].getRGB();
        }
        float lo = this.fractions[i];
        float hi = this.fractions[i + 1];
        float u = (f - lo) / (hi - lo);
        return this.mezclar(this.colors[i], this.colors[i + 1], u);
    }

    /** Interpola dos colores, en el espacio que se haya declarado. */
    private int mezclar(Color a, Color b, float u) {
        int aa = a.getAlpha();
        int ab = b.getAlpha();
        int alfa = (int) (aa + (ab - aa) * u + 0.5f);
        if (this.colorSpace == ColorSpaceType.LINEAR_RGB) {
            float r = interpolarLineal(a.getRed(), b.getRed(), u);
            float g = interpolarLineal(a.getGreen(), b.getGreen(), u);
            float bl = interpolarLineal(a.getBlue(), b.getBlue(), u);
            return (alfa << 24) | (redondear(r) << 16) | (redondear(g) << 8) | redondear(bl);
        }
        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * u + 0.5f);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * u + 0.5f);
        int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * u + 0.5f);
        return (alfa << 24) | (r << 16) | (g << 8) | bl;
    }

    /** Interpola dos componentes sRGB pasando por luz lineal. */
    private static float interpolarLineal(int a, int b, float u) {
        double la = aLineal(a / 255.0);
        double lb = aLineal(b / 255.0);
        return (float) (aSrgb(la + (lb - la) * u) * 255.0);
    }

    /** De sRGB a luz lineal. */
    private static double aLineal(double c) {
        if (c <= 0.04045) {
            return c / 12.92;
        }
        return Math.pow((c + 0.055) / 1.055, 2.4);
    }

    /** De luz lineal a sRGB. */
    private static double aSrgb(double c) {
        if (c <= 0.0031308) {
            return c * 12.92;
        }
        return 1.055 * Math.pow(c, 1.0 / 2.4) - 0.055;
    }

    /** Un `float` de 0 a 255 llevado a un byte. */
    private static int redondear(float v) {
        int i = (int) (v + 0.5f);
        if (i < 0) {
            return 0;
        }
        if (i > 255) {
            return 255;
        }
        return i;
    }
}
