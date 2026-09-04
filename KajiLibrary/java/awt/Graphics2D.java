package java.awt;

import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;

/**
 * Dibujo en coordenadas continuas, con transformación, trazo, relleno y composición.
 *
 * <p>Es {@link Graphics} llevado de píxeles a geometría, y el salto no es de comodidad sino de
 * modelo. Allá había un color y unas coordenadas enteras; acá hay cuatro cosas que se combinan en
 * cada operación:
 *
 * <ul>
 *   <li>la <strong>transformación</strong>, que dice dónde caen las coordenadas de usuario;
 *   <li>el <strong>trazo</strong> ({@link Stroke}), que convierte una línea en la figura de su
 *       grosor, sus puntas y su punteado;
 *   <li>la <strong>pintura</strong> ({@link Paint}), que decide de qué color es cada punto y por eso
 *       puede ser un degradé o una textura y no sólo un color;
 *   <li>la <strong>composición</strong> ({@link Composite}), que dice cómo se mezcla lo que se
 *       dibuja con lo que había.
 * </ul>
 *
 * <p>De ahí sale la simetría de la clase: {@link #draw} es rellenar la figura que el trazo genera a
 * partir del contorno, y {@link #fill} es rellenar la figura misma. Una sola operación de fondo con
 * dos entradas distintas.
 *
 * <p>El color y la pintura son el mismo estado visto de dos maneras. `setColor` es `setPaint` con un
 * color, y `getPaint` después de un `setColor` devuelve ese color; pero `getColor` después de un
 * degradé devuelve el último color liso, no el degradé. Eso explica por qué {@link #draw3DRect}
 * guarda la pintura y no el color antes de tocar nada.
 */
public abstract class Graphics2D extends Graphics {

    /** Para las subclases. */
    protected Graphics2D() {
    }

    /**
     * Un rectángulo con relieve.
     *
     * <p>Se redefine respecto de {@link Graphics} porque acá el estado que hay que preservar es la
     * **pintura** y no el color: si venía un degradé, restaurar sólo el color lo perdería.
     */
    public void draw3DRect(int x, int y, int width, int height, boolean raised) {
        Paint p = this.getPaint();
        Color c = this.getColor();
        Color brighter = c.brighter();
        Color darker = c.darker();
        this.setColor(raised ? brighter : darker);
        this.fillRect(x, y, 1, height + 1);
        this.fillRect(x + 1, y, width - 1, 1);
        this.setColor(raised ? darker : brighter);
        this.fillRect(x + 1, y + height, width, 1);
        this.fillRect(x + width, y, 1, height);
        this.setPaint(p);
    }

    /** Un rectángulo relleno con relieve. */
    public void fill3DRect(int x, int y, int width, int height, boolean raised) {
        Paint p = this.getPaint();
        Color c = this.getColor();
        Color brighter = c.brighter();
        Color darker = c.darker();
        if (!raised) {
            this.setColor(darker);
        } else if (p != c) {
            this.setColor(c);
        }
        this.fillRect(x + 1, y + 1, width - 2, height - 2);
        this.setColor(raised ? brighter : darker);
        this.fillRect(x, y, 1, height);
        this.fillRect(x + 1, y, width - 2, 1);
        this.setColor(raised ? darker : brighter);
        this.fillRect(x + 1, y + height - 1, width - 1, 1);
        this.fillRect(x + width - 1, y, 1, height - 1);
        this.setPaint(p);
    }

    /** El contorno de una figura, con el trazo actual. */
    public abstract void draw(Shape s);

    /** Dibuja una imagen transformada. */
    public abstract boolean drawImage(Image img, AffineTransform xform, ImageObserver obs);

    /** Aplica una operación a una imagen y la dibuja en `(x, y)`. */
    public abstract void drawImage(BufferedImage img, BufferedImageOp op, int x, int y);

    /** Dibuja una imagen ya rasterizada, transformada. */
    public abstract void drawRenderedImage(RenderedImage img, AffineTransform xform);

    /**
     * Dibuja una imagen sin resolución, transformada.
     *
     * <p>La imagen se rasteriza **a la escala en la que va a quedar**, así que ampliarla no pixela:
     * se vuelve a dibujar más grande.
     */
    public abstract void drawRenderableImage(RenderableImage img, AffineTransform xform);

    /** Dibuja un texto con el comienzo de la línea de base en `(x, y)`. */
    public abstract void drawString(String str, int x, int y);

    /** Lo mismo, en coordenadas continuas. */
    public abstract void drawString(String str, float x, float y);

    /** Dibuja un texto con atributos. */
    public abstract void drawString(AttributedCharacterIterator iterator, int x, int y);

    /** Lo mismo, en coordenadas continuas. */
    public abstract void drawString(AttributedCharacterIterator iterator, float x, float y);

    /**
     * Dibuja glifos ya colocados.
     *
     * <p>Es el camino de abajo: acá ya no hay caracteres que interpretar ni texto que armar, sólo
     * dibujos con coordenadas. Sirve para dibujar dos veces el mismo texto sin volver a armarlo.
     */
    public abstract void drawGlyphVector(GlyphVector g, float x, float y);

    /** Rellena una figura con la pintura actual. */
    public abstract void fill(Shape s);

    /**
     * Si una figura toca ese rectángulo del dispositivo.
     *
     * <p>Con `onStroke` se pregunta por el contorno trazado en vez de por el interior, que es la
     * diferencia entre acertarle a una línea fina y acertarle a lo que encierra.
     */
    public abstract boolean hit(Rectangle rect, Shape s, boolean onStroke);

    /** La configuración del dispositivo sobre el que se está dibujando. */
    public abstract GraphicsConfiguration getDeviceConfiguration();

    /** Cambia cómo se mezcla lo que se dibuja con lo que había. */
    public abstract void setComposite(Composite comp);

    /** Cambia con qué se pinta. */
    public abstract void setPaint(Paint paint);

    /** Cambia el grosor, las puntas y el punteado de las líneas. */
    public abstract void setStroke(Stroke s);

    /** Cambia una pista de calidad. */
    public abstract void setRenderingHint(RenderingHints.Key hintKey, Object hintValue);

    /** El valor de una pista, o `null` si no está puesta. */
    public abstract Object getRenderingHint(RenderingHints.Key hintKey);

    /** Reemplaza todas las pistas. */
    public abstract void setRenderingHints(Map<?, ?> hints);

    /** Agrega pistas sin borrar las que ya había. */
    public abstract void addRenderingHints(Map<?, ?> hints);

    /** Todas las pistas. */
    public abstract RenderingHints getRenderingHints();

    /** Corre el origen. */
    public abstract void translate(int x, int y);

    /** Corre el origen, en coordenadas continuas. */
    public abstract void translate(double tx, double ty);

    /** Gira alrededor del origen, en radianes y en sentido horario. */
    public abstract void rotate(double theta);

    /** Gira alrededor de ese punto. */
    public abstract void rotate(double theta, double x, double y);

    /** Escala los dos ejes. */
    public abstract void scale(double sx, double sy);

    /** Inclina los dos ejes. */
    public abstract void shear(double shx, double shy);

    /** Compone una transformación con la que ya hay. */
    public abstract void transform(AffineTransform Tx);

    /**
     * Reemplaza la transformación entera.
     *
     * <p>Peligroso sobre un contexto prestado: descarta la que había, incluida la que el sistema
     * puso para ubicar al componente. Para cambios propios va {@link #transform}.
     */
    public abstract void setTransform(AffineTransform Tx);

    /** La transformación actual. */
    public abstract AffineTransform getTransform();

    /** Con qué se pinta. */
    public abstract Paint getPaint();

    /** Cómo se mezcla lo que se dibuja con lo que había. */
    public abstract Composite getComposite();

    /**
     * Cambia el color de fondo, que es el que usa {@link Graphics#clearRect}.
     *
     * <p>No es el color con el que se dibuja: es con lo que se borra.
     */
    public abstract void setBackground(Color color);

    /** El color de fondo. */
    public abstract Color getBackground();

    /** El trazo actual. */
    public abstract Stroke getStroke();

    /**
     * Reduce el recorte a la intersección con esa figura.
     *
     * <p>Como {@link Graphics#clipRect}, nunca lo agranda.
     */
    public abstract void clip(Shape s);

    /** Las condiciones en las que se va a medir y dibujar el texto. */
    public abstract FontRenderContext getFontRenderContext();
}
