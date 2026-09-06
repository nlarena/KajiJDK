package javax.swing.plaf.nimbus;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;

import javax.swing.JComponent;
import javax.swing.Painter;

/**
 * La base de los pintores de Nimbus: dibuja una region en coordenadas relativas.
 *
 * <h2>El problema que resuelve</h2>
 *
 * <p>Un boton de Nimbus no es una imagen: es una figura descrita con curvas y degradados. Esa figura
 * tiene que verse igual midiendo veinte pixeles o doscientos, y sus esquinas redondeadas tienen que
 * conservar el radio en vez de estirarse.
 *
 * <p>De ahi los {@link #decodeX} y {@link #decodeY}: la figura se escribe en una grilla de tres
 * bandas por eje --nueve cuadrantes en total-- y esos metodos la traducen al tamano real. Las bandas
 * de los bordes son los margenes y no se estiran; la del medio absorbe toda la diferencia. Es lo que
 * hace que un borde redondeado siga teniendo el mismo radio cuando el componente crece.
 *
 * <h2>Los colores derivados</h2>
 *
 * <p>{@link #decodeColor(String, float, float, float, int)} no devuelve un color: devuelve
 * <strong>uno corrido</strong> respecto de un color base de la tabla. Asi es como Nimbus se
 * recolorea entero cambiando un puñado de colores: todo lo demas esta escrito como desplazamientos
 * de tono, saturacion y brillo sobre esos pocos.
 *
 * <p>Sin eso, cambiarle el color a Nimbus significaria tocar los mil valores que salieron de la
 * herramienta de diseno.
 *
 * <h2>{@link #paint} es final</h2>
 *
 * <p>Lo que una subclase escribe es {@link #doPaint}. El {@code paint} se queda con lo que no debe
 * variar: preparar el suavizado, calcular la escala y --si el contexto lo pide-- guardar el
 * resultado en la memoria intermedia. Dejarlo redefinible haria que cada pintor tuviera que acordarse
 * de todo eso.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>Las cuentas son reales: la grilla de nueve cuadrantes, los colores derivados y los degradados
 * se calculan de verdad. Lo que no puede es dibujar, porque {@link #doPaint} lo escribe cada pintor
 * concreto y esta biblioteca no tiene ninguno --son las noventa clases privadas del paquete--.
 *
 * @since 1.7
 */
public abstract class AbstractRegionPainter implements Painter<JComponent> {

    /** El componente que se esta pintando ahora, para {@link #getComponentColor}. */
    private JComponent actual;

    /** El tamano real del componente que se esta pintando; lo necesitan los {@code decode}. */
    private int anchoActual;
    private int altoActual;

    /** Uno. */
    protected AbstractRegionPainter() {
    }

    /**
     * Dibuja la region.
     *
     * @param g donde dibujar
     * @param c el componente
     * @param w el ancho
     * @param h el alto
     */
    public final void paint(Graphics2D g, JComponent c, int w, int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        final PaintContext ctx = getPaintContext();
        if (ctx == null) {
            return;
        }
        actual = c;
        anchoActual = w;
        altoActual = h;
        try {
            configureGraphics(g);
            doPaint(g, c, w, h, getExtendedCacheKeys(c));
        } finally {
            actual = null;
            anchoActual = 0;
            altoActual = 0;
        }
    }

    /**
     * Lo que ademas del tamano distingue a este dibujo, para la memoria intermedia.
     *
     * <p>Un pintor que dibuja siempre igual devuelve {@code null}. Uno que mira un color del
     * componente devuelve ese color: si no lo hiciera, dos componentes de distinto color
     * compartirian la imagen guardada y el segundo saldria con el color del primero.
     *
     * @param c el componente
     * @return lo que distingue, o {@code null}
     */
    protected Object[] getExtendedCacheKeys(JComponent c) {
        return null;
    }

    /**
     * Como se dibuja esta region: sus margenes, su tamano de referencia y si se puede guardar.
     *
     * @return el contexto; {@code null} quiere decir que no se dibuja nada
     */
    protected abstract PaintContext getPaintContext();

    /**
     * Prepara el contexto grafico antes de dibujar.
     *
     * <p>Enciende el suavizado, que es lo que hace que las curvas de Nimbus no se vean escalonadas.
     * Una subclase puede cambiarlo.
     *
     * @param g el contexto grafico
     */
    protected void configureGraphics(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    /**
     * El dibujo propiamente dicho.
     *
     * @param g donde dibujar
     * @param c el componente
     * @param width el ancho
     * @param height el alto
     * @param extendedCacheKeys lo que devolvio {@link #getExtendedCacheKeys}
     */
    protected abstract void doPaint(Graphics2D g, JComponent c, int width, int height,
            Object[] extendedCacheKeys);

    /**
     * Traduce una coordenada horizontal de la grilla al tamano real.
     *
     * <p>La grilla va de cero a tres: la banda de cero a uno es el margen izquierdo, la de uno a dos
     * el centro, y la de dos a tres el margen derecho. Los margenes miden lo mismo sea cual sea el
     * tamano del componente; el centro absorbe la diferencia.
     *
     * @param x la coordenada en la grilla
     * @return la coordenada en pixeles
     */
    protected final float decodeX(float x) {
        final Insets m = margenes();
        return decodeEn(x, m.left, anchoActual - m.left - m.right, m.right);
    }

    /**
     * Traduce una coordenada vertical de la grilla al tamano real.
     *
     * @param y la coordenada en la grilla
     * @return la coordenada en pixeles
     */
    protected final float decodeY(float y) {
        final Insets m = margenes();
        return decodeEn(y, m.top, altoActual - m.top - m.bottom, m.bottom);
    }

    /**
     * Como {@link #decodeX} pero corriendo el resultado unos pixeles.
     *
     * <p>El corrimiento se aplica <strong>despues</strong> de traducir, asi que no se estira con el
     * componente. Es lo que se usa para el grosor de una linea: un borde de un pixel tiene que
     * seguir siendo de un pixel en un boton grande.
     *
     * @param x la coordenada en la grilla
     * @param dx cuantos pixeles correrla
     * @return la coordenada en pixeles
     */
    protected final float decodeAnchorX(float x, float dx) {
        return decodeX(x) + dx;
    }

    /**
     * Como {@link #decodeY} pero corriendo el resultado unos pixeles.
     *
     * @param y la coordenada en la grilla
     * @param dy cuantos pixeles correrla
     * @return la coordenada en pixeles
     */
    protected final float decodeAnchorY(float y, float dy) {
        return decodeY(y) + dy;
    }

    /**
     * Un color corrido respecto de uno de la tabla.
     *
     * <p>Los tres primeros corrimientos son de tono, saturacion y brillo, cada uno entre menos uno y
     * uno; el cuarto es de transparencia, entre menos doscientos cincuenta y cinco y doscientos
     * cincuenta y cinco. Es lo que permite recolorear Nimbus entero cambiando unos pocos colores
     * base.
     *
     * @param key la clave del color base en la tabla
     * @param hOffset cuanto correr el tono
     * @param sOffset cuanto correr la saturacion
     * @param bOffset cuanto correr el brillo
     * @param aOffset cuanto correr la transparencia
     * @return el color, o el base si la clave no esta
     */
    protected final Color decodeColor(String key, float hOffset, float sOffset, float bOffset,
            int aOffset) {
        final Object v = javax.swing.UIManager.get(key);
        final Color base = v instanceof Color ? (Color) v : Color.GRAY;
        return corrido(base, hOffset, sOffset, bOffset, aOffset);
    }

    /**
     * Un color entre dos, en esa proporcion.
     *
     * @param color1 el de un extremo
     * @param color2 el del otro
     * @param midPoint cuanto del segundo, entre cero y uno
     * @return el color intermedio
     */
    protected final Color decodeColor(Color color1, Color color2, float midPoint) {
        return new Color(
                mezclar(color1.getRed(), color2.getRed(), midPoint),
                mezclar(color1.getGreen(), color2.getGreen(), midPoint),
                mezclar(color1.getBlue(), color2.getBlue(), midPoint),
                mezclar(color1.getAlpha(), color2.getAlpha(), midPoint));
    }

    /**
     * Un degradado lineal entre dos puntos.
     *
     * <p>Si los dos puntos coinciden no hay degradado posible; se corre el segundo un cienmilesimo,
     * que es lo que hace el JDK. Es feo y es mejor que lanzar una excepcion en medio de un
     * repintado.
     *
     * @param x1 desde
     * @param y1 desde
     * @param x2 hasta
     * @param y2 hasta
     * @param midpoints en que proporcion va cada color, en orden creciente
     * @param colors los colores
     * @return el degradado
     */
    protected final LinearGradientPaint decodeGradient(float x1, float y1, float x2, float y2,
            float[] midpoints, Color[] colors) {
        float fx2 = x2;
        float fy2 = y2;
        if (x1 == fx2 && y1 == fy2) {
            fy2 += 0.00001f;
        }
        return new LinearGradientPaint(new Point2D.Float(x1, y1), new Point2D.Float(fx2, fy2),
                midpoints, colors);
    }

    /**
     * Un degradado radial desde un centro.
     *
     * @param x el centro
     * @param y el centro
     * @param r el radio
     * @param midpoints en que proporcion va cada color, en orden creciente
     * @param colors los colores
     * @return el degradado
     */
    protected final RadialGradientPaint decodeRadialGradient(float x, float y, float r,
            float[] midpoints, Color[] colors) {
        final float radio = r == 0f ? 0.00001f : r;
        return new RadialGradientPaint(new Point2D.Float(x, y), radio, midpoints, colors,
                MultipleGradientPaint.CycleMethod.NO_CYCLE);
    }

    /**
     * Un color del propio componente, corrido.
     *
     * <p>Es lo que permite que un boton al que el programa le puso un color de fondo se dibuje con
     * ese color y no con el de la tabla. Si el componente no tiene ese color, o si lo tiene puesto
     * el aspecto y no el programa, se usa el que corresponda de la tabla.
     *
     * @param c el componente
     * @param property que color se pide: {@code "background"} o {@code "foreground"}
     * @param defaultColor que usar si el componente no lo tiene
     * @param saturationOffset cuanto correr la saturacion
     * @param brightnessOffset cuanto correr el brillo
     * @param alphaOffset cuanto correr la transparencia
     * @return el color
     */
    protected final Color getComponentColor(JComponent c, String property, Color defaultColor,
            float saturationOffset, float brightnessOffset, int alphaOffset) {
        Color base = defaultColor;
        final JComponent comp = c == null ? actual : c;
        if (comp != null) {
            if ("background".equals(property)) {
                base = comp.getBackground();
            } else if ("foreground".equals(property)) {
                base = comp.getForeground();
            }
        }
        if (base == null) {
            base = defaultColor;
        }
        return base == null ? null : corrido(base, 0f, saturationOffset, brightnessOffset,
                alphaOffset);
    }

    /**
     * Dibuja la region.
     *
     * <p>Es la version de {@link Painter} con el tipo sin concretar. Lo que no sea un
     * {@link JComponent} no se dibuja: este pintor mira el componente para decidir sus colores, y
     * sin componente no hay nada que mirar.
     *
     * @param g donde dibujar
     * @param object el componente
     * @param width el ancho
     * @param height el alto
     */
    public void paint(Graphics2D g, Object object, int width, int height) {
        if (object instanceof JComponent) {
            paint(g, (JComponent) object, width, height);
        }
    }

    /** El corrimiento en tono, saturacion, brillo y transparencia. */
    private static Color corrido(Color base, float h, float s, float b, int a) {
        final float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
        final float hh = acotar(hsb[0] + h);
        final float ss = acotar(hsb[1] + s);
        final float bb = acotar(hsb[2] + b);
        final int aa = Math.max(0, Math.min(255, base.getAlpha() + a));
        final Color c = Color.getHSBColor(hh, ss, bb);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), aa);
    }

    private static float acotar(float v) {
        return v < 0f ? 0f : v > 1f ? 1f : v;
    }

    private static int mezclar(int a, int b, float p) {
        final int v = Math.round(a + (b - a) * p);
        return v < 0 ? 0 : v > 255 ? 255 : v;
    }

    /** Los margenes que no se estiran, o todos en cero si no hay contexto. */
    private Insets margenes() {
        final PaintContext ctx = getPaintContext();
        final Insets m = ctx == null ? null : ctx.margenes();
        return m == null ? SIN_MARGEN : m;
    }

    private static final Insets SIN_MARGEN = new Insets(0, 0, 0, 0);

    /**
     * La coordenada de la grilla traducida al tamano real.
     *
     * <p>La grilla tiene tres bandas y va de cero a tres. La primera y la tercera son los margenes y
     * <strong>no se estiran</strong>: miden lo mismo en un boton chico que en uno grande. La del
     * medio absorbe todo el resto, que es lo que hace que una esquina redondeada siga teniendo el
     * mismo radio cuando el componente crece.
     *
     * @throws IllegalArgumentException si la coordenada cae fuera de la grilla
     */
    private static float decodeEn(float v, int primera, int medio, int tercera) {
        if (v >= 0f && v <= 1f) {
            return v * primera;
        }
        if (v > 1f && v < 2f) {
            return (v - 1f) * medio + primera;
        }
        if (v >= 2f && v <= 3f) {
            return (v - 2f) * tercera + primera + medio;
        }
        throw new IllegalArgumentException("fuera de la grilla: " + v);
    }

    /**
     * Como se dibuja una region: margenes, tamano de referencia y si se puede guardar el resultado.
     *
     * <h2>El tamano de referencia</h2>
     *
     * <p>Es el tamano para el que se dibujo la figura. Los {@code decode} traducen de esa grilla al
     * tamano real, y sin el no habria desde donde traducir.
     *
     * <h2>La memoria intermedia</h2>
     *
     * <p>Dibujar una figura con degradados y suavizado es caro, y un componente se repinta muchas
     * veces sin cambiar. Guardar el resultado vale la pena, y cuando conviene depende de la figura:
     * por eso el modo es parte del contexto y no una decision global.
     *
     * @since 1.7
     */
    public static class PaintContext {

        private final Insets insets;
        private final Dimension canvasSize;
        private final boolean inverted;
        private final CacheMode cacheMode;
        private final double maxH;
        private final double maxV;

        /**
         * Un contexto sin guardar nada.
         *
         * @param insets los margenes que no se estiran
         * @param canvasSize el tamano para el que se dibujo la figura
         * @param inverted si la grilla se lee al reves
         */
        public PaintContext(Insets insets, Dimension canvasSize, boolean inverted) {
            this(insets, canvasSize, inverted, null, 1, 1);
        }

        /**
         * Un contexto completo.
         *
         * @param insets los margenes que no se estiran
         * @param canvasSize el tamano para el que se dibujo la figura
         * @param inverted si la grilla se lee al reves
         * @param cacheMode como guardar el resultado, o {@code null} para no guardarlo
         * @param maxH hasta cuanto se puede escalar horizontalmente lo guardado
         * @param maxV hasta cuanto se puede escalar verticalmente lo guardado
         */
        public PaintContext(Insets insets, Dimension canvasSize, boolean inverted,
                CacheMode cacheMode, double maxH, double maxV) {
            this.insets = insets;
            this.canvasSize = canvasSize;
            this.inverted = inverted;
            this.cacheMode = cacheMode == null ? CacheMode.NO_CACHING : cacheMode;
            this.maxH = maxH;
            this.maxV = maxV;
        }

        /** Los margenes que no se estiran; para el pintor que lo envuelve. */
        Insets margenes() {
            return insets;
        }

        /**
         * Como se guarda el resultado de dibujar.
         *
         * @since 1.7
         */
        public static enum CacheMode {

            /** No se guarda. */
            NO_CACHING,

            /** Se guarda una imagen por cada tamano que aparezca. */
            FIXED_SIZES,

            /**
             * Se guarda una sola imagen y se la estira por cuadrantes.
             *
             * <p>Es lo que permite dibujar un boton de cualquier ancho a partir de una sola imagen
             * sin que las esquinas se deformen: los cuatro cuadrantes de las esquinas se copian tal
             * cual y solo se estiran los del medio.
             */
            NINE_SQUARE_SCALE,
        }
    }
}
