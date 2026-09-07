package javax.swing.text.html;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/**
 * La vista de un {@code <img>}.
 *
 * <h2>Tres cosas que puede estar mostrando</h2>
 *
 * <p>La imagen, si ya llego. Un icono de "cargando", mientras viaja. O un icono de "no se pudo",
 * si fallo. Los tres ocupan lugar, y por eso la vista tiene tamano desde el principio: si midiera
 * cero hasta que llegue la imagen, la pagina se rearmaria entera al llegar cada una.
 *
 * <p>Ese es tambien el motivo de que <code>width</code> y <code>height</code> valgan la pena en el
 * HTML: con ellos la vista ya sabe cuanto va a ocupar antes de tener nada.
 *
 * <h2>Cargar de a una o esperando</h2>
 *
 * <p>{@link #setLoadsSynchronously} decide si la carga bloquea. Por omision no bloquea: una imagen
 * lenta no tiene que dejar la ventana quieta. Bloquear sirve para imprimir o para dibujar fuera de
 * pantalla, donde no hay nadie que vuelva a pintar cuando la imagen llegue.
 *
 * <p>Sin ventana no hay imagen que cargar, asi que aca se muestra siempre el texto alternativo.
 */
public class ImageView extends View {

    private AttributeSet attr;
    private Image image;
    private int width;
    private int height;
    private boolean loadsSynchronously;
    private Color borderColor;
    private short borderSize;
    private short leftInset;
    private short rightInset;
    private short topInset;
    private short bottomInset;

    /** Una vista de imagen sobre ese elemento. */
    public ImageView(Element elem) {
        super(elem);
        loadsSynchronously = false;
    }

    /** El texto del atributo <code>alt</code>. */
    public String getAltText() {
        return (String) getElement().getAttributes().getAttribute(HTML.Attribute.ALT);
    }

    /**
     * La direccion de la imagen, resuelta contra la base del documento.
     *
     * <p>Devuelve nulo si no hay <code>src</code> o si no se puede formar la direccion. Un
     * <code>src</code> roto es lo normal en el HTML de verdad, y no deberia costar una excepcion.
     */
    public URL getImageURL() {
        String src = (String) getElement().getAttributes().getAttribute(HTML.Attribute.SRC);
        if (src == null) {
            return null;
        }
        URL base = ((HTMLDocument) getDocument()).getBase();
        try {
            return new URL(base, src);
        } catch (java.net.MalformedURLException e) {
            return null;
        }
    }

    /** El icono que se muestra cuando la imagen no se pudo traer. */
    public Icon getNoImageIcon() {
        return null;
    }

    /** El icono que se muestra mientras la imagen viaja. */
    public Icon getLoadingImageIcon() {
        return null;
    }

    /** La imagen, o nulo si todavia no llego. */
    public Image getImage() {
        return image;
    }

    /** Si la carga bloquea; ver la nota de la clase. */
    public void setLoadsSynchronously(boolean newValue) {
        loadsSynchronously = newValue;
    }

    public boolean getLoadsSynchronously() {
        return loadsSynchronously;
    }

    /** La hoja de estilos del documento, o nulo si el documento no es de HTML. */
    protected StyleSheet getStyleSheet() {
        Document d = getDocument();
        if (d instanceof HTMLDocument) {
            return ((HTMLDocument) d).getStyleSheet();
        }
        return null;
    }

    public AttributeSet getAttributes() {
        if (attr == null) {
            StyleSheet sheet = getStyleSheet();
            attr = (sheet == null) ? super.getAttributes() : sheet.getViewAttributes(this);
        }
        return attr;
    }

    /** El texto de ayuda: el mismo <code>alt</code>. */
    public String getToolTipText(float x, float y, Shape allocation) {
        return getAltText();
    }

    /** Lee tamano y borde de los atributos. */
    protected void setPropertiesFromAttributes() {
        attr = null;
        AttributeSet a = getAttributes();
        width = HTML.getIntegerAttributeValue(a, HTML.Attribute.WIDTH, -1);
        height = HTML.getIntegerAttributeValue(a, HTML.Attribute.HEIGHT, -1);
        borderSize = (short) HTML.getIntegerAttributeValue(a, HTML.Attribute.BORDER, 0);
        int h = HTML.getIntegerAttributeValue(a, HTML.Attribute.HSPACE, 0);
        int v = HTML.getIntegerAttributeValue(a, HTML.Attribute.VSPACE, 0);
        leftInset = (short) (h + borderSize);
        rightInset = (short) (h + borderSize);
        topInset = (short) (v + borderSize);
        bottomInset = (short) (v + borderSize);
        borderColor = Color.black;
    }

    public void setParent(View parent) {
        super.setParent(parent);
        if (parent != null) {
            setPropertiesFromAttributes();
        }
    }

    public void changedUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        super.changedUpdate(e, a, f);
        setPropertiesFromAttributes();
        preferenceChanged(null, true, true);
    }

    /** Dibuja la imagen si llego, y si no el texto alternativo con su marco. */
    public void paint(Graphics g, Shape a) {
        Rectangle rect = (a instanceof Rectangle) ? (Rectangle) a : a.getBounds();
        if (borderSize > 0 && borderColor != null) {
            g.setColor(borderColor);
            for (int i = 0; i < borderSize; i++) {
                g.drawRect(rect.x + i, rect.y + i, rect.width - 2 * i - 1,
                        rect.height - 2 * i - 1);
            }
        }
        if (image != null) {
            g.drawImage(image, rect.x + leftInset, rect.y + topInset,
                    rect.width - leftInset - rightInset,
                    rect.height - topInset - bottomInset, null);
            return;
        }
        String alt = getAltText();
        if (alt != null) {
            java.awt.FontMetrics fm = g.getFontMetrics();
            g.drawString(alt, rect.x + leftInset, rect.y + topInset + fm.getAscent());
        }
    }

    /**
     * El tamano que ocupa.
     *
     * <p>El declarado si lo hay; si no, el de la imagen; si tampoco, un cuadro donde entre el texto
     * alternativo. Nunca cero: ver la nota de la clase.
     */
    public float getPreferredSpan(int axis) {
        if (axis == X_AXIS) {
            if (width > 0) {
                return width + leftInset + rightInset;
            }
            String alt = getAltText();
            int base = (alt == null) ? 32 : Math.max(32, alt.length() * 7);
            return base + leftInset + rightInset;
        }
        if (height > 0) {
            return height + topInset + bottomInset;
        }
        return 32 + topInset + bottomInset;
    }

    /** Una imagen se apoya en la linea de base del texto. */
    public float getAlignment(int axis) {
        if (axis == Y_AXIS) {
            return 1.0f;
        }
        return super.getAlignment(axis);
    }

    public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
        int p0 = getStartOffset();
        int p1 = getEndOffset();
        if ((pos >= p0) && (pos <= p1)) {
            Rectangle r = (a instanceof Rectangle) ? (Rectangle) a : a.getBounds();
            if (pos == p1) {
                r.x = r.x + r.width;
            }
            r.width = 0;
            return r;
        }
        throw new BadLocationException(pos + " not in range " + p0 + "," + p1, pos);
    }

    public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
        Rectangle alloc = (Rectangle) a;
        if (x < alloc.x + alloc.width / 2f) {
            bias[0] = Position.Bias.Forward;
            return getStartOffset();
        }
        bias[0] = Position.Bias.Backward;
        return getEndOffset();
    }

    public void setSize(float width, float height) {
    }
}
