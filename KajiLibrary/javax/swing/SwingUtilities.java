package javax.swing;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;

/**
 * Las utilidades sueltas de Swing: geometria entre componentes, el hilo de eventos, y el algoritmo
 * que ubica texto e icono.
 *
 * <h2>{@link #layoutCompoundLabel} es la pieza que importa</h2>
 *
 * <p>Toda etiqueta, boton, casilla y celda de Swing coloca su texto y su icono con este metodo, y
 * por eso todos se ven igual entre si: la alineacion, la separacion, el recorte con puntos
 * suspensivos cuando no entra, salen de un solo lugar. Devuelve el texto <em>posiblemente
 * recortado</em> y deja las tres cajas —vista, icono, texto— en los rectangulos que le pasan.
 *
 * <p>Esta escrito para coincidir pixel por pixel con el JDK, que es lo que se verifica contra el:
 * el orden de las operaciones enteras, la division por dos que redondea hacia cero, y que
 * {@code CENTER} en el texto apile en vez de poner al lado, son todos casos que un "parecido" no
 * cubre.
 *
 * <h2>Lo que no esta</h2>
 *
 * <p>Las acciones por teclado ({@code notifyAction}, los mapas de entrada), lo que nombra
 * {@code JRootPane}, {@code JViewport} o {@code TransferHandler}, y las conversiones a coordenadas
 * de <em>pantalla</em>: no hay pantalla. Cada uno es una clase que no existe todavia, no un metodo
 * que se olvido.
 */
public class SwingUtilities implements SwingConstants {

    private SwingUtilities() {
    }

    // -- geometria --------------------------------------------------------------------------------

    /** Si {@code a} contiene a {@code b} por completo. */
    public static final boolean isRectangleContainingRectangle(Rectangle a, Rectangle b) {
        return b.x >= a.x && (b.x + b.width) <= (a.x + a.width)
                && b.y >= a.y && (b.y + b.height) <= (a.y + a.height);
    }

    /** El rectangulo del componente en sus propias coordenadas: {@code (0, 0, ancho, alto)}. */
    public static Rectangle getLocalBounds(Component aComponent) {
        return new Rectangle(0, 0, aComponent.getWidth(), aComponent.getHeight());
    }

    /** La primera {@link Window} entre los ancestros, o {@code null}. */
    public static Window getWindowAncestor(Component c) {
        for (Container p = c.getParent(); p != null; p = p.getParent()) {
            if (p instanceof Window) {
                return (Window) p;
            }
        }
        return null;
    }

    /**
     * Traduce un punto del sistema de coordenadas de un componente al de otro.
     *
     * <p>Pasa por la raiz comun: primero sube de {@code source} hasta su ventana, despues baja
     * hasta {@code destination}. Cualquiera de los dos puede ser {@code null}, que significa "la
     * raiz misma".
     */
    public static Point convertPoint(Component source, Point aPoint, Component destination) {
        Point p;
        if (aPoint == null) {
            p = null;
        } else {
            p = new Point(aPoint.x, aPoint.y);
        }
        if (source == null && destination == null) {
            return p;
        }
        if (p == null) {
            return null;
        }
        if (source != null) {
            // Sube hasta la raiz sumando los origenes.
            for (Component c = source; c != null; c = c.getParent()) {
                p.x = p.x + c.getX();
                p.y = p.y + c.getY();
                if (c instanceof Window) {
                    break;
                }
            }
        }
        if (destination != null) {
            for (Component c = destination; c != null; c = c.getParent()) {
                p.x = p.x - c.getX();
                p.y = p.y - c.getY();
                if (c instanceof Window) {
                    break;
                }
            }
        }
        return p;
    }

    /** Ver {@link #convertPoint(Component, Point, Component)}. */
    public static Point convertPoint(Component source, int x, int y, Component destination) {
        return convertPoint(source, new Point(x, y), destination);
    }

    /** Traduce un rectangulo entre sistemas de coordenadas; el tamano no cambia. */
    public static Rectangle convertRectangle(Component source, Rectangle aRectangle,
            Component destination) {
        Point p = convertPoint(source, new Point(aRectangle.x, aRectangle.y), destination);
        return new Rectangle(p.x, p.y, aRectangle.width, aRectangle.height);
    }

    /** El primer ancestro que sea instancia de {@code c}, o {@code null}. */
    public static Container getAncestorOfClass(Class<?> c, Component comp) {
        if (comp == null || c == null) {
            return null;
        }
        Container parent = comp.getParent();
        while (parent != null && !c.isInstance(parent)) {
            parent = parent.getParent();
        }
        return parent;
    }

    /** El primer ancestro con ese nombre, o {@code null}. */
    public static Container getAncestorNamed(String name, Component comp) {
        if (comp == null || name == null) {
            return null;
        }
        Container parent = comp.getParent();
        while (parent != null && !name.equals(parent.getName())) {
            parent = parent.getParent();
        }
        return parent;
    }

    /**
     * El componente mas profundo bajo el punto, o {@code null} si el punto cae afuera.
     *
     * <p>Baja por los hijos <em>visibles</em> y en orden de agregado, que es el z-order: el
     * primero que contiene el punto gana, aunque otro hermano tambien lo contenga.
     */
    public static Component getDeepestComponentAt(Component parent, int x, int y) {
        if (!parent.contains(x, y)) {
            return null;
        }
        if (parent instanceof Container) {
            Container c = (Container) parent;
            int n = c.getComponentCount();
            for (int i = 0; i < n; i++) {
                Component hijo = c.getComponent(i);
                if (hijo != null && hijo.isVisible()) {
                    Component hondo = getDeepestComponentAt(hijo, x - hijo.getX(), y - hijo.getY());
                    if (hondo != null) {
                        return hondo;
                    }
                }
            }
        }
        return parent;
    }

    /** El mismo evento de mouse, con su posicion traducida al sistema de {@code destination}. */
    public static MouseEvent convertMouseEvent(Component source, MouseEvent sourceEvent,
            Component destination) {
        Point p = convertPoint(source, new Point(sourceEvent.getX(), sourceEvent.getY()),
                destination);
        Component nuevoOrigen = destination != null ? destination : source;
        return new MouseEvent(nuevoOrigen, sourceEvent.getID(), sourceEvent.getWhen(),
                sourceEvent.getModifiersEx(), p.x, p.y, sourceEvent.getClickCount(),
                sourceEvent.isPopupTrigger(), sourceEvent.getButton());
    }

    /** La ventana que contiene al componente, o {@code null}. */
    public static Window windowForComponent(Component c) {
        return getWindowAncestor(c);
    }

    /** Si {@code a} es {@code b} o esta debajo de {@code b}. */
    public static boolean isDescendingFrom(Component a, Component b) {
        if (a == b) {
            return true;
        }
        for (Container p = a.getParent(); p != null; p = p.getParent()) {
            if (p == b) {
                return true;
            }
        }
        return false;
    }

    /** La interseccion, escrita en {@code dest}. Vacia si no se tocan. */
    public static Rectangle computeIntersection(int x, int y, int width, int height, Rectangle dest) {
        int x1 = Math.max(x, dest.x);
        int x2 = Math.min(x + width, dest.x + dest.width);
        int y1 = Math.max(y, dest.y);
        int y2 = Math.min(y + height, dest.y + dest.height);
        dest.x = x1;
        dest.y = y1;
        dest.width = x2 - x1;
        dest.height = y2 - y1;
        if (dest.width < 0 || dest.height < 0) {
            dest.x = 0;
            dest.y = 0;
            dest.width = 0;
            dest.height = 0;
        }
        return dest;
    }

    /** La union, escrita en {@code dest}. */
    public static Rectangle computeUnion(int x, int y, int width, int height, Rectangle dest) {
        int x1 = Math.min(x, dest.x);
        int x2 = Math.max(x + width, dest.x + dest.width);
        int y1 = Math.min(y, dest.y);
        int y2 = Math.max(y + height, dest.y + dest.height);
        dest.x = x1;
        dest.y = y1;
        dest.width = x2 - x1;
        dest.height = y2 - y1;
        return dest;
    }

    /**
     * Lo que queda de {@code rectA} al sacarle {@code rectB}, como hasta cuatro rectangulos.
     *
     * <p>Es lo que un componente repinta cuando algo lo tapa parcialmente: las franjas de arriba,
     * abajo, izquierda y derecha que no quedaron cubiertas. Sin superposicion, devuelve
     * {@code rectA} entero.
     */
    public static Rectangle[] computeDifference(Rectangle rectA, Rectangle rectB) {
        if (rectB == null || !rectA.intersects(rectB) || isRectangleContainingRectangle(rectB, rectA)) {
            return new Rectangle[0];
        }
        Rectangle[] partes = new Rectangle[4];
        int n = 0;
        // Arriba
        if (rectB.y > rectA.y) {
            partes[n] = new Rectangle(rectA.x, rectA.y, rectA.width, rectB.y - rectA.y);
            n = n + 1;
        }
        // Abajo
        int fondoB = rectB.y + rectB.height;
        int fondoA = rectA.y + rectA.height;
        if (fondoB < fondoA) {
            partes[n] = new Rectangle(rectA.x, fondoB, rectA.width, fondoA - fondoB);
            n = n + 1;
        }
        // Izquierda y derecha, solo en la franja del medio
        int medioY = Math.max(rectA.y, rectB.y);
        int medioH = Math.min(fondoA, fondoB) - medioY;
        if (rectB.x > rectA.x) {
            partes[n] = new Rectangle(rectA.x, medioY, rectB.x - rectA.x, medioH);
            n = n + 1;
        }
        int ladoB = rectB.x + rectB.width;
        int ladoA = rectA.x + rectA.width;
        if (ladoB < ladoA) {
            partes[n] = new Rectangle(ladoB, medioY, ladoA - ladoB, medioH);
            n = n + 1;
        }
        Rectangle[] resultado = new Rectangle[n];
        for (int i = 0; i < n; i++) {
            resultado[i] = partes[i];
        }
        return resultado;
    }

    /** El area interior: el componente menos sus insets, escrita en {@code r} si no es {@code null}. */
    public static Rectangle calculateInnerArea(JComponent c, Rectangle r) {
        if (c == null) {
            return null;
        }
        Rectangle rect = r;
        Insets insets = c.getInsets();
        if (rect == null) {
            rect = new Rectangle();
        }
        rect.x = insets.left;
        rect.y = insets.top;
        rect.width = c.getWidth() - insets.left - insets.right;
        rect.height = c.getHeight() - insets.top - insets.bottom;
        return rect;
    }

    /** La raiz de la jerarquia: la ventana o el applet de mas arriba, o el ultimo ancestro. */
    public static Component getRoot(Component c) {
        Component applet = null;
        for (Component p = c; p != null; p = p.getParent()) {
            if (p instanceof Window) {
                return p;
            }
            if (p instanceof java.applet.Applet) {
                applet = p;
            }
        }
        return applet;
    }

    // -- mouse ------------------------------------------------------------------------------------

    /** Si el evento es del boton izquierdo. */
    public static boolean isLeftMouseButton(MouseEvent anEvent) {
        return (anEvent.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) != 0
                || anEvent.getButton() == MouseEvent.BUTTON1;
    }

    /** Si el evento es del boton del medio. */
    public static boolean isMiddleMouseButton(MouseEvent anEvent) {
        return (anEvent.getModifiersEx() & InputEvent.BUTTON2_DOWN_MASK) != 0
                || anEvent.getButton() == MouseEvent.BUTTON2;
    }

    /** Si el evento es del boton derecho. */
    public static boolean isRightMouseButton(MouseEvent anEvent) {
        return (anEvent.getModifiersEx() & InputEvent.BUTTON3_DOWN_MASK) != 0
                || anEvent.getButton() == MouseEvent.BUTTON3;
    }

    // -- texto e icono ----------------------------------------------------------------------------

    /** El ancho de una cadena con esas metricas. */
    public static int computeStringWidth(FontMetrics fm, String str) {
        return fm.stringWidth(str);
    }

    /**
     * Ubica texto e icono dentro de {@code viewR}, resolviendo {@code LEADING} y {@code TRAILING}
     * con la orientacion de {@code c}.
     *
     * <p>Sin componente ({@code null}) se asume de izquierda a derecha.
     */
    public static String layoutCompoundLabel(JComponent c, FontMetrics fm, String text, Icon icon,
            int verticalAlignment, int horizontalAlignment, int verticalTextPosition,
            int horizontalTextPosition, Rectangle viewR, Rectangle iconR, Rectangle textR,
            int textIconGap) {
        boolean izqADer = true;
        if (c != null) {
            izqADer = c.getComponentOrientation().isLeftToRight();
        }
        int hAlign = horizontalAlignment;
        int hText = horizontalTextPosition;
        if (hText == LEADING) {
            hText = izqADer ? LEFT : RIGHT;
        } else if (hText == TRAILING) {
            hText = izqADer ? RIGHT : LEFT;
        }
        if (hAlign == LEADING) {
            hAlign = izqADer ? LEFT : RIGHT;
        } else if (hAlign == TRAILING) {
            hAlign = izqADer ? RIGHT : LEFT;
        }
        return ubicar(fm, text, icon, verticalAlignment, hAlign, verticalTextPosition, hText,
                viewR, iconR, textR, textIconGap);
    }

    /** Ver la otra forma; esta trata {@code LEADING} como {@code LEFT} y {@code TRAILING} como {@code RIGHT}. */
    public static String layoutCompoundLabel(FontMetrics fm, String text, Icon icon,
            int verticalAlignment, int horizontalAlignment, int verticalTextPosition,
            int horizontalTextPosition, Rectangle viewR, Rectangle iconR, Rectangle textR,
            int textIconGap) {
        return layoutCompoundLabel(null, fm, text, icon, verticalAlignment, horizontalAlignment,
                verticalTextPosition, horizontalTextPosition, viewR, iconR, textR, textIconGap);
    }

    /**
     * El algoritmo, ya con las posiciones resueltas a {@code LEFT}/{@code CENTER}/{@code RIGHT}.
     *
     * <p>Tres pasos: medir texto e icono; ubicar el texto <em>relativo al icono en el origen</em>
     * segun las dos posiciones del texto; y mover el par entero para alinearlo en la vista. Que el
     * texto se ubique relativo al icono antes de alinear es lo que hace que "texto a la derecha del
     * icono, todo centrado" salga bien sin casos especiales.
     *
     * <p>Un detalle que el JDK aplica y aca no hace falta: corregir el margen izquierdo negativo de
     * un glifo (una {@code f} italica que sobresale a la izquierda). La fuente de esta VM es un mapa
     * de bits sin margenes negativos, asi que esa correccion es siempre cero.
     */
    private static String ubicar(FontMetrics fm, String text, Icon icon, int verticalAlignment,
            int horizontalAlignment, int verticalTextPosition, int horizontalTextPosition,
            Rectangle viewR, Rectangle iconR, Rectangle textR, int textIconGap) {
        // El icono en el origen; su tamano es el suyo o cero.
        iconR.x = 0;
        iconR.y = 0;
        if (icon != null) {
            iconR.width = icon.getIconWidth();
            iconR.height = icon.getIconHeight();
        } else {
            iconR.width = 0;
            iconR.height = 0;
        }

        boolean sinTexto = text == null || text.isEmpty();
        String texto = text;
        if (sinTexto) {
            textR.width = 0;
            textR.height = 0;
            texto = "";
        } else {
            textR.width = computeStringWidth(fm, texto);
            textR.height = fm.getHeight();
        }

        // Sin texto o sin icono no hay separacion que respetar.
        int gap = (sinTexto || icon == null) ? 0 : textIconGap;

        if (!sinTexto) {
            // Cuanto ancho le queda al texto: si va apilado con el icono, toda la vista; si va al
            // lado, la vista menos el icono y la separacion.
            int disponible;
            if (horizontalTextPosition == CENTER) {
                disponible = viewR.width;
            } else {
                disponible = viewR.width - (iconR.width + gap);
            }
            if (textR.width > disponible) {
                // No entra: se recorta con puntos suspensivos, dejando tantos caracteres como quepan
                // junto con los puntos. El bucle suma de a un caracter y se detiene en el primero
                // que se pasa, que es exactamente lo que hace el JDK.
                String puntos = "...";
                int total = computeStringWidth(fm, puntos);
                int n;
                for (n = 0; n < texto.length(); n++) {
                    total = total + fm.charWidth(texto.charAt(n));
                    if (total > disponible) {
                        break;
                    }
                }
                texto = texto.substring(0, n) + puntos;
                textR.width = computeStringWidth(fm, texto);
            }
        }

        // El texto relativo al icono, que esta en el origen.
        if (verticalTextPosition == TOP) {
            textR.y = (horizontalTextPosition == CENTER) ? -(textR.height + gap) : 0;
        } else if (verticalTextPosition == CENTER) {
            textR.y = (iconR.height / 2) - (textR.height / 2);
        } else {
            textR.y = (horizontalTextPosition == CENTER) ? (iconR.height + gap)
                    : (iconR.height - textR.height);
        }
        if (horizontalTextPosition == LEFT) {
            textR.x = -(textR.width + gap);
        } else if (horizontalTextPosition == CENTER) {
            textR.x = (iconR.width / 2) - (textR.width / 2);
        } else {
            textR.x = iconR.width + gap;
        }

        // La caja que abarca a los dos, y su desplazamiento para alinearla en la vista.
        int cajaX = Math.min(iconR.x, textR.x);
        int cajaAncho = Math.max(iconR.x + iconR.width, textR.x + textR.width) - cajaX;
        int cajaY = Math.min(iconR.y, textR.y);
        int cajaAlto = Math.max(iconR.y + iconR.height, textR.y + textR.height) - cajaY;

        int dx;
        int dy;
        if (verticalAlignment == TOP) {
            dy = viewR.y - cajaY;
        } else if (verticalAlignment == CENTER) {
            dy = (viewR.y + (viewR.height / 2)) - (cajaY + (cajaAlto / 2));
        } else {
            dy = (viewR.y + viewR.height) - (cajaY + cajaAlto);
        }
        if (horizontalAlignment == LEFT) {
            dx = viewR.x - cajaX;
        } else if (horizontalAlignment == RIGHT) {
            dx = (viewR.x + viewR.width) - (cajaX + cajaAncho);
        } else {
            dx = (viewR.x + (viewR.width / 2)) - (cajaX + (cajaAncho / 2));
        }

        textR.x = textR.x + dx;
        textR.y = textR.y + dy;
        iconR.x = iconR.x + dx;
        iconR.y = iconR.y + dy;
        return texto;
    }

    /**
     * El indice del caracter que se subraya como mnemonico, o {@code -1}.
     *
     * <p>Primero la mayuscula, despues la minuscula: es el orden del JDK, y hace que en
     * {@code "Save As"} con mnemonico {@code A} se subraye la {@code A} de {@code As} y no la de
     * {@code Save}.
     */
    public static int findDisplayedMnemonicIndex(String text, int mnemonic) {
        if (text == null || mnemonic == '\0') {
            return -1;
        }
        char mayus = Character.toUpperCase((char) mnemonic);
        char minus = Character.toLowerCase((char) mnemonic);
        int indice = text.indexOf(mayus);
        if (indice == -1) {
            indice = text.indexOf(minus);
        }
        return indice;
    }

    /** Si el componente se lee de izquierda a derecha. */
    static boolean isLeftToRight(Component c) {
        return c.getComponentOrientation().isLeftToRight();
    }

    // -- pintar un componente ajeno ----------------------------------------------------------------

    /**
     * Pinta {@code c} dentro de {@code p}, en ese rectangulo, sin agregarlo de verdad.
     *
     * <p>Es como una tabla pinta su dibujante de celdas: el mismo componente se coloca y se pinta
     * una vez por celda. Aca se hace directo —posicionar, trasladar el contexto, pintar— sin el
     * {@code CellRendererPane} intermedio del JDK, que existe para que el componente tenga un padre
     * mientras se pinta y aca no hace falta.
     */
    public static void paintComponent(Graphics g, Component c, Container p, int x, int y, int w,
            int h) {
        c.setBounds(x, y, w, h);
        Graphics cg = g.create(x, y, w, h);
        if (cg == null) {
            return;
        }
        try {
            c.paint(cg);
        } finally {
            cg.dispose();
        }
    }

    /** Ver {@link #paintComponent(Graphics, Component, Container, int, int, int, int)}. */
    public static void paintComponent(Graphics g, Component c, Container p, Rectangle r) {
        paintComponent(g, c, p, r.x, r.y, r.width, r.height);
    }

    /** Le pide a cada {@link JComponent} del arbol que renueve su aspecto. */
    public static void updateComponentTreeUI(Component c) {
        if (c instanceof JComponent) {
            ((JComponent) c).updateUI();
        }
        if (c instanceof Container) {
            Container cont = (Container) c;
            int n = cont.getComponentCount();
            for (int i = 0; i < n; i++) {
                updateComponentTreeUI(cont.getComponent(i));
            }
        }
    }

    // -- el hilo de eventos -----------------------------------------------------------------------

    /** Encola {@code doRun} en el hilo de eventos de AWT. */
    public static void invokeLater(Runnable doRun) {
        EventQueue.invokeLater(doRun);
    }

    /** Encola {@code doRun} y espera a que termine. */
    public static void invokeAndWait(final Runnable doRun)
            throws InterruptedException, InvocationTargetException {
        EventQueue.invokeAndWait(doRun);
    }

    /** Si el hilo actual es el de eventos. */
    public static boolean isEventDispatchThread() {
        return EventQueue.isDispatchThread();
    }
}
