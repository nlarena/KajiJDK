package javax.swing.text;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Vector;

import javax.swing.plaf.TextUI;

/**
 * El resaltador de siempre: pinta tramos de texto con un color de fondo.
 *
 * <h2>Un resaltado es una marca, no un dibujo</h2>
 *
 * <p>Agregar un resaltado devuelve una etiqueta opaca. Esa etiqueta es la que sirve para cambiarlo
 * o sacarlo despues; el resaltador no promete nada sobre que clase es. Aca es un
 * {@link HighlightInfo}, pero quien lo use no deberia saberlo.
 *
 * <h2>Los limites se guardan como posiciones</h2>
 *
 * <p>Cada resaltado guarda dos {@link Position}, no dos enteros: asi sobrevive a las ediciones. Si
 * se guardaran enteros, insertar una letra antes de una seleccion la dejaria corrida.
 *
 * <h2>Dibujo entero o por capas</h2>
 *
 * <p>Un pintor comun se dibuja al final, sobre el texto. Un {@link LayeredHighlighter.LayerPainter}
 * se dibuja <em>antes</em> de cada porcion de texto, y por eso puede pintar un fondo sin taparlo.
 * {@link #setDrawsLayeredHighlights} elige cual de los dos caminos se usa.
 */
public class DefaultHighlighter extends LayeredHighlighter {

    /** El pintor por omision: fondo con el color de seleccion del componente. */
    public static final LayeredHighlighter.LayerPainter DefaultPainter =
            new DefaultHighlightPainter(null);

    private static final Highlight[] noHighlights = new Highlight[0];

    private Vector<HighlightInfo> highlights = new Vector<HighlightInfo>();
    private JTextComponent component;
    private boolean drawsLayeredHighlights = true;
    private SafeDamager safeDamager = new SafeDamager(this);

    /** Un resaltador vacio. */
    public DefaultHighlighter() {
        drawsLayeredHighlights = true;
    }

    /** Dibuja los resaltados que no son por capas. */
    public void paint(Graphics g) {
        int len = highlights.size();
        for (int i = 0; i < len; i++) {
            HighlightInfo info = highlights.elementAt(i);
            if (!(info instanceof LayeredHighlightInfo)) {
                Rectangle a = component.getBounds();
                java.awt.Insets insets = component.getInsets();
                a.x = insets.left;
                a.y = insets.top;
                a.width = a.width - insets.left - insets.right;
                a.height = a.height - insets.top - insets.bottom;
                for (; i < len; i++) {
                    info = highlights.elementAt(i);
                    if (!(info instanceof LayeredHighlightInfo)) {
                        Highlighter.HighlightPainter p = info.getPainter();
                        p.paint(g, info.getStartOffset(), info.getEndOffset(), a, component);
                    }
                }
            }
        }
    }

    public void install(JTextComponent c) {
        component = c;
        removeAllHighlights();
    }

    public void deinstall(JTextComponent c) {
        component = null;
    }

    /** Marca ese tramo y devuelve la etiqueta para cambiarlo despues. */
    public Object addHighlight(int p0, int p1, Highlighter.HighlightPainter p)
            throws BadLocationException {
        if (p0 < 0) {
            throw new BadLocationException("Invalid start offset", p0);
        }
        if (p1 < p0) {
            throw new BadLocationException("Invalid end offset", p1);
        }
        Document doc = component.getDocument();
        HighlightInfo i = (getDrawsLayeredHighlights()
                && (p instanceof LayeredHighlighter.LayerPainter))
                ? new LayeredHighlightInfo() : new HighlightInfo();
        i.painter = p;
        i.p0 = doc.createPosition(p0);
        i.p1 = doc.createPosition(p1);
        highlights.addElement(i);
        safeDamageRange(p0, p1);
        return i;
    }

    public void removeHighlight(Object tag) {
        if (tag instanceof LayeredHighlightInfo) {
            LayeredHighlightInfo lhi = (LayeredHighlightInfo) tag;
            if (lhi.width > 0 && lhi.height > 0) {
                component.repaint(lhi.x, lhi.y, lhi.width, lhi.height);
            }
        } else {
            HighlightInfo info = (HighlightInfo) tag;
            safeDamageRange(info.p0, info.p1);
        }
        highlights.removeElement(tag);
    }

    public void removeAllHighlights() {
        TextUI mapper = component.getUI();
        if (getDrawsLayeredHighlights()) {
            int len = highlights.size();
            if (len != 0) {
                int minX = 0;
                int minY = 0;
                int maxX = 0;
                int maxY = 0;
                int p0 = -1;
                int p1 = -1;
                for (int i = 0; i < len; i++) {
                    HighlightInfo hi = highlights.elementAt(i);
                    if (hi instanceof LayeredHighlightInfo) {
                        LayeredHighlightInfo info = (LayeredHighlightInfo) hi;
                        minX = Math.min(minX, info.x);
                        minY = Math.min(minY, info.y);
                        maxX = Math.max(maxX, info.x + info.width);
                        maxY = Math.max(maxY, info.y + info.height);
                    } else {
                        if (p0 == -1) {
                            p0 = hi.p0.getOffset();
                            p1 = hi.p1.getOffset();
                        } else {
                            p0 = Math.min(p0, hi.p0.getOffset());
                            p1 = Math.max(p1, hi.p1.getOffset());
                        }
                    }
                }
                if (minX != maxX && minY != maxY) {
                    component.repaint(minX, minY, maxX - minX, maxY - minY);
                }
                if (p0 != -1) {
                    try {
                        safeDamageRange(p0, p1);
                    } catch (BadLocationException e) {
                        // El documento cambio: no queda nada que repintar.
                    }
                }
                highlights.removeAllElements();
            }
        } else if (mapper != null) {
            int len = highlights.size();
            if (len != 0) {
                int p0 = Integer.MAX_VALUE;
                int p1 = 0;
                for (int i = 0; i < len; i++) {
                    HighlightInfo info = highlights.elementAt(i);
                    p0 = Math.min(p0, info.p0.getOffset());
                    p1 = Math.max(p1, info.p1.getOffset());
                }
                try {
                    safeDamageRange(p0, p1);
                } catch (BadLocationException e) {
                    // Igual que arriba.
                }
                highlights.removeAllElements();
            }
        }
    }

    /** Corre un resaltado ya puesto a otro tramo; no cambia su etiqueta. */
    public void changeHighlight(Object tag, int p0, int p1) throws BadLocationException {
        if (p0 < 0) {
            throw new BadLocationException("Invalid beginning of the range", p0);
        }
        if (p1 < p0) {
            throw new BadLocationException("Invalid end of the range", p1);
        }
        Document doc = component.getDocument();
        if (tag instanceof LayeredHighlightInfo) {
            LayeredHighlightInfo lhi = (LayeredHighlightInfo) tag;
            if (lhi.width > 0 && lhi.height > 0) {
                component.repaint(lhi.x, lhi.y, lhi.width, lhi.height);
            }
            lhi.width = 0;
            lhi.height = 0;
            lhi.p0 = doc.createPosition(p0);
            lhi.p1 = doc.createPosition(p1);
            safeDamageRange(Math.min(p0, p1), Math.max(p0, p1));
        } else {
            HighlightInfo info = (HighlightInfo) tag;
            int oldP0 = info.p0.getOffset();
            int oldP1 = info.p1.getOffset();
            if (p0 == oldP0) {
                safeDamageRange(Math.min(oldP1, p1), Math.max(oldP1, p1));
            } else if (p1 == oldP1) {
                safeDamageRange(Math.min(p0, oldP0), Math.max(p0, oldP0));
            } else {
                safeDamageRange(oldP0, oldP1);
                safeDamageRange(p0, p1);
            }
            info.p0 = doc.createPosition(p0);
            info.p1 = doc.createPosition(p1);
        }
    }

    public Highlight[] getHighlights() {
        int size = highlights.size();
        if (size == 0) {
            return noHighlights;
        }
        Highlight[] h = new Highlight[size];
        highlights.copyInto(h);
        return h;
    }

    /**
     * Dibuja los resaltados por capas que caen en ese tramo.
     *
     * <p>La llama la vista, antes de pintar el texto de esa porcion.
     */
    public void paintLayeredHighlights(Graphics g, int p0, int p1, Shape viewBounds,
            JTextComponent editor, View view) {
        for (int counter = highlights.size() - 1; counter >= 0; counter--) {
            HighlightInfo tag = highlights.elementAt(counter);
            if (tag instanceof LayeredHighlightInfo) {
                LayeredHighlightInfo lhi = (LayeredHighlightInfo) tag;
                int start = lhi.getStartOffset();
                int end = lhi.getEndOffset();
                if ((p0 < start && p1 > start) || (p0 >= start && p0 < end)) {
                    lhi.paintLayeredHighlights(g, p0, p1, viewBounds, editor, view);
                }
            }
        }
    }

    /** Manda repintar ese tramo; lo hace en el hilo de eventos. */
    private void safeDamageRange(Position p0, Position p1) {
        safeDamager.damageRange(p0, p1);
    }

    private void safeDamageRange(int a0, int a1) throws BadLocationException {
        Document doc = component.getDocument();
        safeDamageRange(doc.createPosition(a0), doc.createPosition(a1));
    }

    /** Si los pintores por capas se usan como tales; ver la nota de la clase. */
    public void setDrawsLayeredHighlights(boolean newValue) {
        drawsLayeredHighlights = newValue;
    }

    public boolean getDrawsLayeredHighlights() {
        return drawsLayeredHighlights;
    }

    /**
     * Un pintor que llena el fondo de un tramo con un color.
     *
     * <p>Con color nulo usa el de seleccion del componente, que es lo que quiere el cursor: si el
     * color viniera fijo, cambiar el aspecto no cambiaria la seleccion.
     */
    public static class DefaultHighlightPainter extends LayeredHighlighter.LayerPainter {

        private Color color;

        /** Un pintor de ese color; nulo significa "el de seleccion del componente". */
        public DefaultHighlightPainter(Color c) {
            color = c;
        }

        public Color getColor() {
            return color;
        }

        private Color obtenerColor(JTextComponent c) {
            Color col = getColor();
            return (col == null) ? c.getSelectionColor() : col;
        }

        /** Dibuja el tramo entero, de una. */
        public void paint(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c) {
            Rectangle alloc = bounds.getBounds();
            try {
                TextUI mapper = c.getUI();
                Rectangle p0 = mapper.modelToView(c, offs0);
                Rectangle p1 = mapper.modelToView(c, offs1);
                g.setColor(obtenerColor(c));
                if (p0.y == p1.y) {
                    Rectangle r = p0.union(p1);
                    g.fillRect(r.x, r.y, r.width, r.height);
                } else {
                    // Varias lineas: la primera hasta el borde, las del medio enteras.
                    int p0ToMarginWidth = alloc.x + alloc.width - p0.x;
                    g.fillRect(p0.x, p0.y, p0ToMarginWidth, p0.height);
                    if ((p0.y + p0.height) != p1.y) {
                        g.fillRect(alloc.x, p0.y + p0.height, alloc.width,
                                p1.y - (p0.y + p0.height));
                    }
                    g.fillRect(alloc.x, p1.y, (p1.x - alloc.x), p1.height);
                }
            } catch (BadLocationException e) {
                // No se puede ubicar el tramo: no se dibuja.
            }
        }

        /**
         * Dibuja la parte del resaltado que cae en esa vista.
         *
         * <p>Devuelve la region pintada para que el resaltador sepa que repintar despues.
         */
        public Shape paintLayer(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c,
                View view) {
            g.setColor(obtenerColor(c));
            Rectangle r;
            if (offs0 == view.getStartOffset() && offs1 == view.getEndOffset()) {
                r = (bounds instanceof Rectangle) ? (Rectangle) bounds : bounds.getBounds();
            } else {
                try {
                    Shape shape = view.modelToView(offs0, Position.Bias.Forward, offs1,
                            Position.Bias.Backward, bounds);
                    r = (shape instanceof Rectangle) ? (Rectangle) shape : shape.getBounds();
                } catch (BadLocationException e) {
                    return null;
                }
            }
            if (r.width == 0) {
                r.width = 1;
            }
            g.fillRect(r.x, r.y, r.width, r.height);
            return r;
        }
    }

    /** Un resaltado comun: dos posiciones y un pintor. */
    static class HighlightInfo implements Highlight {

        Position p0;
        Position p1;
        Highlighter.HighlightPainter painter;

        public int getStartOffset() {
            return p0.getOffset();
        }

        public int getEndOffset() {
            return p1.getOffset();
        }

        public Highlighter.HighlightPainter getPainter() {
            return painter;
        }
    }

    /** Un resaltado por capas, que ademas recuerda que region ocupo la ultima vez. */
    static class LayeredHighlightInfo extends HighlightInfo {

        int x;
        int y;
        int width;
        int height;

        void union(Shape bounds) {
            if (bounds == null) {
                return;
            }
            Rectangle alloc = (bounds instanceof Rectangle) ? (Rectangle) bounds
                    : bounds.getBounds();
            if (width == 0 || height == 0) {
                x = alloc.x;
                y = alloc.y;
                width = alloc.width;
                height = alloc.height;
            } else {
                width = Math.max(x + width, alloc.x + alloc.width);
                height = Math.max(y + height, alloc.y + alloc.height);
                x = Math.min(x, alloc.x);
                width = width - x;
                y = Math.min(y, alloc.y);
                height = height - y;
            }
        }

        void paintLayeredHighlights(Graphics g, int p0, int p1, Shape viewBounds,
                JTextComponent editor, View view) {
            int start = getStartOffset();
            int end = getEndOffset();
            p0 = Math.max(start, p0);
            p1 = Math.min(end, p1);
            union(((LayeredHighlighter.LayerPainter) painter).paintLayer(g, p0, p1, viewBounds,
                    editor, view));
        }
    }

    /**
     * Junta los tramos a repintar y los manda de una.
     *
     * <p>Sin esto, cada resaltado que cambia pide su propio repintado y el componente se dibuja
     * muchas veces en el mismo cuadro.
     */
    static class SafeDamager implements Runnable {

        private final DefaultHighlighter duenio;
        private Vector<Position> p0 = new Vector<Position>(10);
        private Vector<Position> p1 = new Vector<Position>(10);
        private Document lastDoc = null;

        SafeDamager(DefaultHighlighter duenio) {
            this.duenio = duenio;
        }

        public synchronized void run() {
            if (duenio.component != null) {
                TextUI mapper = duenio.component.getUI();
                if (mapper != null && lastDoc == duenio.component.getDocument()) {
                    int len = p0.size();
                    for (int i = 0; i < len; i++) {
                        mapper.damageRange(duenio.component, p0.elementAt(i).getOffset(),
                                p1.elementAt(i).getOffset());
                    }
                }
            }
            p0.clear();
            p1.clear();
            lastDoc = null;
        }

        public synchronized void damageRange(Position pos0, Position pos1) {
            if (duenio.component == null) {
                p0.clear();
                lastDoc = null;
                return;
            }
            boolean addToQueue = p0.isEmpty();
            Document curDoc = duenio.component.getDocument();
            if (curDoc != lastDoc) {
                if (!p0.isEmpty()) {
                    p0.clear();
                    p1.clear();
                }
                lastDoc = curDoc;
            }
            p0.add(pos0);
            p1.add(pos1);
            if (addToQueue) {
                javax.swing.SwingUtilities.invokeLater(this);
            }
        }
    }
}
