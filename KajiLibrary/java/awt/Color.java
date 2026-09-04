package java.awt;

/**
 * Un color en sRGB con alfa, empaquetado en un solo entero.
 *
 * <p>Todo el estado son 32 bits: {@code 0xAARRGGBB}. Que sea un entero y no cuatro campos no es un
 * detalle de implementacion sino API: {@code getRGB()} devuelve exactamente ese entero,
 * {@code hashCode()} tambien, y el orden de los canales es el que espera cualquier buffer de video.
 *
 * <h2>Los flotantes se guardan aparte, y hay una razon</h2>
 *
 * <p>Un color construido con flotantes --{@code new Color(0.1f, 0.2f, 0.3f)}-- guarda ademas los
 * tres flotantes originales. Parece redundante y no lo es: pasar por enteros pierde precision, y
 * si {@code getRGBColorComponents()} devolviera {@code getRed()/255f} el valor que sale no seria el
 * que entro. Con los flotantes guardados, el que construyo con flotantes los recupera intactos y el
 * que construyo con enteros recibe la division; las dos respuestas son exactas para su origen.
 *
 * <h2>El espacio de color</h2>
 *
 * <p>Un Color construido por cualquiera de los constructores de enteros o flotantes es sRGB. El
 * constructor {@code Color(ColorSpace, float[], float)} permite otro, y ahi los componentes que se
 * guardan son los del espacio dado --no los de sRGB--: {@code getColorComponents(null)} devuelve lo
 * que se paso, y {@code getRed()} devuelve la conversion a sRGB. Esa asimetria es del contrato y es
 * lo que hace que un color en escala de grises conserve su unico componente en vez de degradarse a
 * tres.
 *
 * <p>Como {@link Paint}, es el caso degenerado: {@code createContext} devuelve un contexto que
 * contesta el mismo color en todos los puntos. Eso es lo que hace que dibujar con un color y dibujar
 * con un degrade sean la misma operacion para quien dibuja.
 */
public class Color implements Paint, java.io.Serializable {

    private static final long serialVersionUID = 118526816881161077L;

    public static final Color white = new Color(255, 255, 255);

    public static final Color WHITE = white;

    public static final Color lightGray = new Color(192, 192, 192);

    public static final Color LIGHT_GRAY = lightGray;

    public static final Color gray = new Color(128, 128, 128);

    public static final Color GRAY = gray;

    public static final Color darkGray = new Color(64, 64, 64);

    public static final Color DARK_GRAY = darkGray;

    public static final Color black = new Color(0, 0, 0);

    public static final Color BLACK = black;

    public static final Color red = new Color(255, 0, 0);

    public static final Color RED = red;

    /** No es rojo claro: 255,175,175. El azul acompania al verde para que no vire a naranja. */
    public static final Color pink = new Color(255, 175, 175);

    public static final Color PINK = pink;

    /** 255,200,0 y no 255,165,0: el naranja del AWT es mas amarillo que el "orange" de la web. */
    public static final Color orange = new Color(255, 200, 0);

    public static final Color ORANGE = orange;

    public static final Color yellow = new Color(255, 255, 0);

    public static final Color YELLOW = yellow;

    public static final Color green = new Color(0, 255, 0);

    public static final Color GREEN = green;

    public static final Color magenta = new Color(255, 0, 255);

    public static final Color MAGENTA = magenta;

    public static final Color cyan = new Color(0, 255, 255);

    public static final Color CYAN = cyan;

    public static final Color blue = new Color(0, 0, 255);

    public static final Color BLUE = blue;

    /** 0xAARRGGBB. Es el estado entero de la clase. */
    int value;

    /**
     * Los tres flotantes con los que se construyo, o null si se construyo con enteros. Es
     * transitorio a proposito: al deserializar solo llega {@code value}, y volver a fabricarlos
     * dividiendo daria numeros que nunca fueron los originales.
     */
    private transient float[] frgbvalue;

    private transient float falpha;

    /**
     * Los componentes en el espacio propio, o null si este color es sRGB.
     *
     * <p>Es distinto de {@code frgbvalue}: aquel son siempre tres numeros en sRGB y este tiene
     * tantos como el espacio diga. Un gris tiene uno solo, y guardarlo convertido a tres lo
     * volveria irrecuperable.
     */
    private transient float[] fvalue;

    /** El espacio de este color, o null mientras nadie lo haya pedido y sea sRGB. */
    private transient java.awt.color.ColorSpace cs;

    /**
     * Al aclarar, los canales en cero no se mueven --dividir cero por 0.7 sigue dando cero-- asi
     * que un negro puro nunca se aclararia. Por eso hay un piso: un canal entre 1 y 2 se sube a 3
     * antes de dividir, y el negro entero se convierte en (3,3,3). Sin ese piso,
     * {@code brighter()} aplicado muchas veces sobre un gris muy oscuro se quedaria quieto.
     */
    private static final double FACTOR = 0.7;

    public Color(int r, int g, int b) {
        this(r, g, b, 255);
    }

    public Color(int r, int g, int b, int a) {
        value = ((a & 0xFF) << 24)
                | ((r & 0xFF) << 16)
                | ((g & 0xFF) << 8)
                | ((b & 0xFF) << 0);
        testColorValueRange(r, g, b, a);
    }

    /** Los 8 bits de arriba se ignoran: este constructor siempre da un color opaco. */
    public Color(int rgb) {
        value = 0xff000000 | rgb;
    }

    public Color(int rgba, boolean hasalpha) {
        if (hasalpha) {
            value = rgba;
        } else {
            value = 0xff000000 | rgba;
        }
    }

    public Color(float r, float g, float b) {
        this((int) (r * 255 + 0.5), (int) (g * 255 + 0.5), (int) (b * 255 + 0.5));
        testColorValueRange(r, g, b, 1.0f);
        frgbvalue = new float[3];
        frgbvalue[0] = r;
        frgbvalue[1] = g;
        frgbvalue[2] = b;
        falpha = 1.0f;
    }

    public Color(float r, float g, float b, float a) {
        this((int) (r * 255 + 0.5), (int) (g * 255 + 0.5), (int) (b * 255 + 0.5),
                (int) (a * 255 + 0.5));
        frgbvalue = new float[3];
        frgbvalue[0] = r;
        frgbvalue[1] = g;
        frgbvalue[2] = b;
        falpha = a;
    }

    /**
     * El mensaje enumera <b>todos</b> los canales fuera de rango, no el primero: quien pasa mal el
     * rojo y el azul suele haberse equivocado en la conversion entera, y ver los dos ahorra un
     * segundo viaje.
     */
    private static void testColorValueRange(int r, int g, int b, int a) {
        boolean rangeError = false;
        String badComponentString = "";

        if (a < 0 || a > 255) {
            rangeError = true;
            badComponentString = badComponentString + " Alpha";
        }
        if (r < 0 || r > 255) {
            rangeError = true;
            badComponentString = badComponentString + " Red";
        }
        if (g < 0 || g > 255) {
            rangeError = true;
            badComponentString = badComponentString + " Green";
        }
        if (b < 0 || b > 255) {
            rangeError = true;
            badComponentString = badComponentString + " Blue";
        }
        if (rangeError) {
            throw new IllegalArgumentException(
                    "Color parameter outside of expected range:" + badComponentString);
        }
    }

    private static void testColorValueRange(float r, float g, float b, float a) {
        boolean rangeError = false;
        String badComponentString = "";
        if (a < 0.0 || a > 1.0) {
            rangeError = true;
            badComponentString = badComponentString + " Alpha";
        }
        if (r < 0.0 || r > 1.0) {
            rangeError = true;
            badComponentString = badComponentString + " Red";
        }
        if (g < 0.0 || g > 1.0) {
            rangeError = true;
            badComponentString = badComponentString + " Green";
        }
        if (b < 0.0 || b > 1.0) {
            rangeError = true;
            badComponentString = badComponentString + " Blue";
        }
        if (rangeError) {
            throw new IllegalArgumentException(
                    "Color parameter outside of expected range:" + badComponentString);
        }
    }

    public int getRed() {
        return (getRGB() >> 16) & 0xFF;
    }

    public int getGreen() {
        return (getRGB() >> 8) & 0xFF;
    }

    public int getBlue() {
        return (getRGB() >> 0) & 0xFF;
    }

    public int getAlpha() {
        return (getRGB() >> 24) & 0xff;
    }

    public int getRGB() {
        return value;
    }

    public Color brighter() {
        int r = getRed();
        int g = getGreen();
        int b = getBlue();
        int alpha = getAlpha();

        int i = (int) (1.0 / (1.0 - FACTOR));
        if (r == 0 && g == 0 && b == 0) {
            return new Color(i, i, i, alpha);
        }
        if (r > 0 && r < i) {
            r = i;
        }
        if (g > 0 && g < i) {
            g = i;
        }
        if (b > 0 && b < i) {
            b = i;
        }

        return new Color(Math.min((int) (r / FACTOR), 255),
                Math.min((int) (g / FACTOR), 255),
                Math.min((int) (b / FACTOR), 255),
                alpha);
    }

    /** Oscurecer no necesita piso: multiplicar por 0.7 siempre baja, y el cero ya es el fondo. */
    public Color darker() {
        return new Color(Math.max((int) (getRed() * FACTOR), 0),
                Math.max((int) (getGreen() * FACTOR), 0),
                Math.max((int) (getBlue() * FACTOR), 0),
                getAlpha());
    }

    public int hashCode() {
        return value;
    }

    /** El alfa cuenta: {@code getRGB()} lo incluye, asi que dos colores con distinto alfa difieren. */
    public boolean equals(Object obj) {
        return obj instanceof Color && ((Color) obj).getRGB() == this.getRGB();
    }

    /** El alfa no se imprime, ni siquiera cuando no es 255. Es asi desde 1.1 y no se puede cambiar. */
    public String toString() {
        return getClass().getName() + "[r=" + getRed() + ",g=" + getGreen() + ",b=" + getBlue()
                + "]";
    }

    /**
     * Acepta lo mismo que {@code Integer.decode}: "#RRGGBB", "0xRRGGBB", "0RRGGBB" en octal y un
     * decimal pelado. El alfa que venga en los 8 bits de arriba se descarta.
     */
    public static Color decode(String nm) throws NumberFormatException {
        Integer intval = Integer.decode(nm);
        int i = intval.intValue();
        return new Color((i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF);
    }

    public static Color getColor(String nm) {
        return getColor(nm, null);
    }

    public static Color getColor(String nm, Color v) {
        Integer intval = Integer.getInteger(nm);
        if (intval == null) {
            return v;
        }
        int i = intval.intValue();
        return new Color((i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF);
    }

    public static Color getColor(String nm, int v) {
        Integer intval = Integer.getInteger(nm);
        int i = (intval != null) ? intval.intValue() : v;
        return new Color((i >> 16) & 0xFF, (i >> 8) & 0xFF, (i >> 0) & 0xFF);
    }

    /**
     * Tono, saturacion y brillo a RGB.
     *
     * <p>El tono se toma modulo 1 --{@code hue - floor(hue)}-- asi que 1.25 y 0.25 dan el mismo
     * color y un tono negativo tambien funciona: es un angulo, no una fraccion acotada.
     */
    public static int HSBtoRGB(float hue, float saturation, float brightness) {
        int r = 0;
        int g = 0;
        int b = 0;
        if (saturation == 0) {
            r = (int) (brightness * 255.0f + 0.5f);
            g = r;
            b = r;
        } else {
            float h = (hue - (float) Math.floor(hue)) * 6.0f;
            float f = h - (float) Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
                case 0:
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (t * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                    break;
                case 1:
                    r = (int) (q * 255.0f + 0.5f);
                    g = (int) (brightness * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                    break;
                case 2:
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (brightness * 255.0f + 0.5f);
                    b = (int) (t * 255.0f + 0.5f);
                    break;
                case 3:
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (q * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                    break;
                case 4:
                    r = (int) (t * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                    break;
                case 5:
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (q * 255.0f + 0.5f);
                    break;
                default:
                    break;
            }
        }
        return 0xff000000 | (r << 16) | (g << 8) | (b << 0);
    }

    /**
     * RGB a tono, saturacion y brillo.
     *
     * <p>Cuando la saturacion es cero el tono queda en 0 por convencion: un gris no tiene tono, y
     * cualquier otro valor seria inventado.
     */
    public static float[] RGBtoHSB(int r, int g, int b, float[] hsbvals) {
        float hue;
        float saturation;
        float brightness;
        if (hsbvals == null) {
            hsbvals = new float[3];
        }
        int cmax = (r > g) ? r : g;
        if (b > cmax) {
            cmax = b;
        }
        int cmin = (r < g) ? r : g;
        if (b < cmin) {
            cmin = b;
        }

        brightness = ((float) cmax) / 255.0f;
        if (cmax != 0) {
            saturation = ((float) (cmax - cmin)) / ((float) cmax);
        } else {
            saturation = 0;
        }
        if (saturation == 0) {
            hue = 0;
        } else {
            float redc = ((float) (cmax - r)) / ((float) (cmax - cmin));
            float greenc = ((float) (cmax - g)) / ((float) (cmax - cmin));
            float bluec = ((float) (cmax - b)) / ((float) (cmax - cmin));
            if (r == cmax) {
                hue = bluec - greenc;
            } else if (g == cmax) {
                hue = 2.0f + redc - bluec;
            } else {
                hue = 4.0f + greenc - redc;
            }
            hue = hue / 6.0f;
            if (hue < 0) {
                hue = hue + 1.0f;
            }
        }
        hsbvals[0] = hue;
        hsbvals[1] = saturation;
        hsbvals[2] = brightness;
        return hsbvals;
    }

    public static Color getHSBColor(float h, float s, float b) {
        return new Color(HSBtoRGB(h, s, b));
    }

    public float[] getRGBComponents(float[] compArray) {
        float[] f;
        if (compArray == null) {
            f = new float[4];
        } else {
            f = compArray;
        }
        if (frgbvalue == null) {
            f[0] = ((float) getRed()) / 255f;
            f[1] = ((float) getGreen()) / 255f;
            f[2] = ((float) getBlue()) / 255f;
            f[3] = ((float) getAlpha()) / 255f;
        } else {
            f[0] = frgbvalue[0];
            f[1] = frgbvalue[1];
            f[2] = frgbvalue[2];
            f[3] = falpha;
        }
        return f;
    }

    public float[] getRGBColorComponents(float[] compArray) {
        float[] f;
        if (compArray == null) {
            f = new float[3];
        } else {
            f = compArray;
        }
        if (frgbvalue == null) {
            f[0] = ((float) getRed()) / 255f;
            f[1] = ((float) getGreen()) / 255f;
            f[2] = ((float) getBlue()) / 255f;
        } else {
            f[0] = frgbvalue[0];
            f[1] = frgbvalue[1];
            f[2] = frgbvalue[2];
        }
        return f;
    }

    /**
     * Los componentes del espacio de este color, mas el alfa al final.
     *
     * <p>Para un color sRGB --la mayoria-- es exactamente {@code getRGBComponents}. Para uno
     * construido con otro espacio son los de **ese** espacio, y pueden no ser tres: un color en
     * escala de grises devuelve dos numeros, el gris y el alfa.
     */
    public float[] getComponents(float[] compArray) {
        if (this.fvalue == null) {
            return this.getRGBComponents(compArray);
        }
        float[] f;
        if (compArray == null) {
            f = new float[this.fvalue.length + 1];
        } else {
            f = compArray;
        }
        for (int i = 0; i < this.fvalue.length; i++) {
            f[i] = this.fvalue[i];
        }
        f[this.fvalue.length] = this.falpha;
        return f;
    }

    /** Los componentes del espacio de este color, sin el alfa. Ver {@link #getComponents}. */
    public float[] getColorComponents(float[] compArray) {
        if (this.fvalue == null) {
            return this.getRGBColorComponents(compArray);
        }
        float[] f;
        if (compArray == null) {
            f = new float[this.fvalue.length];
        } else {
            f = compArray;
        }
        for (int i = 0; i < this.fvalue.length; i++) {
            f[i] = this.fvalue[i];
        }
        return f;
    }

    /**
     * Un alfa de 0 da BITMASK y no TRANSLUCENT: el color es completamente invisible, asi que quien
     * compone puede saltearse la mezcla en vez de multiplicar por cero pixel por pixel.
     */
    public int getTransparency() {
        int alpha = getAlpha();
        if (alpha == 0xff) {
            return Transparency.OPAQUE;
        } else if (alpha == 0) {
            return Transparency.BITMASK;
        } else {
            return Transparency.TRANSLUCENT;
        }
    }

    /**
     * Un color en el espacio dado.
     *
     * <p>Los componentes se guardan **en ese espacio**, no convertidos a sRGB: eso es lo que hace
     * que {@code getColorComponents(null)} los devuelva intactos. Lo que si se convierte, y una
     * sola vez aca, es el valor sRGB empaquetado que devuelven {@code getRed()} y compania -- de
     * otro modo cada llamada pagaria la conversion.
     *
     * @throws NullPointerException si el espacio o los componentes son nulos
     * @throws IllegalArgumentException si sobran o faltan componentes para ese espacio, si alguno
     *     cae fuera del rango que el espacio declara, o si el alfa no esta entre 0 y 1
     */
    public Color(java.awt.color.ColorSpace cspace, float[] components, float alpha) {
        if (cspace == null) {
            throw new NullPointerException("el espacio de color no puede ser nulo");
        }
        if (components == null) {
            throw new NullPointerException("los componentes no pueden ser nulos");
        }
        int n = cspace.getNumComponents();
        // Un arreglo mas corto de lo que el espacio pide **no** se comprueba: se recorre y salta
        // `ArrayIndexOutOfBoundsException` sola. Es lo que hace el JDK 25 --comprobado-- y aunque
        // un `IllegalArgumentException` seria mas informativo, cambiarlo rompe a quien atrape la
        // que el contrato produce. Uno mas largo se acepta y sobra lo de mas.
        boolean rangeError = false;
        StringBuilder badComponentString = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (components[i] < cspace.getMinValue(i) || components[i] > cspace.getMaxValue(i)) {
                rangeError = true;
                badComponentString.append(" Component ").append(i);
            }
        }
        if (alpha < 0.0f || alpha > 1.0f) {
            rangeError = true;
            badComponentString.append(" Alpha");
        }
        if (rangeError) {
            throw new IllegalArgumentException(
                    "Color parameter outside of expected range:" + badComponentString.toString());
        }
        this.fvalue = new float[n];
        for (int i = 0; i < n; i++) {
            this.fvalue[i] = components[i];
        }
        this.falpha = alpha;
        this.cs = cspace;
        // El sRGB empaquetado sale de la conversion del espacio; el redondeo es el mismo `+0.5`
        // que usan los constructores de flotantes.
        float[] rgb = cspace.toRGB(this.fvalue);
        this.frgbvalue = new float[] { rgb[0], rgb[1], rgb[2] };
        this.value = ((((int) (alpha * 255 + 0.5)) & 0xFF) << 24)
                | ((((int) (rgb[0] * 255 + 0.5)) & 0xFF) << 16)
                | ((((int) (rgb[1] * 255 + 0.5)) & 0xFF) << 8)
                | (((int) (rgb[2] * 255 + 0.5)) & 0xFF);
    }

    /** El espacio de este color; sRGB salvo que se haya construido con otro. */
    public java.awt.color.ColorSpace getColorSpace() {
        if (this.cs == null) {
            this.cs = java.awt.color.ColorSpace.getInstance(
                    java.awt.color.ColorSpace.CS_sRGB);
        }
        return this.cs;
    }

    /**
     * Los componentes de este color **en el espacio pedido**, mas el alfa al final.
     *
     * @throws NullPointerException si el espacio es nulo
     */
    public float[] getComponents(java.awt.color.ColorSpace cspace, float[] compArray) {
        if (cspace == null) {
            throw new NullPointerException("el espacio de color no puede ser nulo");
        }
        float[] color = this.getColorComponents(cspace, null);
        float[] f;
        if (compArray == null) {
            f = new float[color.length + 1];
        } else {
            f = compArray;
        }
        for (int i = 0; i < color.length; i++) {
            f[i] = color[i];
        }
        f[color.length] = this.getAlpha() / 255f;
        if (this.fvalue != null) {
            f[color.length] = this.falpha;
        }
        return f;
    }

    /**
     * Los componentes de este color en el espacio pedido, **sin** el alfa.
     *
     * <p>La conversion pasa por CIEXYZ, que es como se convierte entre dos espacios cualesquiera:
     * de este espacio a XYZ y de XYZ al pedido. Si el pedido es el propio, se devuelven los
     * componentes tal cual y no se convierte nada -- ida y vuelta por XYZ perderia precision sin
     * ganar nada.
     *
     * @throws NullPointerException si el espacio es nulo
     */
    public float[] getColorComponents(java.awt.color.ColorSpace cspace, float[] compArray) {
        if (cspace == null) {
            throw new NullPointerException("el espacio de color no puede ser nulo");
        }
        float[] propios = this.getColorComponents(null);
        float[] convertidos;
        if (cspace == this.getColorSpace()) {
            convertidos = propios;
        } else {
            convertidos = cspace.fromCIEXYZ(this.getColorSpace().toCIEXYZ(propios));
        }
        float[] f;
        if (compArray == null) {
            f = new float[convertidos.length];
        } else {
            f = compArray;
        }
        for (int i = 0; i < convertidos.length; i++) {
            f[i] = convertidos[i];
        }
        return f;
    }

    /**
     * Arma la maquina que genera los pixeles: la mas simple de todas.
     *
     * <p>Un color plano contesta lo mismo en todos los puntos, asi que el contexto no necesita ni
     * invertir la transformacion ni mirar las coordenadas.
     */
    public java.awt.PaintContext createContext(java.awt.image.ColorModel cm,
            java.awt.Rectangle r, java.awt.geom.Rectangle2D r2d,
            java.awt.geom.AffineTransform xform, java.awt.RenderingHints hints) {
        return new ColorPaintContext(this.getRGB());
    }
}
