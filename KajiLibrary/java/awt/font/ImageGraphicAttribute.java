package java.awt.font;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Rectangle2D;

/**
 * Una imagen metida en un renglón de texto como si fuera un carácter.
 *
 * <p>El origen dice **qué punto de la imagen** se apoya en el renglón, y por eso se da en
 * coordenadas de la imagen y no del texto. Con el origen en (0,0) la imagen cuelga entera por debajo
 * de la línea de base; con el origen en el medio, queda centrada; con el origen abajo, se apoya como
 * una letra.
 *
 * <p>El tamaño se pregunta una sola vez, al construir. Una imagen que todavía se esté cargando
 * contestaría -1, y las medidas del renglón quedarían mal para siempre: conviene construir esto con
 * una imagen ya completa.
 */
public final class ImageGraphicAttribute extends GraphicAttribute {

    private final Image image;
    private final float imageWidth;
    private final float imageHeight;
    private final float originX;
    private final float originY;

    /**
     * Con el origen en el ángulo superior izquierdo de la imagen.
     *
     * @throws IllegalArgumentException si la alineación no es una de las cinco
     * @throws NullPointerException si la imagen es `null`
     */
    public ImageGraphicAttribute(Image image, int alignment) {
        this(image, alignment, 0, 0);
    }

    /**
     * Con el origen en el punto dado de la imagen.
     *
     * @throws IllegalArgumentException si la alineación no es una de las cinco
     * @throws NullPointerException si la imagen es `null`
     */
    public ImageGraphicAttribute(Image image, int alignment, float originX, float originY) {
        super(alignment);
        this.image = image;
        this.imageWidth = image.getWidth(null);
        this.imageHeight = image.getHeight(null);
        this.originX = originX;
        this.originY = originY;
    }

    /** Lo que la imagen queda por encima de la línea de base. */
    public float getAscent() {
        return Math.max(0, this.originY);
    }

    /** Lo que la imagen queda por debajo de la línea de base. */
    public float getDescent() {
        return Math.max(0, this.imageHeight - this.originY);
    }

    /** Lo que avanza el renglón: lo que quede a la derecha del origen. */
    public float getAdvance() {
        return Math.max(0, this.imageWidth - this.originX);
    }

    /** El rectángulo de la imagen, relativo al origen. */
    public Rectangle2D getBounds() {
        return new Rectangle2D.Float(-this.originX, -this.originY, this.imageWidth,
                this.imageHeight);
    }

    /** Dibuja la imagen con su origen en `(x, y)`. */
    public void draw(Graphics2D graphics, float x, float y) {
        graphics.drawImage(this.image, (int) (x - this.originX), (int) (y - this.originY), null);
    }

    public int hashCode() {
        return this.image.hashCode();
    }

    /** Igualdad por imagen, alineación y origen. */
    public boolean equals(Object rhs) {
        if (rhs instanceof ImageGraphicAttribute) {
            return this.equals((ImageGraphicAttribute) rhs);
        }
        return false;
    }

    /**
     * Lo mismo, con el tipo ya conocido.
     *
     * <p>La imagen se compara por **identidad**: dos imágenes distintas con los mismos píxeles son
     * dos objetos, y compararlas píxel a píxel en un `equals` que se llama por tramo de texto no
     * sería razonable.
     */
    public boolean equals(ImageGraphicAttribute rhs) {
        if (rhs == null) {
            return false;
        }
        if (this == rhs) {
            return true;
        }
        if (this.originX != rhs.originX || this.originY != rhs.originY) {
            return false;
        }
        if (this.getAlignment() != rhs.getAlignment()) {
            return false;
        }
        return this.image.equals(rhs.image);
    }
}
