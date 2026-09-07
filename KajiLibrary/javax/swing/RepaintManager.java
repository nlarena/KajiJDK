package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Junta los pedidos de repintado y los resuelve todos juntos.
 *
 * <h2>Por que no se repinta enseguida</h2>
 *
 * <p>Cambiar el texto de una etiqueta, su color y su borde son tres pedidos de repintado del mismo
 * rectangulo. Atenderlos uno por uno dibujaria tres veces lo mismo. En vez de eso, cada pedido se
 * anota -- {@link #addDirtyRegion} -- y los rectangulos del mismo componente se <em>unen</em>; el
 * dibujado ocurre despues, una sola vez, en {@link #paintDirtyRegions}.
 *
 * <p>Lo mismo con las medidas: {@link #addInvalidComponent} anota que hay que volver a acomodar, y
 * {@link #validateInvalidComponents} lo hace todo junto.
 *
 * <h2>El doble buffer</h2>
 *
 * <p>Dibujar directo sobre la pantalla se ve como un parpadeo: primero el fondo, despues el texto.
 * El doble buffer dibuja en una imagen en memoria y copia el resultado de una vez. La imagen se
 * comparte entre todos los componentes de una ventana -- {@link #getOffscreenBuffer} -- porque una
 * por componente seria muchisima memoria para algo que se usa un instante.
 *
 * <h2>Sin pantalla</h2>
 *
 * <p>Todo lo que es <em>contabilidad</em> anda: se anotan los rectangulos, se unen, se listan, se
 * limpian. Lo que no ocurre es el dibujado -- {@link #paintDirtyRegions} recorre lo anotado y no
 * tiene a donde pintarlo --, ni el encolado en el hilo de eventos, que aca no corre. Un componente
 * se pinta cuando alguien le pasa un {@code Graphics}, y eso no pasa por aca.
 */
public class RepaintManager {

    boolean doubleBufferingEnabled = true;

    Rectangle tmp = new Rectangle();

    private static RepaintManager delegado;

    private final Map<Component, Rectangle> dirtyComponents =
            new HashMap<Component, Rectangle>();

    private final List<JComponent> invalidComponents = new ArrayList<JComponent>();

    private Dimension doubleBufferMaxSize;

    private Image doubleBuffer;

    /** El administrador de este contexto. */
    public static RepaintManager currentManager(Component c) {
        return actual();
    }

    /** El administrador de este contexto. */
    public static RepaintManager currentManager(JComponent c) {
        return actual();
    }

    private static synchronized RepaintManager actual() {
        if (delegado == null) {
            delegado = new RepaintManager();
        }
        return delegado;
    }

    /** Cambia el administrador; nulo devuelve el de siempre. */
    public static void setCurrentManager(RepaintManager aRepaintManager) {
        synchronized (RepaintManager.class) {
            delegado = aRepaintManager;
        }
    }

    /** Uno vacio. */
    public RepaintManager() {
    }

    /**
     * Anota que ese componente tiene que volver a acomodarse.
     *
     * <p>Se anota el <em>ancestro validante</em> mas cercano -- el primero que puede resolver un
     * cambio de tamano sin molestar a su padre --, no el componente. Es lo que evita que cambiar el
     * texto de una etiqueta rearme la ventana entera.
     */
    public synchronized void addInvalidComponent(JComponent invalidComponent) {
        Component validateRoot = null;
        for (Component c = invalidComponent; c != null; c = c.getParent()) {
            if (c instanceof java.awt.CellRendererPane) {
                return;
            }
            if (c instanceof JComponent && ((JComponent) c).isValidateRoot()) {
                validateRoot = c;
                break;
            }
        }
        if (validateRoot == null) {
            return;
        }
        for (Component c = validateRoot; c != null; c = c.getParent()) {
            if (!c.isVisible() || !c.isDisplayable()) {
                return;
            }
            if (c instanceof Window || c instanceof java.applet.Applet) {
                break;
            }
        }
        if (invalidComponents.contains(validateRoot)) {
            return;
        }
        invalidComponents.add((JComponent) validateRoot);
    }

    /** Lo saca de la lista de los que hay que acomodar. */
    public synchronized void removeInvalidComponent(JComponent component) {
        invalidComponents.remove(component);
    }

    /**
     * Anota que hay que repintar ese rectangulo de ese componente.
     *
     * <p>Si ya habia uno anotado, los dos se unen en el rectangulo que los contiene. Unir en vez de
     * guardar los dos es la decision que hace barata a esta clase: la union puede pintar de mas,
     * pero nunca de menos, y pintar de mas es solo mas lento.
     */
    public void addDirtyRegion(JComponent c, int x, int y, int w, int h) {
        anotar(c, x, y, w, h);
    }

    /** Idem, para una ventana. */
    public void addDirtyRegion(Window window, int x, int y, int w, int h) {
        anotar(window, x, y, w, h);
    }

    /**
     * Idem, para un applet.
     *
     * @deprecated Como en el JDK: los applets ya no corren en ningun lado.
     */
    @Deprecated
    public void addDirtyRegion(java.applet.Applet applet, int x, int y, int w, int h) {
        anotar(applet, x, y, w, h);
    }

    private synchronized void anotar(Component c, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0 || c == null) {
            return;
        }
        if (c.getWidth() <= 0 || c.getHeight() <= 0) {
            return;
        }
        Rectangle r = dirtyComponents.get(c);
        if (r != null) {
            // Ya estaba anotado: se agranda y listo, sin volver a mirar los ancestros.
            unir(r, x, y, w, h);
            return;
        }
        if (!seVe(c)) {
            return;
        }
        dirtyComponents.put(c, new Rectangle(x, y, w, h));
    }

    /**
     * Si ese componente esta en pantalla de verdad.
     *
     * <p>Anotar lo que no se ve no sirve para nada: cuando el componente aparezca habra que pintarlo
     * entero igual. Asi que si el componente no tiene padre, o alguno de sus ancestros esta
     * escondido o todavia no tiene ventana, la anotacion se descarta.
     *
     * <p>Esto es lo que hace que en una biblioteca sin pantalla {@link #getDirtyRegion} devuelva
     * siempre el rectangulo vacio y {@link #isCompletelyDirty} siempre `false`. Esta medido contra
     * el JDK, que hace exactamente lo mismo.
     */
    private static boolean seVe(Component c) {
        Container padre = c.getParent();
        if (padre == null) {
            return false;
        }
        for (Container p = padre; p != null; p = p.getParent()) {
            if (!p.isVisible() || !p.isDisplayable()) {
                return false;
            }
            if (p instanceof Window) {
                break;
            }
        }
        return true;
    }

    /** Agranda el rectangulo para que contenga tambien al nuevo. */
    private static void unir(Rectangle r, int x, int y, int w, int h) {
        int x1 = Math.min(r.x, x);
        int y1 = Math.min(r.y, y);
        int x2 = Math.max(r.x + r.width, x + w);
        int y2 = Math.max(r.y + r.height, y + h);
        r.x = x1;
        r.y = y1;
        r.width = x2 - x1;
        r.height = y2 - y1;
    }

    /** Lo que hay anotado para repintar de ese componente; vacio si no hay nada. */
    public Rectangle getDirtyRegion(JComponent aComponent) {
        Rectangle r;
        synchronized (this) {
            r = dirtyComponents.get(aComponent);
        }
        if (r == null) {
            return new Rectangle(0, 0, 0, 0);
        }
        return new Rectangle(r);
    }

    /** Anota que hay que repintar el componente entero. */
    public void markCompletelyDirty(JComponent aComponent) {
        addDirtyRegion(aComponent, 0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /** Olvida lo anotado de ese componente. */
    public void markCompletelyClean(JComponent aComponent) {
        synchronized (this) {
            dirtyComponents.remove(aComponent);
        }
    }

    /**
     * Si hay que repintarlo entero.
     *
     * <p>Se contesta mirando el ancho del rectangulo anotado: {@link Integer#MAX_VALUE} es la marca
     * de "todo". Es lo que deja que {@link #markCompletelyDirty} no necesite un campo aparte.
     */
    public boolean isCompletelyDirty(JComponent aComponent) {
        Rectangle r = getDirtyRegion(aComponent);
        return (r.width == Integer.MAX_VALUE) && (r.height == Integer.MAX_VALUE);
    }

    /** Acomoda todo lo que quedo pendiente y limpia la lista. */
    public void validateInvalidComponents() {
        List<JComponent> ic;
        synchronized (this) {
            if (invalidComponents.isEmpty()) {
                return;
            }
            ic = new ArrayList<JComponent>(invalidComponents);
            invalidComponents.clear();
        }
        for (int i = 0; i < ic.size(); i++) {
            ic.get(i).validate();
        }
    }

    /**
     * Repinta todo lo anotado y limpia la lista.
     *
     * <p>Sin pantalla no hay a donde pintar; ver la nota de la clase. Lo que si ocurre es que la
     * lista se vacia, que es lo que el resto del sistema observa.
     */
    public void paintDirtyRegions() {
        Map<Component, Rectangle> tmpDirtyComponents;
        synchronized (this) {
            if (dirtyComponents.isEmpty()) {
                return;
            }
            tmpDirtyComponents = new HashMap<Component, Rectangle>(dirtyComponents);
            dirtyComponents.clear();
        }
        java.util.Iterator<Component> it = tmpDirtyComponents.keySet().iterator();
        while (it.hasNext()) {
            Component c = it.next();
            Rectangle r = tmpDirtyComponents.get(c);
            if (c instanceof JComponent && r != null) {
                ((JComponent) c).paintImmediately(r.x, r.y, r.width, r.height);
            }
        }
    }

    public synchronized String toString() {
        StringBuilder sb = new StringBuilder();
        if (!dirtyComponents.isEmpty()) {
            sb.append("DirtyComponents: ").append(dirtyComponents).append("\n");
        }
        if (!invalidComponents.isEmpty()) {
            sb.append("InvalidComponents: ").append(invalidComponents);
        }
        return sb.toString();
    }

    /**
     * La imagen compartida donde se dibuja antes de copiar; ver la nota de la clase.
     *
     * <p>Se agranda cuando hace falta y nunca se achica: achicarla obligaria a crear otra la proxima
     * vez que se necesite el tamano grande, y son pocos tamanos distintos en la vida de una ventana.
     */
    public Image getOffscreenBuffer(Component c, int proposedWidth, int proposedHeight) {
        return buffer(c, proposedWidth, proposedHeight);
    }

    /**
     * Como {@link #getOffscreenBuffer}, pero pidiendo memoria de la placa si la hubiera.
     *
     * <p>Aca no la hay, asi que devuelve la misma imagen.
     */
    public Image getVolatileOffscreenBuffer(Component c, int proposedWidth,
            int proposedHeight) {
        return buffer(c, proposedWidth, proposedHeight);
    }

    private synchronized Image buffer(Component c, int proposedWidth, int proposedHeight) {
        Dimension maxSize = getDoubleBufferMaximumSize();
        int width = Math.min(proposedWidth, maxSize.width);
        int height = Math.min(proposedHeight, maxSize.height);
        width = Math.max(1, width);
        height = Math.max(1, height);
        if (doubleBuffer != null) {
            int w = doubleBuffer.getWidth(null);
            int h = doubleBuffer.getHeight(null);
            if (w >= width && h >= height) {
                return doubleBuffer;
            }
            width = Math.max(width, w);
            height = Math.max(height, h);
        }
        doubleBuffer = new java.awt.image.BufferedImage(width, height,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        return doubleBuffer;
    }

    /** Cuanto puede crecer la imagen compartida. */
    public void setDoubleBufferMaximumSize(Dimension d) {
        doubleBufferMaxSize = d;
        if (doubleBuffer != null) {
            if (doubleBuffer.getWidth(null) > d.width
                    || doubleBuffer.getHeight(null) > d.height) {
                doubleBuffer = null;
            }
        }
    }

    /**
     * El tope.
     *
     * <p>Por omision, lo que ocupan todas las pantallas juntas: no tiene sentido guardar un buffer
     * mas grande que todo lo que se puede llegar a mostrar. Sin pantallas no hay ese tope, y el
     * valor es {@link Integer#MAX_VALUE} en los dos lados; asi lo hace el JDK y esta medido.
     */
    public Dimension getDoubleBufferMaximumSize() {
        if (doubleBufferMaxSize == null) {
            try {
                java.awt.Rectangle todas = new java.awt.Rectangle();
                java.awt.GraphicsEnvironment ge =
                        java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
                java.awt.GraphicsDevice[] pantallas = ge.getScreenDevices();
                for (int i = 0; i < pantallas.length; i++) {
                    todas = todas.union(
                            pantallas[i].getDefaultConfiguration().getBounds());
                }
                doubleBufferMaxSize = new Dimension(todas.width, todas.height);
            } catch (java.awt.HeadlessException e) {
                doubleBufferMaxSize = new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
            }
        }
        return doubleBufferMaxSize;
    }

    /** Prende o apaga el doble buffer para toda la aplicacion. */
    public void setDoubleBufferingEnabled(boolean aFlag) {
        doubleBufferingEnabled = aFlag;
    }

    public boolean isDoubleBufferingEnabled() {
        return doubleBufferingEnabled;
    }
}
