package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;

import javax.swing.JComponent;
import javax.swing.text.AttributeSet;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

/**
 * Lo que convierte el texto de una etiqueta o un boton en HTML dibujable.
 *
 * <h2>Por que existe</h2>
 *
 * <p>Un {@code JLabel} pinta su texto con {@code drawString} y se acabo. Pero si el texto empieza
 * con {@code <html>}, Swing lo pinta con negritas, saltos de linea y colores. Esa segunda forma no
 * la sabe la etiqueta: la sabe el motor de {@code javax.swing.text.html}, y esta clase es el puente.
 *
 * <p>El puente son dos llamadas. {@link #updateRenderer} arma una vista y la guarda en el
 * componente bajo la clave {@link #propertyKey}; cada UI que dibuja texto la busca ahi, y si esta,
 * mide y pinta con ella en vez de con la fuente. Si el texto deja de ser HTML, la vista se borra.
 *
 * <h2>Que cuenta como HTML</h2>
 *
 * <p>{@link #isHTMLString} es deliberadamente tonta: el texto tiene que arrancar <em>exactamente</em>
 * con {@code <html>} --seis caracteres, sin espacio adelante, sin atributos--. {@code "  <html>x"}
 * no cuenta, y {@code "<html"} tampoco. Esta medido, y la razon es que la prueba corre en cada
 * cambio de texto de cada etiqueta de la pantalla: tiene que costar cinco comparaciones y nada mas.
 *
 * <h2>La vista de arriba de todo</h2>
 *
 * <p>La vista que se guarda no es la del documento: es un envoltorio ({@code Renderer}) que le da
 * un ancho de trabajo y traduce las preguntas de tamano. Hace falta porque el motor de texto espera
 * estar dentro de un {@code JTextComponent} con un {@code Container} de verdad, y aca no hay
 * ninguno: solo la etiqueta que pidio dibujar.
 */
public class BasicHTML {

    /** Donde queda guardada la vista dentro del componente. */
    public static final String propertyKey = "html";

    /** Donde el componente puede dejar la {@link URL} contra la que se resuelven los enlaces. */
    public static final String documentBaseKey = "html.base";

    public BasicHTML() {
    }

    /**
     * Arma la vista para ese texto.
     *
     * <p>La fuente y el color del componente entran como estilo del cuerpo, para que el HTML sin
     * estilo propio se vea como el resto del componente.
     */
    public static View createHTMLView(JComponent c, String html) {
        HTMLEditorKit kit = new HTMLEditorKit();
        Document doc = kit.createDefaultDocument();
        if (doc instanceof HTMLDocument) {
            HTMLDocument hdoc = (HTMLDocument) doc;
            Object base = c.getClientProperty(documentBaseKey);
            if (base instanceof URL) {
                hdoc.setBase((URL) base);
            }
            estilar(hdoc.getStyleSheet(), c.getFont(), c.getForeground());
        }
        Reader r = new StringReader(html);
        try {
            kit.read(r, doc, 0);
        } catch (Exception e) {
            // Un HTML roto no rompe la etiqueta: se dibuja lo que se haya podido leer. Es lo que
            // hace el JDK, y es lo unico razonable -- el texto lo escribio quien programo la
            // pantalla, y una excepcion acá aparecería al pintar, lejos del error.
        }
        ViewFactory f = kit.getViewFactory();
        View hview = f.create(doc.getDefaultRootElement());
        return new Renderer(c, f, hview);
    }

    /** Mete la fuente y el color del componente en la hoja de estilo. */
    private static void estilar(StyleSheet hoja, Font fuente, Color color) {
        if (hoja == null || fuente == null) {
            return;
        }
        StringBuilder regla = new StringBuilder("body {font-family:");
        regla.append(fuente.getFamily()).append(";font-size:").append(fuente.getSize()).append("pt");
        if (fuente.isBold()) {
            regla.append(";font-weight:700");
        }
        if (fuente.isItalic()) {
            regla.append(";font-style:italic");
        }
        if (color != null) {
            regla.append(";color:#").append(hex(color));
        }
        regla.append("}");
        try {
            hoja.addRule(regla.toString());
        } catch (Exception e) {
            // Una hoja que no acepta la regla deja el HTML con sus valores de siempre, que es
            // peor pero no es un error: se sigue viendo.
        }
    }

    private static String hex(Color c) {
        String s = Integer.toHexString(c.getRGB() & 0xffffff);
        while (s.length() < 6) {
            s = "0" + s;
        }
        return s;
    }

    /**
     * Si ese texto es HTML; ver la nota de la clase.
     *
     * @return `false` si es nulo, corto, o no arranca con {@code <html>}
     */
    public static boolean isHTMLString(String s) {
        if (s != null) {
            if ((s.length() >= 6) && (s.charAt(0) == '<') && (s.charAt(5) == '>')) {
                String tag = s.substring(1, 5);
                return tag.equalsIgnoreCase(propertyKey);
            }
        }
        return false;
    }

    /**
     * Pone o saca la vista del componente segun el texto.
     *
     * <p>Es lo que hay que llamar cada vez que cambia el texto: si dejo de ser HTML, la vista
     * vieja se borra y el componente vuelve a pintar con {@code drawString}.
     */
    public static void updateRenderer(JComponent c, String text) {
        View value = null;
        if (isHTMLString(text)) {
            value = createHTMLView(c, text);
        }
        View oldValue = (View) c.getClientProperty(propertyKey);
        if (value != oldValue && oldValue != null) {
            for (int i = 0; i < oldValue.getViewCount(); i++) {
                oldValue.getView(i).setParent(null);
            }
        }
        c.putClientProperty(propertyKey, value);
    }

    /**
     * La linea de base de una vista de HTML.
     *
     * <p>Solo la tiene si el HTML es un solo parrafo; si no, -1. La pregunta la hace
     * {@code JLabel.getBaseline} para alinear una etiqueta con el campo de al lado, y con dos
     * parrafos no hay ninguna respuesta que sirva.
     *
     * @throws IllegalArgumentException si el ancho o el alto son negativos
     */
    public static int getHTMLBaseline(View view, int w, int h) {
        if (w < 0 || h < 0) {
            throw new IllegalArgumentException("Width and height must be >= 0");
        }
        if (view instanceof Renderer) {
            return lineaDeBase(view.getView(0), w, h);
        }
        return -1;
    }

    private static int lineaDeBase(View view, int w, int h) {
        if (!hayUnSoloParrafo(view)) {
            return -1;
        }
        view.setSize(w, h);
        return lineaDeBase(view, new Rectangle(0, 0, w, h));
    }

    /** Baja hasta el parrafo y ahi si mide: arriba de el todo es caja, no texto. */
    private static int lineaDeBase(View view, Shape bounds) {
        if (view.getViewCount() == 0) {
            return -1;
        }
        int index = 0;
        if (esEtiqueta(view, "html") && view.getViewCount() > 1) {
            // La cabecera es la primera hija y no ocupa lugar; lo que se ve es el cuerpo.
            index = 1;
        }
        Shape hija = view.getChildAllocation(index, bounds);
        if (hija == null) {
            return -1;
        }
        View child = view.getView(index);
        if (view instanceof javax.swing.text.ParagraphView) {
            Rectangle rect = (hija instanceof Rectangle) ? (Rectangle) hija : hija.getBounds();
            return rect.y + (int) (child.getPreferredSpan(View.Y_AXIS)
                    * child.getAlignment(View.Y_AXIS));
        }
        return lineaDeBase(child, hija);
    }

    /** Si hay exactamente un parrafo: dos ya no tienen una linea de base comun. */
    private static boolean hayUnSoloParrafo(View view) {
        if (view instanceof javax.swing.text.ParagraphView) {
            return true;
        }
        int n = view.getViewCount();
        int parrafos = 0;
        for (int i = 0; i < n; i++) {
            if (hayUnSoloParrafo(view.getView(i))) {
                parrafos++;
            }
            if (parrafos > 1) {
                return false;
            }
        }
        return parrafos == 1;
    }

    private static boolean esEtiqueta(View view, String nombre) {
        Element e = view.getElement();
        if (e == null) {
            return false;
        }
        AttributeSet a = e.getAttributes();
        if (a == null) {
            return false;
        }
        Object n = a.getAttribute(javax.swing.text.StyleConstants.NameAttribute);
        return n != null && nombre.equalsIgnoreCase(n.toString());
    }

    /**
     * La vista de arriba de todo; ver la nota de la clase.
     *
     * <p>No hereda de {@code javax.swing.text.View} por comodidad: tiene que serlo porque es lo que
     * se guarda en el componente y lo que los UI van a usar para medir y pintar.
     */
    private static class Renderer extends View {

        private int width;
        private final View view;
        private final ViewFactory factory;
        private final JComponent host;

        Renderer(JComponent c, ViewFactory f, View v) {
            super(null);
            host = c;
            factory = f;
            view = v;
            view.setParent(this);
            // El ancho de trabajo arranca en el preferido: sin nadie que lo acomode, la vista se
            // mide a si misma antes de que alguien le diga cuanto lugar tiene.
            setSize(view.getPreferredSpan(X_AXIS), view.getPreferredSpan(Y_AXIS));
        }

        /** El ancho manda: el alto sale de como se parte el texto en ese ancho. */
        public void setSize(float width, float height) {
            this.width = (int) width;
            view.setSize(width, height);
        }

        public AttributeSet getAttributes() {
            return null;
        }

        public float getPreferredSpan(int axis) {
            if (axis == X_AXIS) {
                // Se devuelve el ancho de trabajo y no el natural: es lo que hace que una etiqueta
                // que ya se acomodo no cambie de opinion al medirla de nuevo.
                return width;
            }
            return view.getPreferredSpan(axis);
        }

        public float getMinimumSpan(int axis) {
            return view.getMinimumSpan(axis);
        }

        public float getMaximumSpan(int axis) {
            // Sin tope horizontal: el HTML se estira todo lo que le den.
            if (axis == X_AXIS) {
                return Integer.MAX_VALUE;
            }
            return view.getMaximumSpan(axis);
        }

        public void preferenceChanged(View child, boolean width, boolean height) {
            host.revalidate();
            host.repaint();
        }

        public float getAlignment(int axis) {
            return view.getAlignment(axis);
        }

        public void paint(Graphics g, Shape allocation) {
            Rectangle alloc = allocation.getBounds();
            view.setSize(alloc.width, alloc.height);
            view.paint(g, allocation);
        }

        public void setParent(View parent) {
            throw new Error("Can't set parent on root view");
        }

        public int getViewCount() {
            return 1;
        }

        public View getView(int n) {
            return view;
        }

        public Shape modelToView(int pos, Shape a, Position.Bias b)
                throws javax.swing.text.BadLocationException {
            return view.modelToView(pos, a, b);
        }

        public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
            return view.viewToModel(x, y, a, bias);
        }

        public Document getDocument() {
            return view.getDocument();
        }

        public int getStartOffset() {
            return view.getStartOffset();
        }

        public int getEndOffset() {
            return view.getEndOffset();
        }

        public Element getElement() {
            return view.getElement();
        }

        public ViewFactory getViewFactory() {
            return factory;
        }
    }
}
